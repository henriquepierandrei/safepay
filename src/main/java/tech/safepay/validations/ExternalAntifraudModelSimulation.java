package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import java.math.BigDecimal;
import java.util.List;

/**
 * Simulador de motor de antifraude externo (Third-Party Risk Engine).
 * <p>
 * Esta classe simula o comportamento de serviços externos de detecção de fraude
 * (como Cybersource, Adyen Risk, Stripe Radar, ClearSale) que utilizam modelos
 * proprietários e não-explicáveis para retornar scores de risco.
 * </p>
 * <p>
 * A simulação permite:
 * <ul>
 *   <li>Testes de integração do pipeline antifraude</li>
 *   <li>Validação da arquitetura sem dependências externas</li>
 *   <li>Demonstração de fluxos de análise de risco</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Importante:</strong> Este componente NÃO decide aprovação ou bloqueio
 * de transações de forma isolada. Atua como sinal complementar no cálculo do
 * score global de risco.
 * </p>
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class ExternalAntifraudModelSimulation {

    /**
     * Detecta anomalias estatísticas em transações utilizando análise de desvio padrão.
     * <p>
     * Este método simula um modelo de machine learning externo que analisa o padrão
     * histórico de transações do cartão e identifica comportamentos atípicos.
     * </p>
     *
     * <h3>Estratégia Estatística</h3>
     * <ul>
     *   <li><strong>Janela de análise:</strong> 24 horas (últimas 20 transações)</li>
     *   <li><strong>Requisito mínimo:</strong> 10 transações no histórico</li>
     *   <li><strong>Método de detecção:</strong> Regra dos 3 sigma (3σ)</li>
     * </ul>
     *
     * <h3>Regra de Decisão</h3>
     * <pre>
     * |valor_atual − média| &gt; 2.5 × desvio_padrão → ANOMALIA DETECTADA
     * </pre>
     *
     * <h3>Funcionamento</h3>
     * <ol>
     *   <li>Calcula a média dos valores das transações históricas</li>
     *   <li>Calcula o desvio padrão (dispersão dos valores)</li>
     *   <li>Compara o valor atual com o padrão estatístico</li>
     *   <li>Sinaliza anomalia se o valor estiver muito distante da média</li>
     * </ol>
     *
     * <h3>Exemplo de Uso</h3>
     * <pre>
     * // Histórico: R$ 50, R$ 55, R$ 48, R$ 52...
     * // Transação atual: R$ 5.000
     * // Resultado: ANOMALIA DETECTADA (valor muito acima do padrão)
     * </pre>
     *
     * @param transaction a transação atual sendo analisada (não pode ser null)
     * @param snapshot snapshot contendo o histórico recente de transações do cartão
     * @return {@link ValidationResultDto} contendo:
     *         <ul>
     *           <li>Score de risco se anomalia for detectada (peso 30)</li>
     *           <li>Alerta {@link AlertType#ANOMALY_MODEL_TRIGGERED}</li>
     *           <li>Objeto vazio se nenhuma anomalia for encontrada</li>
     *         </ul>
     *
     * @throws NullPointerException se transaction ou snapshot forem null
     *
     * @see AlertType#ANOMALY_MODEL_TRIGGERED
     * @see ValidationResultDto
     * @see TransactionGlobalValidation.ValidationSnapshot
     */
    public ValidationResultDto anomalyModelTriggered(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {

        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();

        // Validação de pré-requisitos
        if (card == null || transaction.getAmount() == null) {
            return result;
        }

        // Extrai histórico excluindo a transação atual
        List<Transaction> history = snapshot.last20().stream()
                .filter(t -> !t.getTransactionId().equals(transaction.getTransactionId()))
                .toList();

        // Histórico insuficiente para análise estatística
        if (history.size() < 10) return result;

        // Calcula média dos valores históricos
        double avg = history.stream()
                .map(Transaction::getAmount)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0);

        // Calcula variância
        double variance = history.stream()
                .map(Transaction::getAmount)
                .mapToDouble(v -> Math.pow(v.doubleValue() - avg, 2))
                .average()
                .orElse(0);

        // Calcula desvio padrão
        double std = Math.sqrt(variance);

        // Aplica regra de detecção de anomalia (2.5σ)
        if (std > 0 && Math.abs(transaction.getAmount().doubleValue() - avg) > 2.5 * std) {
            result.addScore(AlertType.ANOMALY_MODEL_TRIGGERED.getScore());
            result.addAlert(AlertType.ANOMALY_MODEL_TRIGGERED);
        }

        return result;
    }
}
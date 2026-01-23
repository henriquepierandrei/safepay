package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import java.math.BigDecimal;
import java.util.DoubleSummaryStatistics;
import java.util.List;

@Component
public class ExternalAntifraudModelSimulation {

    /**
     * ANOMALY_MODEL_TRIGGERED (30)
     *
     * Simulação de ANTIFRAUDE EXTERNO (Third-Party Risk Engine)
     *
     * Contexto:
     * Em ambientes reais de pagamento, é comum integrar serviços externos
     * de antifraude (ex: Cybersource, Adyen Risk, Stripe Radar, ClearSale),
     * que retornam apenas um score ou flag de risco baseado em modelos
     * proprietários e não-explicáveis.
     *
     * Este método SIMULA esse comportamento de forma local e controlada,
     * permitindo:
     * - Testes de integração
     * - Validação de arquitetura
     * - Demonstração de pipeline antifraude
     *
     * O que este "modelo externo" faz:
     * - Analisa o histórico recente do cartão
     * - Calcula métricas estatísticas básicas (média e desvio padrão)
     * - Avalia se a transação atual foge significativamente do padrão
     *
     * Estratégia estatística simulada:
     * - Janela móvel de 24 horas
     * - Histórico mínimo de 10 transações
     * - Regra de anomalia baseada em 3 desvios padrão (3σ)
     *
     * Regra de decisão:
     * |valor_atual − média| > 3 × desvio_padrão → anomalia detectada
     *
     * Observações importantes:
     * - NÃO decide aprovação ou bloqueio sozinho
     * - Atua como sinal complementar no score global
     * - Comportamento intencionalmente genérico (black-box simulation)
     * - Peso médio (30)
     *
     * Resultado:
     * - Retorna score fixo quando o "modelo externo" sinaliza risco
     * - Retorna 0 quando não há anomalia estatística
     */
    public ValidationResultDto anomalyModelTriggered(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {

        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();

        if (card == null || transaction.getAmount() == null) {
            return result;
        }

        List<Transaction> history = snapshot.last20().stream()
                .filter(t -> !t.getTransactionId().equals(transaction.getTransactionId()))
                .toList();

        if (history.size() < 10) return result;

        double avg = history.stream()
                .map(Transaction::getAmount)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0);

        double variance = history.stream()
                .map(Transaction::getAmount)
                .mapToDouble(v -> Math.pow(v.doubleValue() - avg, 2))
                .average()
                .orElse(0);

        double std = Math.sqrt(variance);

        if (std > 0 && Math.abs(transaction.getAmount().doubleValue() - avg) > 2.5 * std) {
            result.addScore(AlertType.ANOMALY_MODEL_TRIGGERED.getScore());
            result.addAlert(AlertType.ANOMALY_MODEL_TRIGGERED);
        }


        return result;
    }

}

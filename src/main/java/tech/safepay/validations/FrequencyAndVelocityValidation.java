package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import java.util.List;

/**
 * Componente responsável por validações de frequência e velocidade de transações.
 * <p>
 * Esta classe implementa mecanismos de detecção de padrões anômalos de uso de cartão,
 * focando em identificar atividades suspeitas relacionadas à velocidade e frequência
 * de transações em janelas temporais específicas.
 * </p>
 * <p>
 * As validações incluem:
 * <ul>
 *   <li>Detecção de abuso de velocidade (VELOCITY_ABUSE)</li>
 *   <li>Detecção de picos de atividade (BURST_ACTIVITY)</li>
 * </ul>
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class FrequencyAndVelocityValidation {

    /**
     * Valida se há abuso de velocidade nas transações do cartão.
     * <p>
     * Esta validação detecta múltiplas transações em um curto intervalo de tempo,
     * padrão típico de card testing, bots ou ataques automatizados.
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ul>
     *   <li>Janela móvel de observação: 5 minutos</li>
     *   <li>Contagem simples de transações na janela</li>
     *   <li>Threshold fixo definido por política de risco</li>
     *   <li>Limite máximo permitido: 3 transações</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> 35 pontos
     * <br>
     * Sinal forte, frequentemente associado a fraude real.
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se o cartão não estiver disponível ou se não
     *         houver violação do threshold.
     * @see AlertType#VELOCITY_ABUSE
     * @see TransactionGlobalValidation.ValidationSnapshot#last5Minutes()
     */
    public ValidationResultDto velocityAbuseValidation(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        if (card == null) return result;

        // Janela de observação (últimos 5 minutos)
        List<Transaction> recentTransactions = snapshot.last5Minutes();

        int maxAllowed = 3;

        if (recentTransactions.size() >= maxAllowed) {
            result.addScore(AlertType.VELOCITY_ABUSE.getScore());
            result.addAlert(AlertType.VELOCITY_ABUSE);
        }

        return result;
    }

    /**
     * Valida se há picos súbitos de atividade que fogem do padrão histórico do cartão.
     * <p>
     * Esta validação identifica aumentos anormais de atividade comparando o volume
     * atual com o baseline comportamental do cartão, mesmo quando o volume absoluto
     * não é alto.
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Estabelece baseline comportamental (últimas 24 horas)</li>
     *   <li>Calcula média de transações por hora no período baseline</li>
     *   <li>Analisa janela curta de observação (5 minutos)</li>
     *   <li>Compara volume esperado versus volume real</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Critérios de Ativação:</strong>
     * <ul>
     *   <li>Mínimo de 5 transações no histórico de 24 horas</li>
     *   <li>Volume na janela de 5 minutos superior a 3x a média histórica por hora</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> 25 pontos
     * <br>
     * Atua como reforço complementar para VELOCITY_ABUSE.
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se: o cartão não estiver disponível, houver menos
     *         de 5 transações no baseline, ou não houver desvio significativo do padrão.
     * @see AlertType#BURST_ACTIVITY
     * @see TransactionGlobalValidation.ValidationSnapshot#last24Hours()
     * @see TransactionGlobalValidation.ValidationSnapshot#last5Minutes()
     */
    public ValidationResultDto burstActivityValidation(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        if (card == null) return result;

        // Baseline histórico (últimas 24 horas)
        List<Transaction> baselineTransactions = snapshot.last24Hours();

        if (baselineTransactions.size() < 5) return result;

        // Média histórica de transações por hora
        double avgPerHour = baselineTransactions.size() / 24.0;

        int burstCount = snapshot.last5Minutes().size();

        if (burstCount > avgPerHour * 3) {
            result.addScore(AlertType.BURST_ACTIVITY.getScore());
            result.addAlert(AlertType.BURST_ACTIVITY);
        }

        return result;
    }
}
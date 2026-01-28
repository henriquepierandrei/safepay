package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Transaction;

import java.util.List;

/**
 * Componente responsável por validações relacionadas ao comportamento do usuário.
 * <p>
 * Esta classe implementa mecanismos de detecção de anomalias comportamentais baseadas
 * em padrões históricos de uso do cartão, identificando desvios significativos que
 * podem indicar uso não autorizado ou fraudulento.
 * </p>
 * <p>
 * As validações incluem:
 * <ul>
 *   <li>Detecção de anomalia de horário (TIME_OF_DAY_ANOMALY)</li>
 * </ul>
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class UserBehaviorValidation {

    /**
     * Número mínimo de transações históricas necessárias para estabelecer baseline comportamental.
     */
    private static final int MINIMUM_HISTORICAL_TRANSACTIONS = 10;

    /**
     * Desvio máximo permitido em horas em relação ao horário médio histórico.
     */
    private static final int ALLOWED_HOUR_DEVIATION = 4;

    /**
     * Valida se a transação ocorre em horário atípico comparado ao padrão histórico.
     * <p>
     * Esta validação detecta transações realizadas em horários significativamente
     * diferentes do comportamento histórico do usuário, o que pode indicar:
     * <ul>
     *   <li>Uso do cartão por terceiros em fuso horário diferente</li>
     *   <li>Atividade fraudulenta em horários incomuns para o titular</li>
     *   <li>Comprometimento do cartão sendo usado em padrões diferentes</li>
     *   <li>Mudança temporária legítima de rotina (viagem, mudança de trabalho)</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Recupera histórico recente de transações (últimas 20)</li>
     *   <li>Calcula a média dos horários históricos de transação</li>
     *   <li>Extrai o horário da transação atual (0-23)</li>
     *   <li>Compara com desvio máximo permitido (4 horas)</li>
     *   <li>Sinaliza se o desvio exceder o threshold</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Baseline Comportamental:</strong>
     * <br>
     * Utiliza as últimas 20 transações como proxy do comportamento recente (aproximadamente
     * 30 dias para usuários regulares). A média dos horários estabelece o padrão típico
     * de uso do cartão.
     * </p>
     * <p>
     * <strong>Critérios Mínimos:</strong>
     * <ul>
     *   <li>Mínimo de 10 transações no histórico para baseline confiável</li>
     *   <li>Desvio superior a 4 horas da média histórica</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Tratamento de Casos Especiais:</strong>
     * <ul>
     *   <li>Histórico insuficiente (&lt; 10 transações): retorna risco neutro</li>
     *   <li>Transações sem timestamp: são ignoradas no cálculo</li>
     *   <li>Primeira transação do dia: usa horário atual como fallback</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> 10 pontos
     * <br>
     * Sinal complementar de baixo peso. Atua como reforço quando combinado com outros
     * indicadores, mas nunca deve determinar bloqueio isoladamente devido à natureza
     * variável dos padrões de horário dos usuários.
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se: a transação ou timestamp não estiverem disponíveis,
     *         histórico for insuficiente, ou desvio de horário não exceder o threshold.
     * @see AlertType#TIME_OF_DAY_ANOMALY
     * @see TransactionGlobalValidation.ValidationSnapshot#last20()
     */
    public ValidationResultDto timeOfDayAnomaly(
            Transaction transaction,
            TransactionGlobalValidation.ValidationSnapshot snapshot
    ) {
        ValidationResultDto result = new ValidationResultDto();

        if (transaction == null || transaction.getCreatedAt() == null) {
            return result;
        }

        List<Transaction> historicalTransactions = snapshot.last20();

        // Histórico insuficiente → risco neutro
        if (historicalTransactions.size() < MINIMUM_HISTORICAL_TRANSACTIONS) {
            return result;
        }

        // Média do horário histórico
        double averageHour = historicalTransactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .mapToInt(t -> t.getCreatedAt().getHour())
                .average()
                .orElse(transaction.getCreatedAt().getHour());

        int currentHour = transaction.getCreatedAt().getHour();

        if (Math.abs(currentHour - averageHour) > ALLOWED_HOUR_DEVIATION) {
            result.addScore(AlertType.TIME_OF_DAY_ANOMALY.getScore());
            result.addAlert(AlertType.TIME_OF_DAY_ANOMALY);
        }

        return result;
    }
}
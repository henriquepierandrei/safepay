package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Componente responsável por validações de limite e expiração de cartões.
 * <p>
 * Esta classe implementa verificações críticas relacionadas à capacidade
 * de crédito disponível e validade temporal do cartão, garantindo conformidade
 * operacional e identificando possíveis riscos de uso.
 * </p>
 * <p>
 * As validações incluem:
 * <ul>
 *   <li>Verificação de limite de crédito disponível (CREDIT_LIMIT_REACHED)</li>
 *   <li>Detecção de proximidade da data de expiração (EXPIRATION_DATE_APPROACHING)</li>
 * </ul>
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class LimitAndExpirationValidation {

    /**
     * Período em dias considerado como "próximo da expiração".
     * <p>
     * Cartões com expiração em 30 dias ou menos são sinalizados.
     * </p>
     */
    private static final long EXPIRATION_WARNING_DAYS = 30;

    /**
     * Executa validações combinadas de limite de crédito e data de expiração.
     * <p>
     * Este método realiza duas verificações independentes sobre o cartão utilizado
     * na transação, identificando situações de risco operacional que podem impactar
     * a autorização ou indicar comportamento atípico.
     * </p>
     * <p>
     * <strong>Validações Executadas:</strong>
     * </p>
     * <p>
     * <strong>1. Limite de Crédito (CREDIT_LIMIT_REACHED):</strong>
     * <ul>
     *   <li>Verifica se o valor da transação excede o limite disponível do cartão</li>
     *   <li>Compara: {@code valor_transação > limite_restante}</li>
     *   <li>Peso: definido em {@link AlertType#CREDIT_LIMIT_REACHED}</li>
     *   <li>Impacto: indica tentativa de uso além da capacidade autorizada</li>
     * </ul>
     * </p>
     * <p>
     * <strong>2. Proximidade de Expiração (EXPIRATION_DATE_APPROACHING):</strong>
     * <ul>
     *   <li>Verifica se o cartão expira em 30 dias ou menos</li>
     *   <li>Calcula dias até expiração a partir da data atual</li>
     *   <li>Peso: definido em {@link AlertType#EXPIRATION_DATE_APPROACHING}</li>
     *   <li>Impacto: sinaliza possível renovação pendente ou uso próximo ao fim da validade</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Comportamento:</strong>
     * <br>
     * As validações são independentes e cumulativas. O resultado pode conter
     * nenhum, um ou ambos os alertas, com pontuações somadas quando aplicável.
     * </p>
     *
     * @param transaction a transação sendo validada, contendo referência ao cartão
     *                    e valor da operação
     * @return {@link ValidationResultDto} contendo a pontuação total e lista de alertas
     *         identificados. Retorna resultado vazio se nenhuma condição for atendida.
     * @throws NullPointerException se a transação, cartão ou campos obrigatórios forem nulos
     * @see AlertType#CREDIT_LIMIT_REACHED
     * @see AlertType#EXPIRATION_DATE_APPROACHING
     * @see Card#getRemainingLimit()
     * @see Card#getExpirationDate()
     */
    public ValidationResultDto validate(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        BigDecimal transactionAmount = transaction.getAmount();
        LocalDate expirationDate = card.getExpirationDate();

        // =====================
        // 1️⃣ Valida limite de crédito
        // =====================
        if (transactionAmount.compareTo(card.getRemainingLimit()) > 0) {
            result.addScore(AlertType.CREDIT_LIMIT_REACHED.getScore());
            result.addAlert(AlertType.CREDIT_LIMIT_REACHED);
        }

        // =====================
        // 2️⃣ Valida proximidade da expiração (30 dias)
        // =====================
        long daysToExpiration = LocalDate.now().until(expirationDate, ChronoUnit.DAYS);
        if (daysToExpiration <= EXPIRATION_WARNING_DAYS) {
            result.addScore(AlertType.EXPIRATION_DATE_APPROACHING.getScore());
            result.addAlert(AlertType.EXPIRATION_DATE_APPROACHING);
        }

        return result;
    }
}
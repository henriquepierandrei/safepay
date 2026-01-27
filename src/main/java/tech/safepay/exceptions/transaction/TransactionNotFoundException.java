package tech.safepay.exceptions.transaction;

/**
 * Exceção lançada quando uma transação não é encontrada
 * no sistema.
 *
 * <p>Utilizada em operações de consulta, validação ou
 * auditoria, quando o identificador informado não
 * corresponde a nenhuma transação persistida.</p>
 *
 * <p>Ajuda a diferenciar falhas de domínio de erros
 * técnicos, facilitando o tratamento centralizado
 * via {@code @ControllerAdvice}.</p>
 */
public class TransactionNotFoundException extends RuntimeException {

    /**
     * Cria uma nova exceção indicando que a transação
     * não foi localizada.
     *
     * @param message mensagem descritiva do erro
     */
    public TransactionNotFoundException(String message) {
        super(message);
    }
}

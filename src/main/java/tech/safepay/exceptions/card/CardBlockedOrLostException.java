package tech.safepay.exceptions.card;

/**
 * Exceção lançada quando uma operação é tentada em um cartão que está bloqueado ou perdido.
 *
 * <p>Usada para indicar que transações, pagamentos ou qualquer ação que envolva o cartão
 * não podem ser processadas devido ao estado de bloqueio ou perda.</p>
 *
 * <p>Permite tratar centralizadamente falhas de domínio relacionadas a cartões através
 * de {@code @ControllerAdvice} ou outro mecanismo de tratamento de exceções.</p>
 */
public class CardBlockedOrLostException extends RuntimeException {

    /**
     * Cria uma nova exceção indicando que o cartão está bloqueado ou perdido.
     *
     * @param message Mensagem detalhando o motivo da exceção
     */
    public CardBlockedOrLostException(String message) {
        super(message);
    }

    /**
     * Cria uma nova exceção indicando que o cartão está bloqueado ou perdido,
     * incluindo a causa original.
     *
     * @param message Mensagem detalhando o motivo da exceção
     * @param cause A causa original que levou a esta exceção
     */
    public CardBlockedOrLostException(String message, Throwable cause) {
        super(message, cause);
    }
}

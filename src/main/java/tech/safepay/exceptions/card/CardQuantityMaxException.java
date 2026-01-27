package tech.safepay.exceptions.card;

/**
 * Exceção lançada quando a quantidade máxima de cartões permitida
 * para um ser criada no sistema.
 *
 * <p>Utilizada normalmente durante fluxos de criação ou associação
 * de cartões, garantindo o cumprimento de regras de negócio
 * e limites operacionais definidos pelo sistema.</p>
 *
 * <p>Por estender {@link RuntimeException}, permite tratamento
 * centralizado e consistente via mecanismos globais de exceção.</p>
 */
public class CardQuantityMaxException extends RuntimeException {

    /**
     * Cria uma nova exceção indicando que o limite máximo de cartões foi atingido.
     *
     * @param message mensagem descritiva do erro
     */
    public CardQuantityMaxException(String message) {
        super(message);
    }
}

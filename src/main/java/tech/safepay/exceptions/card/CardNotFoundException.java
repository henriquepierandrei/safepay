package tech.safepay.exceptions.card;

/**
 * Exceção lançada quando um cartão não é encontrado no sistema.
 *
 * <p>Normalmente utilizada em fluxos de consulta, processamento de transações
 * ou validações antifraude quando o identificador do cartão informado
 * não corresponde a nenhum registro persistido.</p>
 *
 * <p>Esta exceção estende {@link RuntimeException} para permitir propagação
 * automática e tratamento centralizado via {@code @ControllerAdvice}.</p>
 */
public class CardNotFoundException extends RuntimeException {

    /**
     * Cria uma nova exceção de cartão não encontrado.
     *
     * @param message mensagem descritiva do erro
     */
    public CardNotFoundException(String message) {
        super(message);
    }
}

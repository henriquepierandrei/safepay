package tech.safepay.exceptions.alerts;

/**
 * Exceção lançada quando uma operação tenta manipular um alerta que não existe.
 *
 * <p>Usada em operações de consulta, atualização ou auditoria,
 * quando o identificador do alerta informado não corresponde a nenhum registro existente.</p>
 *
 * <p>Facilita a separação entre erros de negócio e erros técnicos,
 * permitindo tratamento centralizado via {@code @ControllerAdvice}.</p>
 *
 * @author Tech SafePay
 * @since 1.0
 */
public class AlertNotFoundException extends RuntimeException {

    /**
     * Cria uma nova exceção indicando que o alerta especificado não foi encontrado.
     *
     * @param message Mensagem detalhando o motivo da exceção
     */
    public AlertNotFoundException(String message) {
        super(message);
    }

    /**
     * Cria uma nova exceção indicando que o alerta não foi encontrado e inclui a causa original.
     *
     * @param message Mensagem detalhando o motivo da exceção
     * @param cause A causa original que levou a esta exceção
     */
    public AlertNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

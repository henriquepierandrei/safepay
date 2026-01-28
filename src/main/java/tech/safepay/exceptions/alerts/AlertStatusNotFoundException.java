package tech.safepay.exceptions.alerts;

/**
 * Exceção lançada quando o status de um alerta específico não é encontrado.
 *
 * <p>Esta exceção é normalmente utilizada em operações de consulta, validação ou auditoria
 * quando o identificador do alerta informado não corresponde a nenhum registro persistido.</p>
 *
 * <p>Permite diferenciar falhas de domínio de erros técnicos, facilitando o tratamento
 * centralizado através de {@code @ControllerAdvice}.</p>
 *
 * @author Tech SafePay
 * @since 1.0
 */
public class AlertStatusNotFoundException extends RuntimeException {

    /**
     * Construtor que cria uma nova exceção com uma mensagem descritiva.
     *
     * @param message Mensagem detalhando o motivo da exceção
     */
    public AlertStatusNotFoundException(String message) {
        super(message);
    }
}

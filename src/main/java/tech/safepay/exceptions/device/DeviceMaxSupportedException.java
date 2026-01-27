package tech.safepay.exceptions.device;

/**
 * Exceção lançada quando o número máximo de dispositivos
 * suportados ou permitidos para uma entidade é excedido.
 *
 * <p>Usada para garantir limites operacionais e regras
 * de negócio relacionadas à associação de dispositivos,
 * prevenindo abuso ou uso indevido.</p>
 *
 * <p>Ideal para validações prévias em fluxos de cadastro
 * ou vinculação de dispositivos.</p>
 */
public class DeviceMaxSupportedException extends RuntimeException {

    /**
     * Cria uma nova exceção indicando que o limite máximo
     * de dispositivos foi atingido.
     *
     * @param message mensagem descritiva do erro
     */
    public DeviceMaxSupportedException(String message) {
        super(message);
    }
}

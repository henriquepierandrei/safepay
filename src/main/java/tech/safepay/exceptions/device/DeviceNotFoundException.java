package tech.safepay.exceptions.device;

/**
 * Exceção lançada quando um dispositivo não é encontrado
 * no sistema.
 *
 * <p>Normalmente utilizada durante operações de busca,
 * validação ou associação, quando o identificador
 * informado não corresponde a nenhum registro persistido.</p>
 */
public class DeviceNotFoundException extends RuntimeException {

    /**
     * Cria uma nova exceção indicando que o dispositivo
     * não foi localizado.
     *
     * @param message mensagem descritiva do erro
     */
    public DeviceNotFoundException(String message) {
        super(message);
    }
}

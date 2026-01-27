package tech.safepay.exceptions.device;

/**
 * Exceção lançada quando um dispositivo existe,
 * mas não está vinculado à entidade esperada
 * (ex: cartão, usuário ou conta).
 *
 * <p>Usada para proteger fluxos que exigem
 * relacionamento explícito entre dispositivos
 * e outros domínios do sistema.</p>
 */
public class DeviceNotLinkedException extends RuntimeException {

    /**
     * Cria uma nova exceção indicando que o dispositivo
     * não está vinculado corretamente.
     *
     * @param message mensagem descritiva do erro
     */
    public DeviceNotLinkedException(String message) {
        super(message);
    }
}

package tech.safepay.websocket;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Componente responsável pelo controle centralizado do estado
 * de execução das transações processadas via WebSocket.
 *
 * <p>Este componente atua como um mecanismo de sincronização
 * thread-safe, permitindo pausar e retomar o processamento
 * de transações em tempo de execução.</p>
 *
 * <p>Principais objetivos:</p>
 * <ul>
 *   <li>Garantir controle operacional sobre o fluxo de transações</li>
 *   <li>Permitir intervenções administrativas seguras</li>
 *   <li>Evitar condições de corrida em ambientes concorrentes</li>
 * </ul>
 *
 * <p>A utilização de {@link AtomicBoolean} assegura consistência
 * do estado mesmo em cenários de alta concorrência.</p>
 */
@Component
public class TransactionExecutionControl {

    /**
     * Indica se o processamento de transações está pausado.
     *
     * <p>Valor {@code true} indica que o processamento está suspenso;
     * valor {@code false} indica que o processamento está ativo.</p>
     */
    private final AtomicBoolean paused = new AtomicBoolean(false);

    /**
     * Retorna o estado atual do processamento de transações.
     *
     * @return {@code true} se o processamento estiver pausado,
     *         {@code false} caso contrário
     */
    public boolean isPaused() {
        return paused.get();
    }

    /**
     * Pausa o processamento de transações.
     *
     * <p>Este método pode ser chamado de forma segura por múltiplas
     * threads sem risco de inconsistência de estado.</p>
     */
    public void pause() {
        paused.set(true);
    }

    /**
     * Retoma o processamento de transações previamente pausado.
     */
    public void resume() {
        paused.set(false);
    }
}

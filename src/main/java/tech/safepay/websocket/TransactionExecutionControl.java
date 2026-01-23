package tech.safepay.websocket;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TransactionExecutionControl {

    private final AtomicBoolean paused = new AtomicBoolean(false);

    public boolean isPaused() {
        return paused.get();
    }

    public void pause() {
        paused.set(true);
    }

    public void resume() {
        paused.set(false);
    }
}

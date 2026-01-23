package tech.safepay.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/control")
public class TransactionControlController {


    private static final Logger log = LoggerFactory.getLogger(TransactionControlController.class);
    private final TransactionExecutionControl control;

    public TransactionControlController(TransactionExecutionControl control) {
        this.control = control;
    }

    @PostMapping("/pause")
    public void pause() {
        control.pause();
        log.info("WebSocket Pausado.");
    }

    @PostMapping("/resume")
    public void resume() {
        control.resume();
        log.info("WebSocket Ativado.");
    }

    @GetMapping("/status")
    public boolean status() {
        log.info("WebSocket Status: " + (control.isPaused() ? "Pausado." : "Ativado."));
        return control.isPaused();
    }
}
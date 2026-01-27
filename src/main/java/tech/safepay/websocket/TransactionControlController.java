package tech.safepay.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsável pelo controle operacional da execução
 * de transações processadas via WebSocket.
 *
 * <p>Esta API expõe endpoints administrativos que permitem:</p>
 * <ul>
 *   <li>Pausar o processamento de transações</li>
 *   <li>Retomar o processamento de transações</li>
 *   <li>Consultar o status atual da execução</li>
 * </ul>
 *
 * <p>O objetivo principal é fornecer um mecanismo seguro de
 * governança operacional, permitindo intervenções manuais
 * em cenários como:</p>
 * <ul>
 *   <li>Manutenções programadas</li>
 *   <li>Incidentes operacionais</li>
 *   <li>Diagnóstico e troubleshooting</li>
 * </ul>
 *
 * <p>Todos os comandos são registrados em log para fins de
 * auditoria e observabilidade.</p>
 */
@RestController
@RequestMapping("/control")
public class TransactionControlController {

    private static final Logger log = LoggerFactory.getLogger(TransactionControlController.class);
    private final TransactionExecutionControl control;

    /**
     * Construtor responsável por injetar o controlador
     * de execução das transações.
     *
     * @param control componente que gerencia o estado de execução
     *                das transações via WebSocket
     */
    public TransactionControlController(TransactionExecutionControl control) {
        this.control = control;
    }

    /**
     * Pausa imediatamente o processamento de transações
     * via WebSocket.
     *
     * <p>Após a execução deste comando, novas transações
     * deixam de ser processadas até que o serviço seja retomado.</p>
     */
    @PostMapping("/pause")
    public void pause() {
        control.pause();
        log.info("WebSocket Pausado.");
    }

    /**
     * Retoma o processamento de transações via WebSocket
     * após uma pausa operacional.
     */
    @PostMapping("/resume")
    public void resume() {
        control.resume();
        log.info("WebSocket Ativado.");
    }

    /**
     * Retorna o status atual do processamento de transações.
     *
     * @return {@code true} se o processamento estiver pausado,
     *         {@code false} se estiver ativo
     */
    @GetMapping("/status")
    public boolean status() {
        log.info("WebSocket Status: " + (control.isPaused() ? "Pausado." : "Ativado."));
        return control.isPaused();
    }
}

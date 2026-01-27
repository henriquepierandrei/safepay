package tech.safepay.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listener responsável por monitorar eventos do ciclo de vida
 * das conexões WebSocket da aplicação.
 *
 * <p>Esta classe atua de forma observacional, capturando eventos
 * emitidos pelo Spring WebSocket/STOMP sempre que um cliente:</p>
 * <ul>
 *   <li>Estabelece uma nova conexão</li>
 *   <li>Encerra ou perde uma conexão existente</li>
 * </ul>
 *
 * <p>Seu principal objetivo é fornecer rastreabilidade operacional,
 * facilitando:</p>
 * <ul>
 *   <li>Auditoria de sessões ativas</li>
 *   <li>Diagnóstico de problemas de conectividade</li>
 *   <li>Monitoramento de uso em tempo real</li>
 * </ul>
 *
 * <p>Não possui impacto no fluxo de negócio ou no processamento
 * das mensagens, atuando exclusivamente como mecanismo de observabilidade.</p>
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    /**
     * Manipula o evento de conexão de uma nova sessão WebSocket.
     *
     * <p>Este método é acionado automaticamente pelo Spring
     * quando um cliente estabelece com sucesso uma conexão STOMP.</p>
     *
     * <p>O {@code sessionId} é extraído do cabeçalho da mensagem
     * e registrado para fins de rastreamento e auditoria.</p>
     *
     * @param event evento disparado no momento da conexão WebSocket
     */
    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        log.info("WebSocket conectado | sessionId="
                + event.getMessage().getHeaders().get("simpSessionId"));
    }

    /**
     * Manipula o evento de encerramento de uma sessão WebSocket.
     *
     * <p>Este método é executado quando um cliente encerra a conexão
     * de forma explícita ou quando ocorre uma desconexão inesperada.</p>
     *
     * <p>O {@code sessionId} é utilizado para correlação com logs
     * de conexão e possíveis eventos de negócio.</p>
     *
     * @param event evento disparado no momento da desconexão WebSocket
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.info("WebSocket desconectado | sessionId="
                + event.getSessionId());
    }
}

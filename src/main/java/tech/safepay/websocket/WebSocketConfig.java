package tech.safepay.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Configuração central da infraestrutura WebSocket da aplicação.
 *
 * <p>Esta classe define o setup do WebSocket utilizando o protocolo STOMP,
 * habilitando comunicação assíncrona em tempo real entre servidor e clientes.</p>
 *
 * <p>Responsabilidades principais:</p>
 * <ul>
 *   <li>Habilitar o broker de mensagens</li>
 *   <li>Definir prefixes de roteamento de mensagens</li>
 *   <li>Registrar endpoints WebSocket disponíveis para conexão</li>
 * </ul>
 *
 * <p>A configuração segue o modelo padrão do Spring WebSocket
 * com separação clara entre:</p>
 * <ul>
 *   <li><b>/app</b> → mensagens destinadas à aplicação (controllers)</li>
 *   <li><b>/topic</b> e <b>/queue</b> → mensagens distribuídas pelo broker</li>
 * </ul>
 *
 * <p>Suporta fallback via SockJS para garantir compatibilidade
 * com navegadores e ambientes legados.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura o message broker responsável pelo roteamento
     * das mensagens WebSocket.
     *
     * <p>Utiliza o broker simples embutido do Spring, adequado
     * para ambientes de desenvolvimento e cargas moderadas.</p>
     *
     * @param config registry de configuração do broker de mensagens
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita um message broker simples
        config.enableSimpleBroker("/topic", "/queue");

        // Define o prefixo para mensagens destinadas aos métodos @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registra os endpoints WebSocket expostos pela aplicação.
     *
     * <p>O endpoint {@code /ws} é o ponto de entrada para conexões
     * WebSocket/STOMP realizadas pelos clientes.</p>
     *
     * <p>Configurações aplicadas:</p>
     * <ul>
     *   <li>Permissão de origem via {@code setAllowedOriginPatterns}</li>
     *   <li>Fallback automático via SockJS</li>
     * </ul>
     *
     * @param registry registry responsável pelo registro de endpoints STOMP
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registra o endpoint WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Fallback para navegadores sem suporte a WebSocket
    }
}

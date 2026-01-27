package tech.safepay.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tech.safepay.dtos.transaction.TransactionResponseDto;

/**
 * Componente responsável pela publicação de eventos de transações
 * através do canal WebSocket da aplicação.
 *
 * <p>Este publisher atua como uma camada de abstração sobre o
 * {@link SimpMessagingTemplate}, centralizando a responsabilidade
 * de comunicação em tempo real com os clientes conectados.</p>
 *
 * <p>Principais responsabilidades:</p>
 * <ul>
 *   <li>Publicar transações processadas para subscribers WebSocket</li>
 *   <li>Garantir desacoplamento entre processamento e transporte</li>
 *   <li>Padronizar o canal de saída de eventos</li>
 * </ul>
 *
 * <p>O destino {@code /topic/transactions} segue o modelo
 * publish/subscribe, permitindo que múltiplos consumidores
 * recebam o mesmo evento simultaneamente.</p>
 *
 * <p>Este componente não contém regras de negócio e não realiza
 * validações, atuando exclusivamente como mecanismo de entrega.</p>
 */
@Component
public class TransactionWebSocketPublisher {

    /**
     * Template Spring responsável pelo envio de mensagens via WebSocket.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Construtor com injeção de dependência do template de mensageria.
     *
     * @param messagingTemplate template utilizado para envio de mensagens WebSocket
     */
    public TransactionWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publica uma transação processada para todos os clientes
     * inscritos no tópico de transações.
     *
     * <p>A mensagem é enviada de forma assíncrona seguindo o
     * padrão publish/subscribe.</p>
     *
     * @param transaction objeto de resposta da transação processada
     */
    public void publish(TransactionResponseDto transaction) {
        messagingTemplate.convertAndSend(
                "/topic/transactions",
                transaction
        );
    }
}

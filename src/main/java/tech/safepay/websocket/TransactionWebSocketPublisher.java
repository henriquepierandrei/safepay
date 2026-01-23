package tech.safepay.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tech.safepay.dtos.transaction.TransactionResponseDto;

@Component
public class TransactionWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public TransactionWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(TransactionResponseDto transaction) {
        messagingTemplate.convertAndSend(
                "/topic/transactions",
                transaction
        );
    }
}

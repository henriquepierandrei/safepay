package tech.safepay.websocket;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Recebe mensagens do cliente e envia para todos conectados
    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage message) {
        return message;
    }

    // Quando um usuário entra no chat
    @MessageMapping("/chat.join")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage message,
                               SimpMessageHeaderAccessor headerAccessor) {
        // Adiciona username na sessão websocket
        headerAccessor.getSessionAttributes().put("username", message.getSender());
        return message;
    }

    // Enviar mensagem para usuário específico
    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Payload ChatMessage message) {
        messagingTemplate.convertAndSendToUser(
                message.getSender(),
                "/queue/private",
                message
        );
    }
}
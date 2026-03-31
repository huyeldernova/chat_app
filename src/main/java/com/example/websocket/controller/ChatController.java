package com.example.websocket.controller;

import com.example.websocket.dto.request.ChatMessageRequest;
import com.example.websocket.exception.AppException;
import com.example.websocket.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j(topic = "CHAT")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Nhận tin nhắn từ client qua STOMP destination: /app/chat
     * Kết quả gửi lại tới từng participant qua: /user/{userId}/queue/chat
     */
    @MessageMapping("/chat")
    public void handleChat(@Payload ChatMessageRequest request, Principal principal) {
        String senderId = principal != null ? principal.getName() : null;

        if (senderId == null) {
            log.warn("Anonymous sender — message rejected");
            return;
        }

        try {
            var result = chatService.sendChatMessage(senderId, request);
            log.info("Message sent by {} to conversation {}", senderId, request.getConversationId());
        } catch (AppException ex) {
            log.warn("Chat error for user {}: {}", senderId, ex.getMessage());
            messagingTemplate.convertAndSendToUser(
                    senderId, "/queue/errors", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error processing chat for user {}", senderId, ex);
            messagingTemplate.convertAndSendToUser(
                    senderId, "/queue/errors", "Internal server error");
        }
    }
}
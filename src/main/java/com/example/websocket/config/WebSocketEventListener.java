package com.example.websocket.config;

import com.example.websocket.dto.response.OnlineStatusResponse;
import com.example.websocket.entity.SocketSession;
import com.example.websocket.repository.SocketSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SocketSessionRepository socketSessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onConnected(SessionConnectEvent connectEvent){

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(connectEvent.getMessage());

        String sessionId = accessor.getSessionId();

        String userId = accessor.getUser() != null
                ? accessor.getUser().getName()
                : null;

        if (userId == null) {
            log.warn("Disconnected session has no user");
            return;
        }

        SocketSession session = SocketSession.builder()
                .socketSessionId(sessionId)
                .userId(userId)
                .connectedAt(LocalDateTime.now())
                .build();

        socketSessionRepository.save(session);

        messagingTemplate.convertAndSend("/topic/online-status",
                OnlineStatusResponse.builder()
                        .userId(userId)
                        .online(true)
                        .build()
        );
        log.info("User {} connected", userId);

    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent disconnectEvent) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(disconnectEvent.getMessage());
        String userId = accessor.getUser().getName();
        String sessionId = accessor.getSessionId();

        socketSessionRepository.deleteById(sessionId);

        List<SocketSession> remainingSessions = socketSessionRepository.findByUserId(userId);


        if (remainingSessions.isEmpty()) {
            // Hết tất cả session → thật sự offline
            messagingTemplate.convertAndSend("/topic/online-status",
                    OnlineStatusResponse.builder()
                            .userId(userId)
                            .online(false)
                            .build()
            );
            log.info("User {} is now offline", userId);
        } else {
            log.info("User {} still has {} active sessions",
                    userId, remainingSessions.size());
        }
    }
}

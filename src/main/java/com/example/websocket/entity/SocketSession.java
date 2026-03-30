package com.example.websocket.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;

@RedisHash("socket_session")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SocketSession {

    @Id
    private String socketSessionId;

    @Indexed
    private String userId;

    @Builder.Default
    private LocalDateTime connectedAt = LocalDateTime.now();

}

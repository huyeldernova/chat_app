package com.example.websocket.dto.token;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtInfo implements Serializable {
    private String jwtId;
    private LocalDateTime issueTime;
    private LocalDateTime expirationTime;
}


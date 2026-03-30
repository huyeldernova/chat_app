package com.example.websocket.dto.token;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenPayload implements Serializable {
    private String jwtId;
    private String token;
    private Date issueTime;
    private Date expiredTime;
}

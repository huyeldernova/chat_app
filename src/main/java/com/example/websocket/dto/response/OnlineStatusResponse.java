package com.example.websocket.dto.response;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineStatusResponse {
    private String userId;
    private boolean online;
}

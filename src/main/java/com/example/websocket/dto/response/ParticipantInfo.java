package com.example.websocket.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ParticipantInfo {
    private String userId;
    private String username;
    private boolean me;
}

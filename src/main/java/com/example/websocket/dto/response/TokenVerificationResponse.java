package com.example.websocket.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
public class TokenVerificationResponse implements Serializable {
    private boolean isValid;
    private List<String> authorities;
}

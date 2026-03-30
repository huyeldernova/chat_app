package com.example.websocket.dto.response;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class UserDetailResponse {
    private String id;
    private String email;
    private String username;
    private List<String> roles;
}


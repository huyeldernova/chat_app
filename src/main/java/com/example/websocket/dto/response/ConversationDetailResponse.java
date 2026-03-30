package com.example.websocket.dto.response;

import com.example.websocket.common.ConversationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationDetailResponse {
    private String id;
    private ConversationType conversationType;
    private String conversationName;
    private String conversationAvatar;
    private List<ParticipantInfo> participants;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private LocalDateTime lastActivityTime;
}
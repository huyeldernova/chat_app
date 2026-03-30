package com.example.websocket.dto.response;


import com.example.websocket.common.ConversationType;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationCreationResponse implements Serializable {

    private String id;
    private ConversationType conversationType;
    private String participantHash;
    private String conversationAvatar;
    private String conversationName;
    private List<ParticipantInfo> participantInfo;
    private LocalDateTime createdAt;
    private String conversationCreatedBy;
}
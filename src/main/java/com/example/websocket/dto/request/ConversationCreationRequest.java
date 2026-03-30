package com.example.websocket.dto.request;

import com.example.websocket.common.ConversationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConversationCreationRequest {

    @NotNull(message = "Conversation Type cannot be blank")
    private ConversationType conversationType;

    @Size(max = 100, message = "Conversation Name must be less than 100 characters")
    private String conversationName;

    private String conversationAvatar;

    @NotEmpty(message = "Participant cannot be null")
    @Size(min = 1,  message = "At least 1 participants are required")
    private List<String> participantIds;

}

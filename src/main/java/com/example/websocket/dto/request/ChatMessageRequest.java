package com.example.websocket.dto.request;

import com.example.websocket.common.MessageType;
import com.example.websocket.entity.MessageMedia;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class ChatMessageRequest implements Serializable {
    private String tempId;
    private String conversationId;
    private String message;
    private List<Long> mediaIds;
    private MessageType messageType;
}

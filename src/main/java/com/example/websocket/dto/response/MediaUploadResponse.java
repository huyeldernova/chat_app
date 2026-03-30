package com.example.websocket.dto.response;

import com.example.websocket.common.MessageType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MediaUploadResponse {
    private Long id;
    private String url;
    private String fileName;
    private MessageType mediaType;
    private String mimeType;
    private Long fileSize;
}
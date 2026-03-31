package com.example.websocket.controller;

import com.example.websocket.dto.request.ConversationCreationRequest;
import com.example.websocket.dto.response.*;
import com.example.websocket.service.ChatService;
import com.example.websocket.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatService chatService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ApiResponse<ConversationCreationResponse> createConversation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ConversationCreationRequest request) {

        var userId = jwt.getSubject();
        var result = conversationService.createConversation(userId, request);
        return ApiResponse.<ConversationCreationResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Conversation created successfully")
                .result(result)
                .build();
    }

    @GetMapping("/my-conversation")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ApiResponse<PageResponse<ConversationDetailResponse>> myConversation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "")   String conversationType) {

        var userId = jwt.getSubject();
        var result = conversationService.myConversation(userId, page, size, conversationType);
        return ApiResponse.<PageResponse<ConversationDetailResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Conversation list retrieved successfully")
                .result(result)
                .build();
    }

    @GetMapping("/{conversationId}/messages")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ApiResponse<PageResponse<ChatMessageResponse>> getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var userId = jwt.getSubject();
        var result = chatService.getMessagesByConversationId(userId, conversationId, page, size);
        return ApiResponse.<PageResponse<ChatMessageResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Messages retrieved successfully")
                .result(result)
                .build();
    }
}
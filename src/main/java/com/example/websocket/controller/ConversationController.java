package com.example.websocket.controller;

import com.example.websocket.dto.request.ConversationCreationRequest;
import com.example.websocket.dto.response.ApiResponse;
import com.example.websocket.dto.response.ConversationCreationResponse;
import com.example.websocket.dto.response.ConversationDetailResponse;
import com.example.websocket.dto.response.PageResponse;
import com.example.websocket.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    ApiResponse<ConversationCreationResponse> createConversation(
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
    ApiResponse<PageResponse<ConversationDetailResponse>> myConversation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String conversationType) {

        var userId = jwt.getSubject();
        var result = conversationService.myConversation(userId, page, size, conversationType);
        return ApiResponse.<PageResponse<ConversationDetailResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Conversation list retrieved successfully")
                .result(result)
                .build();
    }

}
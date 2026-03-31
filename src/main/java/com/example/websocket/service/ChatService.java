package com.example.websocket.service;


import com.example.websocket.common.MessageType;
import com.example.websocket.dto.request.ChatMessageRequest;
import com.example.websocket.dto.response.ChatMessageResponse;
import com.example.websocket.dto.response.PageResponse;
import com.example.websocket.entity.*;
import com.example.websocket.exception.AppException;
import com.example.websocket.exception.ErrorCode;
import com.example.websocket.repository.ChatMessageRepository;
import com.example.websocket.repository.ConversationRepository;
import com.example.websocket.repository.MessageMediaRepository;
import com.example.websocket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j( topic = "CHAT-SERVICE")
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageMediaRepository messageMediaRepository;

    @Transactional
    public ChatMessageResponse sendChatMessage(String senderId, ChatMessageRequest request) {

        var conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // ① Load và validate media TRƯỚC khi tạo message
        List<MessageMedia> mediaFiles = new ArrayList<>();
        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            mediaFiles = messageMediaRepository.findAllById(request.getMediaIds());

            // Validate đủ số lượng
            if (mediaFiles.size() != request.getMediaIds().size()) {
                throw new AppException(ErrorCode.FILE_NOT_FOUND);
            }

            // Validate ownership
            boolean hasUnauthorized = mediaFiles.stream()
                    .anyMatch(m -> !m.getUploadedBy().equals(senderId));
            if (hasUnauthorized) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        }

        // ② Tạo message
        ChatMessage chatMessage = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getMessage() != null && !request.getMessage().isBlank()
                        ? request.getMessage()
                        : "[Media]")
                .messageType(request.getMessageType() != null
                        ? request.getMessageType()
                        : MessageType.TEXT)
                .build();

        chatMessageRepository.save(chatMessage);

        // ③ Link media: update DB + update in-memory cùng lúc — KHÔNG cần reload
        if (!mediaFiles.isEmpty()) {
            for (MessageMedia media : mediaFiles) {
                media.setMessage(chatMessage);
                media.setStatus(Status.ACTIVE);
            }
            messageMediaRepository.saveAll(mediaFiles);
            chatMessage.setMediaFiles(mediaFiles); // ← sync in-memory với DB
        }

        conversation.setLastMessageContent(chatMessage.getContent());
        conversation.setLastMessageTime(chatMessage.getSentAt());
        conversationRepository.save(conversation);

        log.info("Message saved successfully");

        List<String> recipientIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .toList();

        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .tempId(request.getTempId())
                .conversationId(conversation.getId())
                .conversationAvatar(conversation.getConversationAvatar())
                .username(sender.getUsername())
                .conversationType(conversation.getConversationType() != null
                        ? conversation.getConversationType().name()
                        : null)
                .message(chatMessage.getContent())
                .messageMedia(chatMessage.getMediaFiles()) // ← đã populated, không cần reload
                .messageType(chatMessage.getMessageType())
                .participants(recipientIds)
                .createdAt(chatMessage.getSentAt())
                .conversationCreatedBy(senderId)
                .build();

        recipientIds.forEach(uid -> {
            try {
                messagingTemplate.convertAndSendToUser(
                        uid,
                        "/queue/chat",
                        response.toBuilder().me(uid.equals(sender.getId())).build());
            } catch (MessagingException e) {
                log.warn("Failed to send realtime message to user {}: {}", uid, e.getMessage());
            }
        });

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> getMessagesByConversationId(String userId, String conversationId, int page, int size) {


        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ChatMessage> chatMessagePage = chatMessageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable);

        List<ChatMessage> chatMessages = new ArrayList<>(chatMessagePage.getContent());
        Collections.reverse(chatMessages);

        return PageResponse.<ChatMessageResponse>builder()
                .currentPages(page)
                .pageSizes(size)
                .totalPages(chatMessagePage.getTotalPages())
                .totalElements(chatMessagePage.getTotalElements())
                .data(chatMessages.stream().map(message -> ChatMessageResponse.builder()
                        .id(message.getId())
                        .conversationId(conversation.getId())
                        .me(message.getSender().getId().equals(userId))
                        .username(message.getSender().getUsername())
                        .message(message.getContent())
                        .messageType(message.getMessageType())
                        .createdAt(message.getSentAt())
                        .messageMedia(message.getMediaFiles())
                        .build()).toList())
                .build();
    }

}

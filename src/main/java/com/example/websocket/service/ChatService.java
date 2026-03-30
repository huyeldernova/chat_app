package com.example.websocket.service;


import com.example.websocket.dto.request.ChatMessageRequest;
import com.example.websocket.dto.response.ChatMessageResponse;
import com.example.websocket.dto.response.PageResponse;
import com.example.websocket.entity.ChatMessage;
import com.example.websocket.entity.Conversation;
import com.example.websocket.entity.User;
import com.example.websocket.exception.AppException;
import com.example.websocket.exception.ErrorCode;
import com.example.websocket.repository.ChatMessageRepository;
import com.example.websocket.repository.ConversationRepository;
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
    private final MediaService mediaService;

    @Transactional
    public ChatMessageResponse sendChatMessage(String senderId, ChatMessageRequest request) {


        var conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ChatMessage chatMessage = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getMessage())
                .messageType(request.getMessageType() != null ? request.getMessageType() : com.example.websocket.common.MessageType.TEXT)
                .build();

        chatMessageRepository.save(chatMessage);

        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            mediaService.linkFilesToMessage(
                    request.getMediaIds(),
                    chatMessage,
                    senderId
            );
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
                    .conversationType(conversation.getConversationType() != null ? conversation.getConversationType().name() : null)
                    .message(chatMessage.getContent())
                    .messageMedia(chatMessage.getMediaFiles())
                    .messageType(chatMessage.getMessageType())
                    .participants(recipientIds)
                    .createdAt(chatMessage.getSentAt())
                    .conversationCreatedBy(senderId)
                    .build();

            recipientIds.forEach(uid -> {
                try {
                    ChatMessageResponse perUserResponse = response.toBuilder()
                            .me(uid.equals(sender.getId()))
                            .build();
                    messagingTemplate.convertAndSendToUser(
                            uid,
                            "/queue/chat",
                            perUserResponse);
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

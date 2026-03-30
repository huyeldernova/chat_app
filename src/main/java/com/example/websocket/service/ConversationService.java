package com.example.websocket.service;


import com.example.websocket.common.ConversationType;
import com.example.websocket.dto.request.ConversationCreationRequest;
import com.example.websocket.dto.response.ConversationCreationResponse;
import com.example.websocket.dto.response.ConversationDetailResponse;
import com.example.websocket.dto.response.PageResponse;
import com.example.websocket.dto.response.ParticipantInfo;
import com.example.websocket.entity.Conversation;
import com.example.websocket.entity.ConversationParticipant;
import com.example.websocket.entity.User;
import com.example.websocket.exception.AppException;
import com.example.websocket.exception.ErrorCode;
import com.example.websocket.mapper.ConversationMapper;
import com.example.websocket.repository.ConversationRepository;
import com.example.websocket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CONVERSATION-SERVICE")
public class ConversationService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;


    public ConversationCreationResponse createConversation(String userId, ConversationCreationRequest request) {

        List<String> participantIds = request.getParticipantIds();
        if(!participantIds.contains(userId)) {
            participantIds.add(userId);
        }

        List<User> participantInfos = userRepository.findAllById(participantIds);

        String participantHash = request.getConversationType() == ConversationType.PRIVATE && !participantInfos.isEmpty() && participantInfos.size() <= 2
                ? participantIds.stream().sorted().collect(Collectors.joining("-"))
                : null;

        if(request.getConversationType() == ConversationType.PRIVATE) {
            if(participantInfos.size() > 2) {
                log.info("Private conversation maximum two participants");
                throw new AppException(ErrorCode.PRIVATE_CONVERSATION_MAX_TWO_PARTICIPANTS);
            }

            Optional<Conversation> conversation = conversationRepository.findByParticipantHash(participantHash);

            if (conversation.isPresent()) {
                log.info("Conversation existed");
                return ConversationMapper.mapToConversationResponse(conversation.get(), userId);
            }
        }

        if (request.getConversationType() == ConversationType.GROUP) {
            if (request.getConversationName() == null || request.getConversationName().trim().isEmpty()) {
                log.info("Conversation name is required");
                throw new AppException(ErrorCode.CONVERSATION_NAME_REQUIRED);
            }
            if (participantIds.size() < 3) {
                log.info("Group conversation minimum three participants");
                throw new AppException(ErrorCode.GROUP_CONVERSATION_MINIMUM_THREE_PARTICIPANTS);
            }
        }

        Conversation conversation = Conversation.builder()
                .name(request.getConversationName())
                .conversationType(request.getConversationType())
                .conversationAvatar(request.getConversationAvatar())
                .participantHash(participantHash)
                .createdAt(LocalDateTime.now())
                .build();

        List<ConversationParticipant> conversationParticipants = participantInfos.stream()
                .map(user -> ConversationParticipant.builder()
                        .conversation(conversation)
                        .user(user)
                        .build())
                .toList();

        conversation.setParticipants(conversationParticipants);
        conversationRepository.save(conversation);

        return ConversationMapper.mapToConversationResponse(conversation, userId);
    }

    public PageResponse<ConversationDetailResponse> myConversation(String userId, int page, int size, String conversationType) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("lastMessageTime").descending());

        Page<Conversation> conversationsPage;
        if (conversationType == null || conversationType.trim().isEmpty()) {
            conversationsPage = conversationRepository.findByParticipantsUserId(userId, pageable);
        } else {
            ConversationType type = ConversationType.valueOf(conversationType);
            conversationsPage = conversationRepository.findByConversationTypeAndParticipantsUserId(type, userId, pageable);
        }

        List<ConversationDetailResponse> data = conversationsPage.getContent().stream().map(conv -> {
            var participants = conv.getParticipants().stream()
                    .map(p -> ParticipantInfo.builder()
                            .userId(p.getUser().getId())
                            .username(p.getUser().getUsername())
                            .me(p.getUser().getId().equals(userId))
                            .build())
                    .toList();

            String displayName;
            if (conv.getConversationType() == ConversationType.GROUP) {
                displayName = conv.getName();
            } else {
                if (conv.getParticipants().size() == 1) {
                    var single = conv.getParticipants().getFirst().getUser();
                    displayName = single.getUsername() != null ? single.getUsername() : single.getEmail();
                } else {
                    displayName = conv.getParticipants().stream()
                            .map(ConversationParticipant::getUser)
                            .filter(u -> ! u.getId().equals(userId))
                            .findFirst()
                            .map(u -> u.getUsername() != null ? u.getUsername() : u.getEmail())
                            .orElse(null);
                }
            }
            return ConversationDetailResponse.builder()
                    .id(conv.getId())
                    .conversationType(conv.getConversationType())
                    .conversationName(displayName)
                    .conversationAvatar(conv.getConversationAvatar())
                    .participants(participants)
                    .lastMessageContent(conv.getLastMessageContent())
                    .lastMessageTime(conv.getLastMessageTime())
                    .lastActivityTime(conv.getLastMessageTime() != null ? conv.getLastMessageTime() : conv.getCreatedAt())
                    .build();
        }).toList();

        return PageResponse.<ConversationDetailResponse>builder()
                .currentPages(page)
                .pageSizes(size)
                .totalPages(conversationsPage.getTotalPages())
                .totalElements(conversationsPage.getTotalElements())
                .data(data)
                .build();
    }

}

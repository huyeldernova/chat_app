package com.example.websocket.mapper;

import com.example.websocket.common.ConversationType;
import com.example.websocket.dto.response.ConversationCreationResponse;
import com.example.websocket.dto.response.ParticipantInfo;
import com.example.websocket.entity.Conversation;

public class ConversationMapper {

    private ConversationMapper() {
    }

    public static ConversationCreationResponse mapToConversationResponse(Conversation conversation, String userId) {
        ConversationCreationResponse response = ConversationCreationResponse.builder()
                .id(conversation.getId())
                .conversationType(conversation.getConversationType())
                .participantHash(conversation.getParticipantHash())
                .participantInfo(conversation.getParticipants().stream()
                        .map(participantInfo -> ParticipantInfo.builder()
                                .userId(participantInfo.getId())
                                .username(participantInfo.getUser().getUsername())
                                .me(participantInfo.getUser().getId().equals(userId))
                                .build())
                        .toList())
                .createdAt(conversation.getCreatedAt())
                .conversationCreatedBy(userId)
                .build();

        if(conversation.getConversationType() == ConversationType.GROUP) {
            response.setConversationName(conversation.getName());
            response.setConversationAvatar(conversation.getConversationAvatar());
        } else {
            if(conversation.getParticipants().size() == 1) {
                var single = conversation.getParticipants().getFirst().getUser();
                response.setConversationName(single.getUsername() != null ? single.getUsername() : single.getEmail());
            } else {
                conversation.getParticipants().stream()
                        .filter(participantInfo -> ! participantInfo.getUser().getId().equals(userId))
                        .findFirst().ifPresent(participantInfo -> {
                            var other = participantInfo.getUser();
                            response.setConversationName(other.getUsername() != null ? other.getUsername() : other.getEmail());
                        });
            }
        }
        return response;
    }

}

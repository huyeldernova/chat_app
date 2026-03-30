package com.example.websocket.repository;

import com.example.websocket.common.ConversationType;
import com.example.websocket.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    Optional<Conversation> findByParticipantHash(String participantHash);
    Page<Conversation> findByParticipantsUserId(String userId, Pageable pageable);
    Page<Conversation> findByConversationTypeAndParticipantsUserId(ConversationType conversationType, String userId, Pageable pageable);
}
package com.example.websocket.repository;

import com.example.websocket.entity.MessageMedia;
import com.example.websocket.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageMediaRepository extends JpaRepository<MessageMedia, Long> {

    // Tìm files TEMP quá hạn → CleanupJob dùng
    List<MessageMedia> findByStatusAndUploadedAtBefore(Status status, LocalDateTime time);

    // Tìm files theo message
    List<MessageMedia> findByMessageId(String messageId);
}
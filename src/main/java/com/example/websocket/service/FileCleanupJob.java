package com.example.websocket.service;

import com.example.websocket.entity.MessageMedia;
import com.example.websocket.entity.Status;
import com.example.websocket.repository.MessageMediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileCleanupJob {

    private final MessageMediaRepository messageMediaRepository;
    private final MediaService mediaService;

    // Chạy mỗi ngày lúc 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupTempFiles() {

        // File TEMP quá 1 tiếng → mồ côi
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        List<MessageMedia> orphanedFiles = messageMediaRepository
                .findByStatusAndUploadedAtBefore(Status.TEMP, cutoff);

        if (orphanedFiles.isEmpty()) {
            log.info("No orphaned files to clean up");
            return;
        }

        log.info("Cleaning up {} orphaned files", orphanedFiles.size());

        orphanedFiles.forEach(media -> {
            try {
                mediaService.deleteFile(media.getId(), media.getUploadedBy());
            } catch (Exception e) {
                log.warn("Failed to delete file {}: {}", media.getId(), e.getMessage());
            }
        });

        log.info("Cleanup completed");
    }
}
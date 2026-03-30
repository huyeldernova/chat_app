package com.example.websocket.service;

import com.example.websocket.common.MessageType;
import com.example.websocket.config.S3Properties;
import com.example.websocket.dto.response.MediaUploadResponse;
import com.example.websocket.entity.ChatMessage;
import com.example.websocket.entity.MessageMedia;
import com.example.websocket.entity.Status;
import com.example.websocket.exception.AppException;
import com.example.websocket.exception.ErrorCode;
import com.example.websocket.repository.MessageMediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final MessageMediaRepository messageMediaRepository;

    // ── Upload nhiều file ──
    public List<MediaUploadResponse> uploadFiles(
            List<MultipartFile> files,
            String uploadedBy
    ) {
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        return files.stream()
                .map(file -> uploadSingleFile(file, uploadedBy))
                .toList();
    }

    // ── Upload 1 file ──
    public MediaUploadResponse uploadSingleFile(
            MultipartFile file,
            String uploadedBy
    ) {
        validateFile(file);

        String s3Key = null;
        try {
            s3Key = generateS3Key(file);
            String url = uploadToS3(s3Key, file);

            MessageType mediaType = detectMediaType(file.getContentType());

            MessageMedia media = MessageMedia.builder()
                    .fileName(file.getOriginalFilename())
                    .url(url)
                    .s3Key(s3Key)
                    .mediaType(mediaType)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedBy(uploadedBy)
                    .status(Status.TEMP) // ← chưa link với message
                    .build();

            messageMediaRepository.save(media);

            log.info("File uploaded: {} by {}", url, uploadedBy);

            return MediaUploadResponse.builder()
                    .id(media.getId())
                    .url(url)
                    .fileName(file.getOriginalFilename())
                    .mediaType(mediaType)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

        } catch (AppException e) {
            // Nếu lưu DB thất bại → xóa file S3 vừa upload
            if (s3Key != null) tryDeleteFromS3(s3Key);
            throw e;
        }
    }

    // ── Link files với message → ACTIVE ──
    public void linkFilesToMessage(List<Long> mediaIds, ChatMessage message, String requesterId) {
        if (mediaIds == null || mediaIds.isEmpty()) return;

        List<MessageMedia> mediaList =
                messageMediaRepository.findAllById(mediaIds);

        // Check tất cả files thuộc về người gửi
        boolean hasUnauthorized = mediaList.stream()
                .anyMatch(m -> !m.getUploadedBy().equals(requesterId));
        if (hasUnauthorized) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        mediaList.forEach(media -> {
            media.setMessage(message);
            media.setStatus(Status.ACTIVE); // ← TEMP → ACTIVE
        });

        messageMediaRepository.saveAll(mediaList);
        log.info("Linked {} files to message {}", mediaIds.size(), message.getId());
    }

    // ── Xóa file ──
    public void deleteFile(Long mediaId, String requesterId) {
        MessageMedia media = messageMediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        if (!media.getUploadedBy().equals(requesterId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        tryDeleteFromS3(media.getS3Key());
        messageMediaRepository.delete(media);

        log.info("Deleted file: {}", media.getS3Key());
    }

    // ── Private methods ──

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > s3Properties.getMaxSize()) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        boolean allowed = contentType.startsWith("image/")
                || contentType.startsWith("video/")
                || contentType.equals("application/pdf");

        if (!allowed) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String generateS3Key(MultipartFile file) {
        String original = file.getOriginalFilename();
        String extension = "";

        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf("."));
        }

        String contentType = file.getContentType();
        String folder;

        if (contentType.startsWith("image/")) {
            folder = "chat/images";
        } else if (contentType.startsWith("video/")) {
            folder = "chat/videos";
        } else {
            folder = "chat/files";
        }

        return folder + "/" + UUID.randomUUID() + extension;
    }

    private String uploadToS3(String s3Key, MultipartFile file) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.getBucketName())
                            .key(s3Key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(
                            file.getInputStream(), file.getSize())
            );

            return String.format(
                    "https://%s.s3.%s.amazonaws.com/%s",
                    s3Properties.getBucketName(),
                    s3Properties.getRegion(),
                    s3Key
            );

        } catch (IOException e) {
            log.error("S3 upload failed", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private MessageType detectMediaType(String contentType) {
        if (contentType.startsWith("image/")) return MessageType.IMAGE;
        if (contentType.startsWith("video/")) return MessageType.VIDEO;
        return MessageType.FILE;
    }

    private void tryDeleteFromS3(String s3Key) {
        try {
            s3Client.deleteObject(b -> b
                    .bucket(s3Properties.getBucketName())
                    .key(s3Key)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete from S3: {}", s3Key);
            // CleanupJob sẽ dọn sau!
        }
    }
}

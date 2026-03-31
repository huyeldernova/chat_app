package com.example.websocket.controller;

import com.example.websocket.dto.response.MediaUploadResponse;
import com.example.websocket.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    /**
     * POST /api/media/upload
     * Upload nhiều file (image/video/pdf) — trả về danh sách với status=TEMP
     * File sẽ chuyển ACTIVE khi được link vào tin nhắn qua WebSocket
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<List<MediaUploadResponse>> uploadFiles(
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        List<MediaUploadResponse> responses = mediaService.uploadFiles(files, userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * DELETE /api/media/{mediaId}
     * Xóa file theo ID — chỉ người upload mới xóa được (MediaService kiểm tra)
     */
    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        mediaService.deleteFile(mediaId, userId);
        return ResponseEntity.noContent().build();
    }
}
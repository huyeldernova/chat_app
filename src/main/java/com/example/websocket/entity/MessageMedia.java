package com.example.websocket.entity;

import com.example.websocket.common.MessageType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_media")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MessageMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private ChatMessage message;

    @Column(nullable = false)
    private String fileName;

    private String url;

    private String thumbnailUrl;

    private String s3Key;

    private String mimeType;

    private Long fileSize;

    @Enumerated(EnumType.STRING)
    private MessageType mediaType;

    private String uploadedBy;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.TEMP;

    @Column(name = "uploaded_at", nullable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
package com.sonsminpark.auratalkback.domain.chat.entity;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_files")
public class ChatFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = true)
    private ChatMessage message;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private String s3Url;

    @Column(nullable = false)
    private String fileExtension;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isImage() {
        return mimeType.startsWith("image/");
    }

    public boolean isVideo() {
        return mimeType.startsWith("video/");
    }

    public boolean isAudio() {
        return mimeType.startsWith("audio/");
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
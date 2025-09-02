package com.sonsminpark.auratalkback.domain.chat.repository;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatFileRepository extends JpaRepository<ChatFile, Long> {

    @Query("SELECT cf FROM ChatFile cf " +
            "JOIN FETCH cf.uploader u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "WHERE cf.chatRoom.id = :chatroomId AND cf.isDeleted = false " +
            "ORDER BY cf.createdAt DESC")
    Page<ChatFile> findByChatRoomIdWithUploader(
            @Param("chatroomId") Long chatroomId,
            Pageable pageable);

    @Query("SELECT cf FROM ChatFile cf " +
            "WHERE cf.id = :fileId AND cf.uploader.id = :uploaderId AND cf.isDeleted = false")
    Optional<ChatFile> findByIdAndUploaderIdAndIsDeletedFalse(
            @Param("fileId") Long fileId,
            @Param("uploaderId") Long uploaderId);

    @Query("SELECT cf FROM ChatFile cf " +
            "WHERE cf.id = :fileId AND cf.isDeleted = false")
    Optional<ChatFile> findByIdAndIsDeletedFalse(@Param("fileId") Long fileId);

    @Query("SELECT cf FROM ChatFile cf " +
            "JOIN FETCH cf.uploader u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "WHERE cf.chatRoom.id = :chatroomId AND cf.mimeType LIKE 'image/%' AND cf.isDeleted = false " +
            "ORDER BY cf.createdAt DESC")
    Page<ChatFile> findImagesByChatRoomIdWithUploader(@Param("chatroomId") Long chatroomId, Pageable pageable);

    @Query("SELECT cf FROM ChatFile cf " +
            "JOIN FETCH cf.uploader u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "WHERE cf.chatRoom.id = :chatroomId AND cf.mimeType LIKE 'video/%' AND cf.isDeleted = false " +
            "ORDER BY cf.createdAt DESC")
    Page<ChatFile> findVideosByChatRoomIdWithUploader(@Param("chatroomId") Long chatroomId, Pageable pageable);

    @Query("SELECT cf FROM ChatFile cf " +
            "JOIN FETCH cf.uploader u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "WHERE cf.chatRoom.id = :chatroomId AND cf.mimeType LIKE 'audio/%' AND cf.isDeleted = false " +
            "ORDER BY cf.createdAt DESC")
    Page<ChatFile> findAudiosByChatRoomIdWithUploader(@Param("chatroomId") Long chatroomId, Pageable pageable);

    @Query("SELECT cf FROM ChatFile cf " +
            "JOIN FETCH cf.uploader u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "WHERE cf.chatRoom.id = :chatroomId AND cf.isDeleted = false " +
            "AND (cf.mimeType LIKE 'application/%' OR cf.mimeType LIKE 'text/%') " +
            "ORDER BY cf.createdAt DESC")
    Page<ChatFile> findDocumentsByChatRoomIdWithUploader(@Param("chatroomId") Long chatroomId, Pageable pageable);

    @Query("SELECT cf FROM ChatFile cf " +
            "JOIN FETCH cf.uploader u " +
            "LEFT JOIN FETCH u.userProfileImage " +
            "WHERE cf.chatRoom.id = :chatroomId AND cf.isDeleted = false " +
            "AND cf.mimeType NOT LIKE 'image/%' " +
            "AND cf.mimeType NOT LIKE 'video/%' " +
            "AND cf.mimeType NOT LIKE 'audio/%' " +
            "AND cf.mimeType NOT LIKE 'application/%' " +
            "AND cf.mimeType NOT LIKE 'text/%' " +
            "ORDER BY cf.createdAt DESC")
    Page<ChatFile> findOthersByChatRoomIdWithUploader(@Param("chatroomId") Long chatroomId, Pageable pageable);

    @Query("SELECT cf FROM ChatFile cf " +
            "WHERE cf.chatRoom.id = :chatroomId AND cf.mimeType LIKE 'image/%' AND cf.isDeleted = false " +
            "ORDER BY cf.createdAt DESC")
    List<ChatFile> findImagesByChatRoomId(@Param("chatroomId") Long chatroomId);

    @Query("SELECT cf FROM ChatFile cf " +
            "WHERE cf.chatRoom.id = :chatroomId AND cf.mimeType LIKE 'video/%' AND cf.isDeleted = false " +
            "ORDER BY cf.createdAt DESC")
    List<ChatFile> findVideosByChatRoomId(@Param("chatroomId") Long chatroomId);
}
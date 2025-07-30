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
            "WHERE cf.chatRoom.id = :chatroomId AND cf.isDeleted = false " +
            "ORDER BY cf.createdAt DESC")
    Page<ChatFile> findByChatRoomIdAndIsDeletedFalse(
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
            "WHERE cf.chatRoom.id = :chatroomId AND cf.mimeType LIKE 'image/%' AND cf.isDeleted = false " +
            "ORDER BY cf.createdAt DESC")
    List<ChatFile> findImagesByChatRoomId(@Param("chatroomId") Long chatroomId);

    @Query("SELECT cf FROM ChatFile cf " +
            "WHERE cf.chatRoom.id = :chatroomId AND cf.mimeType LIKE 'video/%' AND cf.isDeleted = false " +
            "ORDER BY cf.createdAt DESC")
    List<ChatFile> findVideosByChatRoomId(@Param("chatroomId") Long chatroomId);
}
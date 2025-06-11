package com.sonsminpark.auratalkback.domain.chat.entity;

public enum MessageType {
    TEXT,           // 일반 텍스트 메시지
    FILE,           // 파일 전송
    IMAGE,          // 이미지 전송
    VIDEO,          // 비디오 전송
    VOICE,          // 음성 메시지
    SYSTEM,         // 시스템 메시지
    INVITE          // 초대 링크 메시지
}
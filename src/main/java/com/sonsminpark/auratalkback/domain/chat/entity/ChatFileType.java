package com.sonsminpark.auratalkback.domain.chat.entity;

public enum ChatFileType {
    ALL("전체"),
    IMAGE("이미지"),
    VIDEO("비디오"),
    AUDIO("오디오"),
    DOCUMENT("문서"),
    OTHER("기타");

    private final String description;

    ChatFileType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
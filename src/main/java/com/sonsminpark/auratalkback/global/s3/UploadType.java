package com.sonsminpark.auratalkback.global.s3;

public enum UploadType {

    PROFILE("profile-images/original/"),
    GROUP("group-images/original/"),
    CHAT("chat-files/");

    private final String prefix;

    UploadType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}


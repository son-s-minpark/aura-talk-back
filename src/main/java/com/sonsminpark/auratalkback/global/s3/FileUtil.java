package com.sonsminpark.auratalkback.global.s3;

import java.util.HashMap;
import java.util.Map;

public class FileUtil {

    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<>();

    static {
        MIME_TYPE_MAP.put("jpg", "image/jpeg");
        MIME_TYPE_MAP.put("jpeg", "image/jpeg");
        MIME_TYPE_MAP.put("png", "image/png");
        MIME_TYPE_MAP.put("gif", "image/gif");
        MIME_TYPE_MAP.put("webp", "image/webp");
        MIME_TYPE_MAP.put("pdf", "application/pdf");
        MIME_TYPE_MAP.put("txt", "text/plain");
        MIME_TYPE_MAP.put("doc", "application/msword");
        MIME_TYPE_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPE_MAP.put("xls", "application/vnd.ms-excel");
        MIME_TYPE_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    public static String extractExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf(".");

        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    public static String getMimeType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "text/plain";
        }

        return MIME_TYPE_MAP.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }

    public static String createS3Key(String prefix, String uuid, String extension) {
        if (extension == null || extension.isEmpty()) {
            return prefix + uuid;
        }
        return prefix + uuid + "." + extension;
    }

}

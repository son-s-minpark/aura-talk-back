package com.sonsminpark.auratalkback.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatFileUploadRequestDto {

    @NotBlank(message = "S3 키는 필수 입력값입니다.")
    private String s3Key;

    @NotBlank(message = "원본 파일명은 필수 입력값입니다.")
    private String originalFileName;

    @NotNull(message = "파일 크기는 필수 입력값입니다.")
    private Long fileSize;
}
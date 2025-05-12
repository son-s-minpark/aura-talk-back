package com.sonsminpark.auratalkback.global.s3.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadResponseDto {
    private String url;
    private String s3Key;
}

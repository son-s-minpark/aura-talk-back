package com.sonsminpark.auratalkback.global.s3.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UploadCompletedRequestDto {
    private String s3Key;
}

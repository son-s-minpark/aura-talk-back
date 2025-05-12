package com.sonsminpark.auratalkback.global.s3;

import com.sonsminpark.auratalkback.global.s3.dto.request.PresignedUploadRequestDto;
import com.sonsminpark.auratalkback.global.s3.dto.response.PresignedUploadResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;


    public PresignedUploadResponseDto generatePresignedUploadUrl(UploadType type, PresignedUploadRequestDto requestDto) {
        String originalFileName = requestDto.getFileName();
        String ext = FileUtil.extractExtension(originalFileName);
        String contentType = FileUtil.getMimeType(ext);

        String uuid = UUID.randomUUID().toString();
        String key = FileUtil.createS3Key(type.getPrefix(), uuid, ext);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(3))
                .build();

        URL url = s3Presigner.presignPutObject(presignRequest).url();
        return new PresignedUploadResponseDto(url.toString(),key);
    }

    public void deleteFileFromS3(String url) {
        String key = extractKeyFromUrl(url);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    private String extractKeyFromUrl(String url) {
        String domain = "https://" + bucketName + ".s3.amazonaws.com/";
        return url.replace(domain, "");
    }
}

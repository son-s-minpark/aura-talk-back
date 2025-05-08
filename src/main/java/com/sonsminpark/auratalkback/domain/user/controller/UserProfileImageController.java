package com.sonsminpark.auratalkback.domain.user.controller;

import com.sonsminpark.auratalkback.domain.user.dto.response.ProfileImageResponseDto;
import com.sonsminpark.auratalkback.domain.user.service.UserProfileImageService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.s3.dto.request.PresignedUploadRequestDto;
import com.sonsminpark.auratalkback.global.s3.dto.response.PresignedUploadResponseDto;
import com.sonsminpark.auratalkback.global.s3.S3Service;
import com.sonsminpark.auratalkback.global.s3.UploadType;
import com.sonsminpark.auratalkback.global.s3.dto.request.UploadCompletedRequestDto;
import com.sonsminpark.auratalkback.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@Tag(name = "User Profile Image", description = "사용자 프로필 이미지 관련 API")
@RequestMapping("/api/users")
@RestController
public class UserProfileImageController {

    private final S3Service s3Service;
    private final UserProfileImageService userProfileImageService;

    @PostMapping("/me/profile-image/presigned-url")
    @Operation(
            summary = "프로필 이미지 업로드용 S3 PresignedUrl 생성",
            description = "사용자의 프로필 이미지 업로드를 위한 S3 PresignedUrl을 생성합니다. 클라이언트는 이 URL로 직접 S3에 파일을 업로드할 수 있습니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<PresignedUploadResponseDto>> getProfileUploadUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PresignedUploadRequestDto presignedUploadRequestDto) {
        PresignedUploadResponseDto presignedUploadResponseDto =
                s3Service.generatePresignedUploadUrl(UploadType.PROFILE, presignedUploadRequestDto);
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지 업로드 URL이 생성되었습니다.", presignedUploadResponseDto));
    }

    @PostMapping("/me/profile-image/upload-complete")
    @Operation(
            summary = "S3 프로필 이미지 업로드 완료 처리",
            description = "S3에 프로필 이미지 업로드가 완료된 후 호출합니다. 서버에 사용자 프로필 이미지 정보를 저장합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ProfileImageResponseDto>> saveProfileImageUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UploadCompletedRequestDto uploadCompleteRequestDto) {
        ProfileImageResponseDto profileImageResponseDto = userProfileImageService.updateProfileImage(
                userDetails.getUserId(), uploadCompleteRequestDto.getS3Key());
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 성공적으로 업데이트되었습니다.", profileImageResponseDto));
    }


    @GetMapping("/{userId}/profile-image")
    @Operation(
            summary = "프로필 이미지 조회",
            description = "특정 사용자의 프로필 이미지 정보(원본/썸네일 URL)를 조회합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<ApiResponse<ProfileImageResponseDto>> getProfileImage(@PathVariable Long userId) {
        ProfileImageResponseDto profileImageResponseDto = userProfileImageService.getProfileImage(userId);
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지 조회에 성공했습니다.", profileImageResponseDto));
    }

    @DeleteMapping("/me/profile-image")
    @Operation(
            summary = "프로필 이미지 삭제 및 기본 이미지로 변경",
            description = "사용자의 프로필 이미지를 삭제하고 기본 이미지로 변경합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<ApiResponse<ProfileImageResponseDto>> deleteProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProfileImageResponseDto profileImageResponseDto = userProfileImageService.deleteProfileImage(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 성공적으로 삭제되었습니다.", profileImageResponseDto));
    }

}

package com.sonsminpark.auratalkback.domain.interest.controller;

import com.sonsminpark.auratalkback.domain.interest.dto.InterestCategoryDto;
import com.sonsminpark.auratalkback.domain.interest.dto.response.InterestResponseDto;
import com.sonsminpark.auratalkback.domain.interest.service.InterestService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
@Tag(name = "Interest", description = "관심사 관련 API")
public class InterestController {

    private final InterestService interestService;

    @GetMapping
    @Operation(summary = "전체 관심사 조회", description = "카테고리별로 모든 관심사를 조회합니다.")
    public ResponseEntity<ApiResponse<List<InterestCategoryDto>>> getAllInterests() {
        List<InterestCategoryDto> interests = interestService.getAllInterestsByCategory();
        return ResponseEntity.ok(ApiResponse.success("관심사 조회에 성공했습니다.", interests));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "카테고리별 관심사 조회", description = "특정 카테고리의 관심사를 조회합니다.")
    public ResponseEntity<ApiResponse<List<InterestResponseDto>>> getInterestsByCategory(
            @PathVariable String category) {
        List<InterestResponseDto> interests = interestService.getInterestsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success("카테고리별 관심사 조회에 성공했습니다.", interests));
    }

    @GetMapping("/{interestName}/users")
    @Operation(
            summary = "관심사별 사용자 조회 (페이징)",
            description = "특정 관심사를 가진 사용자들을 페이징으로 조회합니다. 무한 스크롤을 지원합니다."
    )
    public ResponseEntity<ApiResponse<Page<com.sonsminpark.auratalkback.domain.user.dto.response.MyProfileResponseDto>>> getUsersByInterest(
            @Parameter(description = "관심사 이름", required = true)
            @PathVariable String interestName,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Page<com.sonsminpark.auratalkback.domain.user.dto.response.MyProfileResponseDto> response =
                interestService.getUsersByInterestNameWithPaging(interestName, page, size);
        return ResponseEntity.ok(ApiResponse.success("관심사별 사용자 조회에 성공했습니다.", response));
    }
}
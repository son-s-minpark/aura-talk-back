package com.sonsminpark.auratalkback.domain.interest.controller;

import com.sonsminpark.auratalkback.domain.interest.dto.InterestCategoryDto;
import com.sonsminpark.auratalkback.domain.interest.dto.response.InterestUsersResponseDto;
import com.sonsminpark.auratalkback.domain.interest.dto.response.InterestResponseDto;
import com.sonsminpark.auratalkback.domain.interest.service.InterestService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "관심사별 사용자 조회", description = "특정 관심사를 가진 사용자들을 조회합니다.")
    public ResponseEntity<ApiResponse<InterestUsersResponseDto>> getUsersByInterest(
            @PathVariable String interestName) {
        InterestUsersResponseDto response = interestService.getUsersByInterestName(interestName);
        return ResponseEntity.ok(ApiResponse.success("관심사별 사용자 조회에 성공했습니다.", response));
    }
}
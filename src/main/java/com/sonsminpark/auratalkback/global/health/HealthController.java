package com.sonsminpark.auratalkback.global.health;

import com.sonsminpark.auratalkback.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "서버 상태 확인 API")
public class HealthController {

    @GetMapping
    @Operation(summary = "서버 상태 확인", description = "애플리케이션 서버가 정상적으로 실행 중인지 확인합니다.")
    public ResponseEntity<ApiResponse<String>> checkHealth() {
        return ResponseEntity.ok(ApiResponse.success("서버 정상 작동", "OK"));
    }
}

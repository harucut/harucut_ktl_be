package com.harucut.media.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.media.dto.TranscodeRequest
import com.harucut.media.dto.TranscodeTaskStatusResponse
import com.harucut.media.dto.TranscodeTaskSubmitResponse
import com.harucut.media.service.TranscodingService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Transcoding", description = "동영상 변환(WebM → MP4) API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/auth/user/files")
class TranscodeController(
    private val transcodingService: TranscodingService
) {

    @Operation(
        summary = "동영상 변환 요청 (WebM → MP4)",
        description = "S3에 WebM 업로드가 완료된 후 호출하면 MediaConvert 작업을 시작하고 즉시 taskId/jobId를 반환합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "변환 작업 제출 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "500", description = "변환 작업 제출 실패")
    )
    @PostMapping("/transcode")
    fun startTranscoding(
        @RequestBody @Valid request: TranscodeRequest,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<TranscodeTaskSubmitResponse>> {
        val response = transcodingService.submitTranscodeTask(principal.publicId, request.filename)
        return ResponseEntity.accepted().body(Response.ok(response))
    }

    @Operation(summary = "동영상 변환 상태 조회", description = "taskId를 기준으로 MediaConvert 변환 상태를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "상태 조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "403", description = "다른 사용자의 작업 접근"),
        ApiResponse(responseCode = "404", description = "변환 작업을 찾을 수 없음")
    )
    @GetMapping("/transcode/status")
    fun getTranscodeStatus(
        @RequestParam("taskId") taskId: String,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<TranscodeTaskStatusResponse>> {
        val response = transcodingService.getTaskStatus(taskId, principal.publicId)
        return Response.ok(response).toResponseEntity()
    }
}
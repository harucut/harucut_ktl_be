package com.harucut.auth.controller

import com.harucut.auth.dto.NaverUnlinkRequest
import com.harucut.auth.oauth2.service.NaverOAuth2UnlinkService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "OAuth2 (Unlink)", description = "네이버 연동 해제 웹훅 수신 API (네이버 서버 호출, HMAC 서명으로 인증)")
@RestController
class NaverOAuth2UnlinkController(
    private val naverOAuth2UnlinkService: NaverOAuth2UnlinkService
) {

    @Operation(
        summary = "네이버 연동 해제 웹훅",
        description = "네이버가 연동 해제를 알리면 서명 검증 후 해당 사용자의 탈퇴 요청을 처리합니다. (네이버 서버 호출 전용)"
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "처리 성공"),
        ApiResponse(responseCode = "404", description = "해당 providerId 의 사용자 없음"),
        ApiResponse(responseCode = "500", description = "서명 검증 실패 등 처리 실패")
    )
    @PostMapping("/api/oauth2/unlink/naver")
    fun unlink(@RequestBody request: NaverUnlinkRequest): ResponseEntity<Void> {
        naverOAuth2UnlinkService.unlink(request)
        return ResponseEntity.noContent().build()
    }
}

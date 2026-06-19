package com.harucut.auth.controller

import com.harucut.auth.dto.NaverUnlinkRequest
import com.harucut.auth.oauth2.service.NaverOAuth2UnlinkService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class NaverOAuth2UnlinkController(
    private val naverOAuth2UnlinkService: NaverOAuth2UnlinkService
) {
    @PostMapping("/api/oauth2/unlink/naver")
    fun unlink(@RequestBody request: NaverUnlinkRequest): ResponseEntity<Void> {
        naverOAuth2UnlinkService.unlink(request)
        return ResponseEntity.noContent().build()
    }
}
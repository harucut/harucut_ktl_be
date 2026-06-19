package com.harucut.auth.controller

import com.harucut.auth.exit.service.UserExitService
import com.harucut.auth.jwt.util.CookieManager
import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.util.response.Response
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/harucut")
class UserExitController(
    private val userExitService: UserExitService,
    private val cookieManager: CookieManager
) {

    @DeleteMapping("/exit")
    fun exit(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<Unit>> {
        userExitService.requestExit(principal.id!!)

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieManager.createExpiredCookie("accessToken").toString())
            .header(HttpHeaders.SET_COOKIE, cookieManager.createExpiredCookie("refreshToken").toString())
            .body(Response.ok())
    }

    @PreAuthorize("hasRole('DELETED_REQUESTED')")
    @PostMapping("/reactivate")
    fun reactivate(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<Unit>> {
        userExitService.reActivate(principal.id!!)

        return Response.ok().toResponseEntity()
    }
}
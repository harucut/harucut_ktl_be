package com.harucut.user.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.user.dto.ChangeProfileImageRequest
import com.harucut.user.dto.SubscriptionUsageResponse
import com.harucut.user.dto.UserInfoResponse
import com.harucut.user.service.UserService
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "User", description = "사용자 정보 조회 및 수정 관련 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/auth/user")
class UserController(
    private val userService: UserService
) {

    // 내 정보 조회
    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    )
    @GetMapping("/info")
    fun getUserInfo(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<UserInfoResponse>> {
        val response = userService.getUserInfo(principal.id!!)
        return Response.ok(response).toResponseEntity()
    }

    // 구독 사용량 조회
    @Operation(
        summary = "구독 사용량 조회",
        description = "로그인한 사용자의 프레임 동시 보관 사용량을 조회합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    )
    @GetMapping("/subscription/usage")
    fun getSubscriptionUsage(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<SubscriptionUsageResponse>> {
        val response = userService.getSubscriptionUsage(principal.id!!)
        return Response.ok(response).toResponseEntity()
    }

    // 닉네임 변경
    @Operation(summary = "사용자 이름 변경", description = "로그인한 사용자의 닉네임(Username)을 변경합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "변경 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    )
    @PatchMapping("/change/username")
    fun changeUsername(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Parameter(description = "변경할 새로운 사용자 이름", required = true)
        @RequestParam
        @NotBlank(message = "사용자 이름은 비어 있을 수 없습니다.")
        @Size(max = 20, message = "사용자 이름은 20자 이하여야 합니다.")
        username: String
    ): ResponseEntity<Response<Unit>> {
        userService.changeUsername(principal.id!!, username)
        return Response.ok().toResponseEntity()
    }

    // 프로필 이미지 변경
    @Operation(summary = "프로필 이미지 변경", description = "S3에 업로드된 이미지 Key로 프로필 이미지를 변경합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "변경 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    )
    @PatchMapping("/change/profile-image")
    fun changeProfileImage(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestBody @Valid request: ChangeProfileImageRequest
    ): ResponseEntity<Response<Unit>> {
        userService.changeProfileImage(principal.id!!, request.s3Key)
        return Response.ok().toResponseEntity()
    }
}

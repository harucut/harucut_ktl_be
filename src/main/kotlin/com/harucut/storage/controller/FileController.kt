package com.harucut.storage.controller

import com.harucut.auth.security.CustomUserPrincipal
import com.harucut.storage.dto.PresignedUploadRequest
import com.harucut.storage.dto.PresignedUploadResponse
import com.harucut.storage.service.FileStorageService
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

@Tag(name = "File Storage", description = "파일 업로드 및 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/auth/user/files")
class FileController(
    private val fileStorageService: FileStorageService
) {

    @Operation(
        summary = "Presigned URL 생성",
        description = "S3에 파일을 업로드하기 위한 Presigned URL을 생성합니다. 업로드 타입(type)에 따라 저장 경로가 결정됩니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공"),
        ApiResponse(responseCode = "400", description = "검증 실패 / 지원하지 않는 업로드 타입"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "415", description = "지원하지 않는 MIME 타입 또는 확장자")
    )
    @PostMapping("/presigned-upload")
    fun createPresignedUpload(
        @RequestBody @Valid request: PresignedUploadRequest,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<Response<PresignedUploadResponse>> {
        val response = fileStorageService.generatePresignedUploadUrl(
            request.type,
            request.filename,
            request.contentType,
            principal.publicId,
            request.isTemp
        )

        return Response.ok(response).toResponseEntity()
    }

    @Operation(
        summary = "이미지 조회용 Presigned URL 생성",
        description = "저장된 key를 기반으로 이미지 조회용 Presigned URL을 생성합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요")
    )
    @GetMapping("/presigned-img")
    fun getPresignedImgUrl(
        @RequestParam("key") key: String
    ): ResponseEntity<Response<String>> {
        val response = fileStorageService.generatePresignedGetUrl(key)

        return Response.ok(response).toResponseEntity()
    }

    @Operation(
        summary = "S3 객체 삭제",
        description = "S3에 업로드되어 있는 파일을 key를 참고해 삭제합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "삭제 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요")
    )
    @DeleteMapping("/delete")
    fun deleteFile(
        @RequestParam("key") key: String
    ): ResponseEntity<Response<Unit>> {
        fileStorageService.delete(key)

        return Response.ok().toResponseEntity()
    }
}
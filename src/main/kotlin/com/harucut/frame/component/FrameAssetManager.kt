package com.harucut.frame.component

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.enums.BackgroundType
import com.harucut.frame.enums.ComponentType
import com.harucut.storage.service.FileStorageService
import com.harucut.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI

// 프레임 자산(S3) 라이프사이클 관리 — temp→영구 승격, presigned URL 해석, 정리
@Component
class FrameAssetManager(
    private val fileStorageService: FileStorageService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 컴포넌트 소스 중 사용자 temp 경로를 영구 경로로 승격하고 (원본 → 최종) key 매핑 반환
    fun moveTempFilesToPermanent(
        user: User,
        components: List<FrameCreateRequest.ComponentRequest>?
    ): Map<String, String> {
        if (components.isNullOrEmpty()) return emptyMap()

        val userTempPrefix = "$TEMP_ROOT/users/${user.publicId}/"
        val normalizedByOriginal = LinkedHashMap<String, String>()
        val tempKeys = mutableSetOf<String>()

        // 1) 각 소스를 정규화하고 사용자 temp 경로면 승격 대상으로 수집
        for (component in components) {
            val normalized = normalizeManagedKey(component.source) ?: continue
            if (normalized.isBlank()) continue
            normalizedByOriginal[component.source] = normalized
            if (normalized.startsWith(userTempPrefix)) tempKeys.add(normalized)
        }

        // 2) temp key를 영구 경로로 이동
        val keyMapping = HashMap<String, String>()
        for (tempKey in tempKeys) {
            try {
                val targetKey = toPermanentKey(tempKey)
                fileStorageService.moveFile(tempKey, targetKey)
                keyMapping[tempKey] = targetKey
            } catch (e: Exception) {
                log.error("Failed to move file from temp: {}", tempKey, e)
                throw BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "이미지 저장 중 오류가 발생했습니다.")
            }
        }

        // 3) 원본 소스 → 최종 key 매핑 완성 (이동 안 된 건 정규화 key 그대로)
        for ((original, normalized) in normalizedByOriginal) {
            val finalKey = keyMapping.getOrDefault(normalized, normalized)
            if (original != finalKey) keyMapping[original] = finalKey
        }
        return keyMapping
    }

    // 단일 temp key를 영구 경로로 승격 (사용자 temp가 아니면 정규화만)
    fun moveTempFileToPermanent(user: User, tempKey: String?): String? {
        val normalized = normalizeManagedKey(tempKey)
        if (normalized.isNullOrBlank()) return tempKey

        val userTempPrefix = "$TEMP_ROOT/users/${user.publicId}/"
        if (!normalized.startsWith(userTempPrefix)) return normalized

        return try {
            val targetKey = toPermanentKey(normalized)
            fileStorageService.moveFile(normalized, targetKey)
            targetKey
        } catch (e: Exception) {
            log.error("배경 파일 이동 실패: {}", normalized, e)
            throw BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "배경 파일 저장 중 오류 발생")
        }
    }

    // 컴포넌트 소스를 화면용 URL로 해석 (PHOTO이고 관리 S3 경로면 presigned GET)
    fun resolveSource(type: ComponentType, source: String?): String? {
        if (source == null) return null
        return if (type == ComponentType.PHOTO) presignIfManaged(source) else source
    }

    // 배경 소스를 화면용 URL로 해석 (IMAGE/VIDEO이고 관리 S3 경로면 presigned GET)
    fun resolveSource(type: BackgroundType, source: String?): String? {
        if (source == null) return null
        return if (type == BackgroundType.IMAGE || type == BackgroundType.VIDEO) presignIfManaged(source) else source
    }

    // S3 key 목록 일괄 삭제 (공백 key는 건너뜀)
    fun deleteFiles(keys: List<String?>) {
        keys.forEach { key -> if (!key.isNullOrBlank()) fileStorageService.delete(key) }
    }

    // 관리 S3 경로면 presigned GET URL로, 아니면 원본 그대로
    private fun presignIfManaged(source: String): String {
        val normalized = normalizeManagedKey(source)
        return if (isManagedS3Path(normalized)) fileStorageService.generatePresignedGetUrl(normalized!!) else source
    }

    // 입력(s3://, http(s)://, key)을 관리 가능한 순수 S3 key로 정규화
    private fun normalizeManagedKey(pathOrKey: String?): String? {
        if (pathOrKey.isNullOrBlank()) return pathOrKey
        val value = pathOrKey.trim()
        if (value.startsWith("s3://")) {
            val key = stripLeadingSlash(URI.create(value).path)
            return if (!key.isNullOrBlank()) key else value
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            val key = stripLeadingSlash(URI.create(value).path)
            return if (isManagedS3Path(key)) key else value
        }
        return stripLeadingSlash(value)
    }

    // temp/uploads 루트로 시작하는 관리 경로인지 판별
    private fun isManagedS3Path(key: String?): Boolean =
        !key.isNullOrBlank() && (key.startsWith("$TEMP_ROOT/") || key.startsWith("$UPLOAD_ROOT/"))

    // 앞쪽 '/' 제거
    private fun stripLeadingSlash(value: String?): String? =
        if (value.isNullOrBlank()) value else value.removePrefix("/")

    // temp 루트 → uploads 루트 치환
    private fun toPermanentKey(tempKey: String): String =
        tempKey.replaceFirst(Regex("^$TEMP_ROOT/"), "$UPLOAD_ROOT/")

    companion object {
        private const val TEMP_ROOT = "temp"
        private const val UPLOAD_ROOT = "uploads"
    }
}
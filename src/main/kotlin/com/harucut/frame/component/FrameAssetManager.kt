package com.harucut.frame.component

import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.enums.BackgroundType
import com.harucut.frame.enums.ComponentType
import com.harucut.storage.service.FileStorageService
import org.springframework.stereotype.Component
import java.net.URI

@Component
class FrameAssetManager(
    private val fileStorageService: FileStorageService
) {

    fun normalizeComponentKeys(
        components: List<FrameCreateRequest.ComponentRequest>?
    ): Map<String, String> {
        if (components.isNullOrEmpty()) return emptyMap()
        val mapping = LinkedHashMap<String, String>()
        for (component in components) {
            val normalized = normalizeManagedKey(component.source) ?: continue
            if (normalized.isBlank()) continue
            if (component.source != normalized) mapping[component.source] = normalized
        }
        return mapping
    }

    fun normalizeKey(key: String?): String? {
        return normalizeManagedKey(key)
    }

    fun resolveSource(type: ComponentType, source: String?): String? {
        if (source == null) return null
        return if (type == ComponentType.PHOTO) presignIfManaged(source) else source
    }

    fun resolveSource(type: BackgroundType, source: String?): String? {
        if (source == null) return null
        return if (type == BackgroundType.IMAGE) presignIfManaged(source) else source
    }

    fun deleteFiles(keys: List<String?>) {
        keys.forEach { key -> if (!key.isNullOrBlank()) fileStorageService.delete(key) }
    }

    private fun presignIfManaged(source: String): String {
        val normalized = normalizeManagedKey(source)
        return if (isManagedS3Path(normalized)) fileStorageService.generatePresignedGetUrl(normalized!!) else source
    }

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

    private fun isManagedS3Path(key: String?): Boolean =
        !key.isNullOrBlank() && key.startsWith("$UPLOAD_ROOT/")

    private fun stripLeadingSlash(value: String?): String? =
        if (value.isNullOrBlank()) value else value.removePrefix("/")

    companion object {
        private const val UPLOAD_ROOT = "uploads"
    }
}

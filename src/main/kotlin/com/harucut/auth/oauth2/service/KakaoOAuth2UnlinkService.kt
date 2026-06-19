package com.harucut.auth.oauth2.service

import com.harucut.auth.exception.AuthErrorCode
import com.harucut.auth.oauth2.property.KakaoAuthProperties
import com.harucut.exception.BusinessException
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Service
class KakaoOAuth2UnlinkService(
    private val restClient: RestClient,
    private val kakaoAuthProperties: KakaoAuthProperties
) : OAuth2UnlinkService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(provider: Provider): Boolean =
        provider == Provider.KAKAO

    override fun unlink(user: User) {
        if (user.provider != Provider.KAKAO) return

        val targetId = user.providerId
            ?: throw BusinessException(AuthErrorCode.OAUTH2_UNLINK_FAILED)

        val body = LinkedMultiValueMap<String, String>().apply {
            add("target_id_type", kakaoAuthProperties.unlinkTargetIdType)
            add("target_id", targetId)
        }

        try {
            restClient.post()
                .uri(kakaoAuthProperties.unlinkUrl)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK ${kakaoAuthProperties.adminKey}")
                .header(HttpHeaders.CONTENT_TYPE, kakaoAuthProperties.unlinkContentType)
                .body(body)
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            log.warn("[Kakao unlink 실패] targetId={}", targetId, e)
            throw BusinessException(AuthErrorCode.OAUTH2_UNLINK_FAILED)
        }
    }
}
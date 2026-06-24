package com.harucut.storage.util

import com.harucut.exception.BusinessException
import com.harucut.exception.GlobalErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class S3KeyNormalizerTest {

    @Test
    @DisplayName("순수 key는 그대로 반환한다")
    fun plainKey() {
        assertThat(normalizeToS3Key("uploads/users/pub/profile/a.png"))
            .isEqualTo("uploads/users/pub/profile/a.png")
    }

    @Test
    @DisplayName("앞쪽 슬래시를 제거한다")
    fun stripsLeadingSlash() {
        assertThat(normalizeToS3Key("/uploads/a.png")).isEqualTo("uploads/a.png")
    }

    @Test
    @DisplayName("s3:// URL에서 순수 key를 추출한다")
    fun s3Url() {
        assertThat(normalizeToS3Key("s3://bucket/uploads/a.png")).isEqualTo("uploads/a.png")
    }

    @Test
    @DisplayName("https URL에서 순수 key를 추출한다")
    fun httpsUrl() {
        assertThat(normalizeToS3Key("https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/a.png"))
            .isEqualTo("uploads/a.png")
    }

    @Test
    @DisplayName("비어있으면 INVALID_INPUT_VALUE 예외를 던진다")
    fun blank() {
        assertThatThrownBy { normalizeToS3Key("  ") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE)
    }

    @Test
    @DisplayName("key를 추출할 수 없는 URL이면 INVALID_INPUT_VALUE 예외를 던진다")
    fun noKeyInUrl() {
        assertThatThrownBy { normalizeToS3Key("https://bucket.s3.amazonaws.com/") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE)
    }
}

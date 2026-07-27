package com.harucut.frame.entity

import com.harucut.frame.attributes.ColorBackgroundAttributes
import com.harucut.frame.enums.FrameType
import com.harucut.user.entity.User
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// 불변식 검증: isSystem=true ⇔ user=null
class FrameTest {

    private fun userMock(): User = mockk(relaxed = true)

    @Nested
    inner class Invariant {

        @Test
        @DisplayName("사용자 프레임(isSystem=false)은 user가 있어야 한다")
        fun userFrameRequiresUser() {
            val frame = Frame(
                title = "t",
                description = "d",
                previewKey = "p",
                frameType = FrameType.CLASSIC,
                background = ColorBackgroundAttributes("#fff"),
                user = userMock()
            )

            assertThat(frame.isSystem).isFalse()
            assertThat(frame.user).isNotNull()
        }

        @Test
        @DisplayName("user=null 인데 isSystem=false 면 생성 시 예외를 던진다")
        fun userNullWithoutSystemFlagFails() {
            assertThatThrownBy {
                Frame(
                    title = "t",
                    description = "d",
                    previewKey = "p",
                    frameType = FrameType.CLASSIC,
                    background = ColorBackgroundAttributes("#fff"),
                    user = null
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("user가 있는데 isSystem=true 면 생성 시 예외를 던진다")
        fun userPresentWithSystemFlagFails() {
            assertThatThrownBy {
                Frame(
                    title = "t",
                    description = "d",
                    previewKey = "p",
                    frameType = FrameType.CLASSIC,
                    background = ColorBackgroundAttributes("#fff"),
                    user = userMock(),
                    isSystem = true
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("Frame.system 팩토리는 user=null, isSystem=true 인 시스템 프레임을 만든다")
        fun systemFactoryCreatesOwnerlessFrame() {
            val frame = Frame.system(
                title = "system title",
                description = "d",
                previewKey = "p",
                frameType = FrameType.CLASSIC,
                background = ColorBackgroundAttributes("#fff")
            )

            assertThat(frame.isSystem).isTrue()
            assertThat(frame.user).isNull()
        }
    }
}

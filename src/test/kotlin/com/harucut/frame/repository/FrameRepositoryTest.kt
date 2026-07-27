package com.harucut.frame.repository

import com.harucut.frame.attributes.ColorBackgroundAttributes
import com.harucut.frame.entity.Frame
import com.harucut.frame.enums.FrameType
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

// frame.user_id nullable + is_system 매핑이 정상 동작하는지 H2로 검증.
// (V6__alter_frame_system.sql은 prod validate용이며, @DataJpaTest는 Flyway 없이 ddl-auto로 H2 스키마를 생성한다.)
@DataJpaTest
class FrameRepositoryTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var frameRepository: FrameRepository

    private fun user(email: String): User = userRepository.save(
        User(
            provider = Provider.HARUCUT,
            userRole = UserRole.ROLE_USER,
            email = email,
            username = "tester",
            profileImageUrl = "resources/defaults/userDefaultImage.png",
            userStatus = UserStatus.ACTIVE
        )
    )

    @Test
    @DisplayName("user_id가 null인 시스템 프레임과 user_id가 있는 사용자 프레임을 모두 저장·조회할 수 있다")
    fun savesUserAndSystemFrames() {
        val owner = user("frame-owner@harucut.com")
        val userFrame = frameRepository.save(
            Frame(
                title = "내 프레임",
                description = "설명",
                previewKey = "preview.png",
                frameType = FrameType.CLASSIC,
                background = ColorBackgroundAttributes("#ffffff"),
                user = owner
            )
        )
        val systemFrame = frameRepository.save(
            Frame.system(
                title = "기본 프레임",
                description = "설명",
                previewKey = "system-preview.png",
                frameType = FrameType.CLASSIC,
                background = ColorBackgroundAttributes("#ffffff")
            )
        )
        frameRepository.flush()

        assertThat(userFrame.isSystem).isFalse()
        assertThat(systemFrame.user).isNull()
        assertThat(systemFrame.isSystem).isTrue()

        val systemFrames = frameRepository.findAllByIsSystemTrueOrderByCreatedAtDesc()
        assertThat(systemFrames).extracting("id").containsExactly(systemFrame.id)

        val ownerFrames = frameRepository.findAllByUserOrderByCreatedAtDesc(owner)
        assertThat(ownerFrames).extracting("id").containsExactly(userFrame.id)
    }
}

package com.harucut.frame.repository

import com.harucut.frame.attributes.ColorBackgroundAttributes
import com.harucut.frame.attributes.ImageBackgroundAttributes
import com.harucut.frame.entity.Frame
import com.harucut.frame.entity.FrameComponent
import com.harucut.frame.enums.ComponentType
import com.harucut.frame.enums.FrameType
import com.harucut.user.entity.User
import com.harucut.user.enums.Provider
import com.harucut.user.enums.UserRole
import com.harucut.user.enums.UserStatus
import com.harucut.user.repository.UserRepository
import jakarta.persistence.EntityManager
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

    @Autowired
    lateinit var em: EntityManager

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

    // JSON 컬럼(columnDefinition="json") + AttributeConverter 조합이 Hibernate 6에서 이중 인코딩되던
    // 회귀 버그 검증 (GEN-091). save → flush → clear 로 영속성 컨텍스트를 비우고 실제 재조회 경로를 태운다.
    @Test
    @DisplayName("[회귀] COLOR 배경이 저장 후 재조회에서 원본 그대로 복원된다")
    fun colorBackgroundSurvivesReload() {
        val saved = frameRepository.save(
            Frame.system(
                title = "제목", description = "설명", previewKey = "preview.png",
                frameType = FrameType.CLASSIC, background = ColorBackgroundAttributes("#ffffff")
            )
        )
        em.flush()
        em.clear()

        val reloaded = frameRepository.findById(saved.id!!).get()

        assertThat(reloaded.background).isEqualTo(ColorBackgroundAttributes("#ffffff"))
    }

    @Test
    @DisplayName("[회귀] IMAGE 배경이 저장 후 재조회에서 원본 그대로 복원된다")
    fun imageBackgroundSurvivesReload() {
        val saved = frameRepository.save(
            Frame.system(
                title = "제목", description = "설명", previewKey = "preview.png",
                frameType = FrameType.CLASSIC, background = ImageBackgroundAttributes("uploads/bg.png", 0.5)
            )
        )
        em.flush()
        em.clear()

        val reloaded = frameRepository.findById(saved.id!!).get()

        assertThat(reloaded.background).isEqualTo(ImageBackgroundAttributes("uploads/bg.png", 0.5))
    }

    @Test
    @DisplayName("[회귀] 컴포넌트의 styleJson이 저장 후 재조회에서 원본 그대로 복원된다")
    fun styleJsonSurvivesReload() {
        val frame = Frame.system(
            title = "제목", description = "설명", previewKey = "preview.png",
            frameType = FrameType.CLASSIC, background = ColorBackgroundAttributes("#ffffff")
        )
        frame.addComponent(
            FrameComponent(
                source = "uploads/photo.png", type = ComponentType.PHOTO,
                x = 0.0, y = 0.0, width = null, height = null, scale = null,
                rotation = 0.0, zIndex = 0, styleJson = """{"color":"red","fontSize":14}"""
            )
        )
        val saved = frameRepository.save(frame)
        em.flush()
        em.clear()

        val reloaded = frameRepository.findById(saved.id!!).get()

        assertThat(reloaded.components).hasSize(1)
        assertThat(reloaded.components[0].styleJson).isEqualTo("""{"color":"red","fontSize":14}""")
    }
}

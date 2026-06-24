package com.harucut.frame.handler

import com.harucut.frame.repository.FrameRepository
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FrameUserDeletionHandlerTest {

    private val frameRepository = mockk<FrameRepository>(relaxed = true)
    private val handler = FrameUserDeletionHandler(frameRepository)

    @Test
    @DisplayName("FK 제약을 지켜 컴포넌트 → 프레임 순서로 삭제한다")
    fun deletesInOrder() {
        handler.handleUserDeletion(1L)

        verifyOrder {
            frameRepository.deleteComponentsByUserId(1L)
            frameRepository.deleteFramesByUserId(1L)
        }
    }
}

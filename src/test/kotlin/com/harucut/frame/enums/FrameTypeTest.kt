package com.harucut.frame.enums

import com.harucut.frame.layout.FrameLayout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

// FrameType별 캔버스 크기는 프론트 apps/web/constants/frameLayouts.ts 실측값과 1:1로 일치해야 한다.
// 값이 어긋나면 기존에 저장된 프레임의 장식 위치가 조용히 틀어지므로, 값 자체를 고정해 드리프트를 감지한다.
class FrameTypeTest {

    @Test
    @DisplayName("CLASSIC은 2000x6000 캔버스를 가진다")
    fun classic() {
        assertThat(FrameType.CLASSIC.layout).isEqualTo(FrameLayout(canvasWidth = 2000, canvasHeight = 6000))
    }

    @Test
    @DisplayName("WIDE는 6000x4000 캔버스를 가진다")
    fun wide() {
        assertThat(FrameType.WIDE.layout).isEqualTo(FrameLayout(canvasWidth = 6000, canvasHeight = 4000))
    }

    @Test
    @DisplayName("GRID는 4000x6000 캔버스를 가진다")
    fun grid() {
        assertThat(FrameType.GRID.layout).isEqualTo(FrameLayout(canvasWidth = 4000, canvasHeight = 6000))
    }

    @Test
    @DisplayName("POLAROID는 4000x6000 캔버스를 가진다")
    fun polaroid() {
        assertThat(FrameType.POLAROID.layout).isEqualTo(FrameLayout(canvasWidth = 4000, canvasHeight = 6000))
    }
}

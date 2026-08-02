package com.harucut.frame.layout

// frameType별 캔버스 크기(px). 프론트 apps/web/constants/frameLayouts.ts 실측값과 1:1로 일치해야 한다.
// 이 값이 프론트 상수와 어긋나면 기존에 저장된 프레임의 장식(컴포넌트) 위치가 조용히 틀어진다.
data class FrameLayout(
    val canvasWidth: Int,
    val canvasHeight: Int
)

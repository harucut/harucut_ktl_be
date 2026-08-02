package com.harucut.frame.enums

import com.harucut.frame.layout.FrameLayout

// 프레임 레이아웃 종류
// layout(캔버스 크기)은 프론트 apps/web/constants/frameLayouts.ts 실측값과 반드시 일치해야 한다.
enum class FrameType(val title: String, val description: String, val layout: FrameLayout) {
    CLASSIC("클래식", "2x2 기본 인생네컷 스타일", FrameLayout(canvasWidth = 2000, canvasHeight = 6000)),
    WIDE("와이드", "가로로 넓은 4컷 스타일", FrameLayout(canvasWidth = 6000, canvasHeight = 4000)),
    GRID("그리드", "정방형 격자 스타일", FrameLayout(canvasWidth = 4000, canvasHeight = 6000)),
    POLAROID("폴라로이드", "하단 여백이 있는 폴라로이드 스타일", FrameLayout(canvasWidth = 4000, canvasHeight = 6000))
}
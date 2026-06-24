package com.harucut.frame.enums

// 프레임 배경 종류
enum class BackgroundType(val title: String, val description: String) {
    COLOR("단색", "단일 색상 코드(HEX) 사용"),
    IMAGE("이미지", "S3 이미지 URL 사용 (패턴 등)"),
    GRADIENT("그라디언트", "CSS 그라디언트 문법 사용"),
    VIDEO("비디오", "S3 영상 URL 사용 (움직이는 배경)")
}

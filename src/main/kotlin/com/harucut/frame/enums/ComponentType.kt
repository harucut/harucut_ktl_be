package com.harucut.frame.enums

// 프레임 위에 올라가는 요소 종류
enum class ComponentType(val title: String, val description: String) {
    PHOTO("사진", "사용자가 업로드한 사진"),
    STICKER("스티커", "제공되는 데코레이션 스티커"),
    TEXT("텍스트", "사용자 입력 텍스트")
}
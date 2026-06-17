package com.harucut.util.mail.service

interface EmailVerificationService {

    // 1. 인증 코드 발송 (코드 생성 -> 레디스 저장 -> MailService로 발송)
    fun sendVerificationCode(email: String)

    // 2. 사용자가 입력한 코드 검증 (맞으면 레디스에 '인증완료' 마킹)
    fun verifyCode(email: String, code: String): Boolean

    // 3. 회원가입 시 최종 검증 확인 후 소모 (완료 마킹 확인 후 삭제)
    fun consumeVerified(email: String): Boolean
}
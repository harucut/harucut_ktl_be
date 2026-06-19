package com.harucut.auth.exit.batch.service

import com.harucut.auth.exit.service.UserExitService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class UserExitBatchService(
    private val userExitService: UserExitService
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun exitInNewTransaction(userId: Long) {
        userExitService.exit(userId)
    }
}
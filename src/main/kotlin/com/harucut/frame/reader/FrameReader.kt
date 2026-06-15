package com.harucut.frame.reader

import com.harucut.exception.BusinessException
import com.harucut.frame.entity.Frame
import com.harucut.frame.exception.FrameErrorCode
import com.harucut.frame.repository.FrameRepository
import com.harucut.user.entity.User
import org.springframework.stereotype.Component

@Component
class FrameReader(
    private val frameRepository: FrameRepository
) {
    fun getById(frameId: Long): Frame =
        frameRepository.findById(frameId).orElseThrow { BusinessException(FrameErrorCode.FRAME_NOT_FOUND) }

    fun getByIdAndUser(frameId: Long, user: User): Frame =
        frameRepository.findByIdAndUser(frameId, user)
            ?: throw BusinessException(FrameErrorCode.FRAME_NOT_FOUND)

    fun getAllByUser(user: User): List<Frame> =
        frameRepository.findAllByUser(user)

    fun countByUser(userId: Long): Long =
        frameRepository.countByUserId(userId)
}
package com.harucut.media.reader

import com.harucut.exception.BusinessException
import com.harucut.media.entity.UserMedia
import com.harucut.media.exception.MediaErrorCode
import com.harucut.media.repository.UserMediaRepository
import com.harucut.user.entity.User
import org.springframework.stereotype.Component

@Component
class UserMediaReader(
    private val userMediaRepository: UserMediaRepository
) {
    fun getById(mediaId: Long): UserMedia =
        userMediaRepository.findById(mediaId).orElseThrow { BusinessException(MediaErrorCode.MEDIA_NOT_FOUND) }

    fun getByIdAndUser(mediaId: Long, user: User): UserMedia =
        userMediaRepository.findByIdAndUser(mediaId, user)
            ?: throw BusinessException(MediaErrorCode.MEDIA_NOT_FOUND)

    fun getAllByUser(user: User): List<UserMedia> =
        userMediaRepository.findAllByUserOrderByCreatedAtDesc(user)

    fun findByS3Key(s3Key: String): UserMedia? =
        userMediaRepository.findByS3Key(s3Key)
}

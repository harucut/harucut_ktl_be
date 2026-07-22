package com.harucut.notice.repository

import com.harucut.notice.entity.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository : JpaRepository<Notice, Long> {

    fun findByPublishedTrueOrderByPinnedDescPublishedAtDesc(pageable: Pageable): Page<Notice>

    fun findByPublicId(publicId: String): Notice?
}

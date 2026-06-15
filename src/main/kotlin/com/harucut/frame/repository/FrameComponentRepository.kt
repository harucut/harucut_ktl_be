package com.harucut.frame.repository

import com.harucut.frame.entity.FrameComponent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FrameComponentRepository : JpaRepository<FrameComponent, Long>
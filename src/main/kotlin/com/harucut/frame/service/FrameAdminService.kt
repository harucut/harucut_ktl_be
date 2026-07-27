package com.harucut.frame.service

import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.dto.FrameResponse

interface FrameAdminService {
    fun createSystemFrame(request: FrameCreateRequest)
    fun updateSystemFrame(frameId: Long, request: FrameCreateRequest)
    fun deleteSystemFrame(frameId: Long)
    fun listSystemFrames(): List<FrameResponse>
}

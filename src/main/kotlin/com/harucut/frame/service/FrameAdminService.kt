package com.harucut.frame.service

import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.dto.FrameResponse

interface FrameAdminService {
    fun createSystemFrame(request: FrameCreateRequest): FrameResponse
    fun updateSystemFrame(frameId: Long, request: FrameCreateRequest): FrameResponse
    fun deleteSystemFrame(frameId: Long)
    fun listSystemFrames(): List<FrameResponse>
}

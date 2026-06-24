package com.harucut.frame.service

import com.harucut.frame.dto.FrameCreateRequest
import com.harucut.frame.dto.FrameResponse

interface FrameService {
    fun createFrame(userId: Long, request: FrameCreateRequest)
    fun getFrame(frameId: Long, userId: Long): FrameResponse
    fun getMyFrames(userId: Long): List<FrameResponse>
    fun deleteFrame(userId: Long, frameId: Long)
    fun updateFrame(userId: Long, frameId: Long, request: FrameCreateRequest)
}
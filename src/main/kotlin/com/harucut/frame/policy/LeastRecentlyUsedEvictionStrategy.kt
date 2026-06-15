package com.harucut.frame.policy

import com.harucut.frame.entity.Frame
import org.springframework.stereotype.Component

@Component
class LeastRecentlyUsedEvictionStrategy : FrameEvictionStrategy {

    override fun selectEvictionTargets(frames: List<Frame>, count: Int): List<Frame> =
        frames.sortedBy { it.lastAccessedAt ?: it.createdAt }.take(count)
}
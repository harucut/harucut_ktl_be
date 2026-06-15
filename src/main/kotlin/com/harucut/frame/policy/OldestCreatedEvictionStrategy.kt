package com.harucut.frame.policy

import com.harucut.frame.entity.Frame
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class OldestCreatedEvictionStrategy : FrameEvictionStrategy {

    override fun selectEvictionTargets(frames: List<Frame>, count: Int): List<Frame> =
        frames.sortedBy { it.createdAt }.take(count)
}
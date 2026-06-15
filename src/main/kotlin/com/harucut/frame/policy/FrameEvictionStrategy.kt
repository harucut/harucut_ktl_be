package com.harucut.frame.policy

import com.harucut.frame.entity.Frame

interface FrameEvictionStrategy {

    fun selectEvictionTargets(frames: List<Frame>, count: Int): List<Frame>
}
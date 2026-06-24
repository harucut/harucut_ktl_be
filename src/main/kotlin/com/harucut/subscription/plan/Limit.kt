package com.harucut.subscription.plan

sealed interface Limit {
    fun allows(currentCount: Int): Boolean
    val isUnlimited: Boolean

    /** API 노출용 한도값. 무제한이면 -1 */
    fun maxOrUnlimited(): Int

    /** 사용량 대비 남은 가능 수. 무제한이면 -1, 음수는 0으로 보정 */
    fun remainingFrom(used: Int): Int

    data object Unlimited : Limit {
        override fun allows(currentCount: Int) = true
        override val isUnlimited = true
        override fun maxOrUnlimited() = UNLIMITED
        override fun remainingFrom(used: Int) = UNLIMITED
    }

    data class Limited(val max: Int) : Limit {
        init {
            require(max >= 0) { "max는 0 이상이어야 합니다: $max" }
        }

        override fun allows(currentCount: Int) = currentCount < max
        override val isUnlimited = false
        override fun maxOrUnlimited() = max
        override fun remainingFrom(used: Int) = maxOf(max - used, 0)
    }

    companion object {
        /** 무제한을 나타내는 API 노출값 */
        const val UNLIMITED = -1
    }
}

package com.harucut.subscription.plan

sealed interface Limit {
    fun allows(currentCount: Int): Boolean
    val isUnlimited: Boolean

    data object Unlimited : Limit {
        override fun allows(currentCount: Int) = true
        override val isUnlimited = true
    }

    data class Limited(val max: Int) : Limit {
        init {
            require(max >= 0) { "max는 0 이상이어야 합니다: $max" }
        }

        override fun allows(currentCount: Int) = currentCount < max
        override val isUnlimited = false
    }
}
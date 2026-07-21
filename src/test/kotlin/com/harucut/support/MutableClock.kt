package com.harucut.support

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

/**
 * 테스트에서 시간을 앞으로 흘려보내기 위한 [Clock]. `@Primary` 빈으로 등록해
 * 애플리케이션의 [Clock] 의존성을 이 인스턴스로 오버라이드한다(운영 코드는 변경하지 않음).
 */
class MutableClock(initial: Clock) : Clock() {

    private val delegate = AtomicReference(initial)

    override fun getZone(): ZoneId = delegate.get().zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(delegate.get().withZone(zone))

    override fun instant(): Instant = delegate.get().instant()

    fun advanceBy(duration: Duration) {
        delegate.set(Clock.offset(delegate.get(), duration))
    }
}

package com.harucut.admin.stats.service

import com.harucut.admin.stats.enums.Granularity
import com.harucut.admin.stats.exception.AdminErrorCode
import com.harucut.exception.BusinessException
import com.harucut.payment.entity.Payment
import com.harucut.payment.entity.PaymentOrder
import com.harucut.payment.enums.OrderType
import com.harucut.payment.enums.PaymentStatus
import com.harucut.payment.repository.PaymentRepository
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AdminStatsServiceTest {

    private val paymentRepository = mockk<PaymentRepository>()
    private val userSubscriptionRepository = mockk<UserSubscriptionRepository>()
    private val clock = Clock.fixed(
        LocalDate.of(2026, 7, 22).atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
    )

    private val service = AdminStatsServiceImpl(paymentRepository, userSubscriptionRepository, clock)

    private fun payment(amount: Int, approvedAt: LocalDateTime, orderType: OrderType): Payment {
        val order = mockk<PaymentOrder>(relaxed = true)
        every { order.orderType } returns orderType
        return mockk<Payment>(relaxed = true).also {
            every { it.amount } returns amount
            every { it.approvedAt } returns approvedAt
            every { it.order } returns order
        }
    }

    @Nested
    @DisplayName("getRevenueSummary")
    inner class GetRevenueSummary {

        @Test
        @DisplayName("승인 결제만 조회하며 신규/갱신 매출을 분해해 합산한다")
        fun sumsInitialAndRenewal() {
            val from = LocalDate.of(2026, 7, 1)
            val to = LocalDate.of(2026, 7, 22)
            val payments = listOf(
                payment(3900, LocalDateTime.of(2026, 7, 10, 12, 0), OrderType.INITIAL),
                payment(3900, LocalDateTime.of(2026, 7, 11, 12, 0), OrderType.RENEWAL),
                payment(9900, LocalDateTime.of(2026, 7, 12, 12, 0), OrderType.RENEWAL)
            )
            every {
                paymentRepository.findApprovedInRange(PaymentStatus.APPROVED, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
            } returns payments

            val result = service.getRevenueSummary(from, to, Granularity.DAY)

            assertThat(result.totalAmount).isEqualTo(17700)
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.initialAmount).isEqualTo(3900)
            assertThat(result.renewalAmount).isEqualTo(13800)
        }

        @Test
        @DisplayName("granularity=DAY면 서버에서 날짜별로 버킷팅한다")
        fun bucketsByDay() {
            val from = LocalDate.of(2026, 7, 1)
            val to = LocalDate.of(2026, 7, 22)
            val payments = listOf(
                payment(1000, LocalDateTime.of(2026, 7, 10, 9, 0), OrderType.INITIAL),
                payment(2000, LocalDateTime.of(2026, 7, 10, 15, 0), OrderType.RENEWAL),
                payment(3000, LocalDateTime.of(2026, 7, 11, 9, 0), OrderType.RENEWAL)
            )
            every {
                paymentRepository.findApprovedInRange(PaymentStatus.APPROVED, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
            } returns payments

            val result = service.getRevenueSummary(from, to, Granularity.DAY)

            assertThat(result.series).hasSize(2)
            assertThat(result.series[0].bucket).isEqualTo("2026-07-10")
            assertThat(result.series[0].amount).isEqualTo(3000)
            assertThat(result.series[0].count).isEqualTo(2)
            assertThat(result.series[1].bucket).isEqualTo("2026-07-11")
            assertThat(result.series[1].amount).isEqualTo(3000)
            assertThat(result.series[1].count).isEqualTo(1)
        }

        @Test
        @DisplayName("granularity=MONTH면 서버에서 월별로 버킷팅한다")
        fun bucketsByMonth() {
            val from = LocalDate.of(2026, 6, 1)
            val to = LocalDate.of(2026, 7, 22)
            val payments = listOf(
                payment(1000, LocalDateTime.of(2026, 6, 15, 9, 0), OrderType.INITIAL),
                payment(2000, LocalDateTime.of(2026, 7, 10, 9, 0), OrderType.RENEWAL)
            )
            every {
                paymentRepository.findApprovedInRange(PaymentStatus.APPROVED, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
            } returns payments

            val result = service.getRevenueSummary(from, to, Granularity.MONTH)

            assertThat(result.series).extracting("bucket").containsExactly("2026-06", "2026-07")
        }

        @Test
        @DisplayName("from/to를 생략하면 이번 달 1일부터 오늘까지를 기본 범위로 사용한다")
        fun defaultsToCurrentMonth() {
            every { paymentRepository.findApprovedInRange(any(), any(), any()) } returns emptyList()

            val result = service.getRevenueSummary(null, null, Granularity.DAY)

            assertThat(result.from).isEqualTo(LocalDate.of(2026, 7, 1))
            assertThat(result.to).isEqualTo(LocalDate.of(2026, 7, 22))
        }

        @Test
        @DisplayName("from이 to보다 늦으면 INVALID_DATE_RANGE(ADMIN-001) 예외를 던진다")
        fun fromAfterTo() {
            val from = LocalDate.of(2026, 7, 22)
            val to = LocalDate.of(2026, 7, 1)

            assertThatThrownBy { service.getRevenueSummary(from, to, Granularity.DAY) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AdminErrorCode.INVALID_DATE_RANGE)
        }

        @Test
        @DisplayName("조회 범위가 정확히 366일이면 허용한다")
        fun rangeExactlyMax() {
            val from = LocalDate.of(2026, 1, 1)
            val to = from.plusDays(365) // from 포함 366일째
            every {
                paymentRepository.findApprovedInRange(PaymentStatus.APPROVED, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
            } returns emptyList()

            val result = service.getRevenueSummary(from, to, Granularity.DAY)

            assertThat(result.from).isEqualTo(from)
            assertThat(result.to).isEqualTo(to)
        }

        @Test
        @DisplayName("조회 범위가 367일(366일 초과)이면 INVALID_DATE_RANGE(ADMIN-001) 예외를 던진다")
        fun rangeTooWide() {
            val from = LocalDate.of(2026, 1, 1)
            val to = from.plusDays(366) // from 포함 367일째

            assertThatThrownBy { service.getRevenueSummary(from, to, Granularity.DAY) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(AdminErrorCode.INVALID_DATE_RANGE)
        }
    }

    @Nested
    @DisplayName("getSubscriptionSnapshot")
    inner class GetSubscriptionSnapshot {

        @Test
        @DisplayName("tier별 '현재 유료 접근 중'(ACTIVE·CANCELED 만료 전) 구독 수, status별 구독 수, 자동갱신 on/off 수를 집계한다")
        fun aggregatesSnapshot() {
            val now = LocalDateTime.of(2026, 7, 22, 0, 0)
            val paidAccessStatuses = listOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED)
            every {
                userSubscriptionRepository.countByStatusInAndPlanTierAndCurrentPeriodEndAfter(paidAccessStatuses, PlanTier.PLUS, now)
            } returns 5
            every {
                userSubscriptionRepository.countByStatusInAndPlanTierAndCurrentPeriodEndAfter(paidAccessStatuses, PlanTier.PRO, now)
            } returns 2
            every { userSubscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE) } returns 7
            every { userSubscriptionRepository.countByStatus(SubscriptionStatus.CANCELED) } returns 1
            every { userSubscriptionRepository.countByStatus(SubscriptionStatus.PAST_DUE) } returns 1
            every { userSubscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED) } returns 3
            every { userSubscriptionRepository.countByAutoRenew(true) } returns 6
            every { userSubscriptionRepository.countByAutoRenew(false) } returns 6

            val result = service.getSubscriptionSnapshot()

            assertThat(result.byTier).containsEntry(PlanTier.PLUS, 5L).containsEntry(PlanTier.PRO, 2L)
            assertThat(result.byTier).doesNotContainKey(PlanTier.BASIC)
            assertThat(result.byStatus).containsEntry(SubscriptionStatus.ACTIVE, 7L)
            assertThat(result.autoRenewOn).isEqualTo(6)
            assertThat(result.autoRenewOff).isEqualTo(6)
        }
    }
}

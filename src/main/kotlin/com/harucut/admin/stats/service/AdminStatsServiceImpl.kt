package com.harucut.admin.stats.service

import com.harucut.admin.stats.dto.RevenuePoint
import com.harucut.admin.stats.dto.RevenueSummaryResponse
import com.harucut.admin.stats.dto.SubscriptionSnapshotResponse
import com.harucut.admin.stats.enums.Granularity
import com.harucut.admin.stats.exception.AdminErrorCode
import com.harucut.exception.BusinessException
import com.harucut.payment.entity.Payment
import com.harucut.payment.enums.OrderType
import com.harucut.payment.enums.PaymentStatus
import com.harucut.payment.repository.PaymentRepository
import com.harucut.subscription.enums.SubscriptionStatus
import com.harucut.subscription.plan.PlanTier
import com.harucut.subscription.repository.UserSubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
@Transactional(readOnly = true)
class AdminStatsServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val clock: Clock
) : AdminStatsService {

    override fun getRevenueSummary(from: LocalDate?, to: LocalDate?, granularity: Granularity): RevenueSummaryResponse {
        val today = LocalDate.now(clock)
        val rangeFrom = from ?: today.withDayOfMonth(1)
        val rangeTo = to ?: today
        validateRange(rangeFrom, rangeTo)

        val payments = paymentRepository.findApprovedInRange(
            PaymentStatus.APPROVED,
            rangeFrom.atStartOfDay(),
            rangeTo.plusDays(1).atStartOfDay()
        )

        return RevenueSummaryResponse(
            from = rangeFrom,
            to = rangeTo,
            granularity = granularity,
            totalAmount = payments.sumAmount(),
            totalCount = payments.size.toLong(),
            initialAmount = payments.filter { it.order.orderType == OrderType.INITIAL }.sumAmount(),
            renewalAmount = payments.filter { it.order.orderType == OrderType.RENEWAL }.sumAmount(),
            series = buildSeries(payments, granularity)
        )
    }

    override fun getSubscriptionSnapshot(): SubscriptionSnapshotResponse {
        val now = LocalDateTime.now(clock)
        val byTier = PlanTier.entries
            .filter { it != PlanTier.BASIC }
            .associateWith {
                userSubscriptionRepository.countByStatusInAndPlanTierAndCurrentPeriodEndAfter(
                    PAID_ACCESS_STATUSES, it, now
                )
            }

        val byStatus = SubscriptionStatus.entries
            .associateWith { userSubscriptionRepository.countByStatus(it) }

        return SubscriptionSnapshotResponse(
            byTier = byTier,
            byStatus = byStatus,
            autoRenewOn = userSubscriptionRepository.countByAutoRenew(true),
            autoRenewOff = userSubscriptionRepository.countByAutoRenew(false)
        )
    }

    private fun validateRange(from: LocalDate, to: LocalDate) {
        // MAX_RANGE_DAYS는 from~to 사이 허용되는 최대 "포함" 일수다(예: 366이면 from == to일 때 1일,
        // from.plusDays(365) == to일 때 정확히 366일이 최대 허용 폭).
        if (from.isAfter(to) || from.plusDays(MAX_RANGE_DAYS - 1) < to) {
            throw BusinessException(AdminErrorCode.INVALID_DATE_RANGE)
        }
    }

    // granularity 버킷팅은 DB 함수(DATE/DATE_FORMAT) 방언 차이를 피하기 위해
    // 승인 결제 행을 기간으로만 조회한 뒤 서버(Kotlin)에서 LocalDate/YearMonth로 묶는다.
    private fun buildSeries(payments: List<Payment>, granularity: Granularity): List<RevenuePoint> {
        val grouped = when (granularity) {
            Granularity.DAY -> payments.groupBy { it.approvedAt!!.toLocalDate().toString() }
            Granularity.MONTH -> payments.groupBy { YearMonth.from(it.approvedAt!!).toString() }
        }

        return grouped.entries
            .sortedBy { it.key }
            .map { (bucket, bucketPayments) ->
                RevenuePoint(bucket = bucket, amount = bucketPayments.sumAmount(), count = bucketPayments.size.toLong())
            }
    }

    private fun List<Payment>.sumAmount(): Long = sumOf { it.amount.toLong() }

    companion object {
        private const val MAX_RANGE_DAYS = 366L
        private val PAID_ACCESS_STATUSES = listOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED)
    }
}

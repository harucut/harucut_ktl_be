package com.harucut.payment.repository

import com.harucut.payment.entity.Payment
import com.harucut.payment.enums.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PaymentRepository : JpaRepository<Payment, Long> {

    fun findByPgTransactionId(pgTransactionId: String): Payment?

    fun countByOrderId(orderId: Long): Long

    // 매출 통계용: 승인 결제를 기간으로만 조회한다. granularity 버킷팅은 서버(Kotlin)에서
    // 수행해 DB 방언(DATE/DATE_FORMAT) 차이를 피한다.
    @Query(
        """
            select p from Payment p
            join fetch p.order o
            where p.status = :status
            and p.approvedAt >= :from
            and p.approvedAt < :to
        """
    )
    fun findApprovedInRange(
        @Param("status") status: PaymentStatus,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): List<Payment>
}

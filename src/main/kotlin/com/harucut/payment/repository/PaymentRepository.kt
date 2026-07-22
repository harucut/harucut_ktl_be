package com.harucut.payment.repository

import com.harucut.payment.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {

    fun findByPgTransactionId(pgTransactionId: String): Payment?

    fun countByOrderId(orderId: Long): Long
}

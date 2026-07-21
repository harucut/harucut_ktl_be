package com.harucut.payment.repository

import com.harucut.payment.entity.PaymentOrder
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentOrderRepository : JpaRepository<PaymentOrder, Long> {

    fun existsByIdempotencyKey(idempotencyKey: String): Boolean

    fun findByIdempotencyKey(idempotencyKey: String): PaymentOrder?
}

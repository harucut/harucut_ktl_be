package com.harucut.subscription.repository

import com.harucut.subscription.entity.PaymentTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, Long> {

    fun existsByProviderAndProviderEventId(provider: String, providerEventId: String): Boolean

    fun findByProviderAndProviderEventId(provider: String, providerEventId: String): PaymentTransaction?
}
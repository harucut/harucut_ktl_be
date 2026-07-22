package com.harucut.payment.repository

import com.harucut.payment.entity.BillingKey
import com.harucut.payment.enums.BillingKeyStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BillingKeyRepository : JpaRepository<BillingKey, Long> {

    fun findByUserIdAndStatus(userId: Long, status: BillingKeyStatus): BillingKey?

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM BillingKey b WHERE b.user.id = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)
}

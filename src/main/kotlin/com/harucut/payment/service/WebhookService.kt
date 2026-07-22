package com.harucut.payment.service

interface WebhookService {

    fun handle(rawBody: String)
}

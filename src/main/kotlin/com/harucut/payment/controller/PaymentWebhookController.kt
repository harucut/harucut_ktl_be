package com.harucut.payment.controller

import com.harucut.exception.BusinessException
import com.harucut.payment.exception.PaymentErrorCode
import com.harucut.payment.service.WebhookService
import com.harucut.payment.webhook.PaymentWebhookVerifier
import com.harucut.util.response.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Payment Webhook", description = "PG 결제 웹훅 수신 API (골격, 인증 불필요)")
@RestController
@RequestMapping("/api/payments/webhook")
class PaymentWebhookController(
    private val paymentWebhookVerifier: PaymentWebhookVerifier,
    private val webhookService: WebhookService
) {

    @Operation(summary = "결제 웹훅 수신", description = "PG가 보내는 결제 상태 변경 웹훅을 수신합니다. 서명 검증 후 멱등하게 처리합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수신 성공"),
        ApiResponse(responseCode = "400", description = "서명 검증 실패(PAY-008)")
    )
    @PostMapping
    fun handleWebhook(
        @RequestBody rawBody: String,
        @Parameter(description = "PG 서명 헤더") @RequestHeader(name = "X-Signature", required = false) signature: String?
    ): ResponseEntity<Response<Unit>> {
        if (!paymentWebhookVerifier.verify(rawBody, signature)) {
            throw BusinessException(PaymentErrorCode.WEBHOOK_SIGNATURE_INVALID)
        }
        webhookService.handle(rawBody)
        return Response.ok().toResponseEntity()
    }
}

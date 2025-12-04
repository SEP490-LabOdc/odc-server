package com.odc.paymentservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.Webhook;


@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Webhook nhận thông báo từ PayOS
     * Dùng Object Webhook của package v2
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handlePayOSWebhook(@RequestBody Webhook webhookBody) {
        log.info("Received Webhook from PayOS: {}", webhookBody);
        return ResponseEntity.ok(null);
//        try {
//            // Chuyển xử lý vào Service
//            paymentService.processWebhook(webhookBody);
//            return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully", null));
//        } catch (Exception e) {
//            log.error("Webhook processing failed", e);
//            return ResponseEntity.badRequest().body(ApiResponse.error("Webhook processing failed: " + e.getMessage()));
//        }
    }
}
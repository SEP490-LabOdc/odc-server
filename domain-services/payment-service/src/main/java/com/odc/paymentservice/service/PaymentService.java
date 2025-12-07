package com.odc.paymentservice.service;

import com.odc.paymentservice.dto.request.CreatePaymentForMilestoneRequest;
import com.odc.paymentservice.dto.request.CreatePaymentRequest;
import com.odc.paymentservice.dto.response.CreatePaymentResponse;
import vn.payos.model.webhooks.Webhook;

import java.util.UUID;

public interface PaymentService {
    void processWebhook(Webhook webhookBody);

    CreatePaymentResponse createDepositLink(UUID userId, CreatePaymentRequest request);

    void payMilestoneWithWallet(UUID userId, CreatePaymentForMilestoneRequest request);
}
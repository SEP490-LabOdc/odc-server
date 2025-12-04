package com.odc.paymentservice.service;

import com.odc.paymentservice.dto.request.CreatePaymentRequest;
import com.odc.paymentservice.dto.response.CreatePaymentResponse;
import vn.payos.model.webhooks.Webhook;

public interface PaymentService {
    void processWebhook(Webhook webhookBody);

    CreatePaymentResponse createPaymentLink(CreatePaymentRequest request);
}

package com.odc.paymentservice.service;

import vn.payos.model.webhooks.Webhook;

public interface PaymentService {
    void processWebhook(Webhook webhookBody);
}

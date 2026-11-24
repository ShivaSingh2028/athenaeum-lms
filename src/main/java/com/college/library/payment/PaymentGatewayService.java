package com.college.library.payment;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGatewayService {

    Map<String, Object> createOrder(BigDecimal amount, String currency, String receiptId);

    boolean verifySignature(String orderId, String paymentId, String signature);

    boolean verifyWebhookSignature(String payload, String signature);
}

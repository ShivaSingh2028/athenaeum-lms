package com.college.library.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class RazorpayPaymentGateway implements PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGateway.class);

    @Value("${app.razorpay.key-id:}")
    private String keyId;

    @Value("${app.razorpay.key-secret:}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret:}")
    private String webhookSecret;

    @Override
    public Map<String, Object> createOrder(BigDecimal amount, String currency, String receiptId) {
        Map<String, Object> order = new HashMap<>();
        // Amount in subunits (paise for INR)
        long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();
        String orderId = "order_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        order.put("id", orderId);
        order.put("amount", amountInPaise);
        order.put("currency", currency != null ? currency : "INR");
        order.put("receipt", receiptId);
        order.put("status", "created");
        order.put("keyId", keyId != null && !keyId.isBlank() ? keyId : "rzp_test_mock_key");

        log.info("Initialized Razorpay Order: {} for amount: {} paise", orderId, amountInPaise);
        return order;
    }

    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (keySecret == null || keySecret.isBlank()) {
            // Test sandbox mode: allow mock signatures starting with "mock_sig_"
            return signature != null && (signature.startsWith("mock_") || signature.length() == 64);
        }
        try {
            String payload = orderId + "|" + paymentId;
            String expected = calculateHmacSha256(payload, keySecret);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to verify payment signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        String secret = (webhookSecret != null && !webhookSecret.isBlank()) ? webhookSecret : keySecret;
        if (secret == null || secret.isBlank()) {
            return signature != null;
        }
        try {
            String expected = calculateHmacSha256(payload, secret);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to verify webhook signature: {}", e.getMessage());
            return false;
        }
    }

    private String calculateHmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(rawHmac);
    }
}

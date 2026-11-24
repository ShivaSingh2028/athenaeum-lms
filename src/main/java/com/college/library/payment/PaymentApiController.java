package com.college.library.payment;

import com.college.library.entity.User;
import com.college.library.finance.Fine;
import com.college.library.finance.FineService;
import com.college.library.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentApiController {

    private final PaymentGatewayService paymentGatewayService;
    private final FineService fineService;
    private final UserService userService;

    public PaymentApiController(PaymentGatewayService paymentGatewayService,
                                FineService fineService,
                                UserService userService) {
        this.paymentGatewayService = paymentGatewayService;
        this.fineService = fineService;
        this.userService = userService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        try {
            Long fineId = Long.valueOf(body.get("fineId").toString());
            Fine fine = fineService.getById(fineId);
            BigDecimal amount = fine.getOutstandingAmount();

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "No outstanding balance for fine #" + fineId));
            }

            Map<String, Object> order = paymentGatewayService.createOrder(amount, "INR", "FINE-" + fineId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {
        String orderId = body.get("razorpay_order_id");
        String paymentId = body.get("razorpay_payment_id");
        String signature = body.get("razorpay_signature");
        Long fineId = Long.valueOf(body.get("fineId"));

        boolean isValid = paymentGatewayService.verifySignature(orderId, paymentId, signature);
        if (!isValid) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payment signature"));
        }

        Fine fine = fineService.getById(fineId);
        User patron = fine.getUser();
        String idempotencyKey = "PAY-" + paymentId;

        Payment payment = fineService.recordPayment(
                fineId,
                fine.getOutstandingAmount(),
                PaymentMethod.RAZORPAY_UPI,
                paymentId,
                idempotencyKey,
                patron
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentId", payment.getId(),
                "reference", payment.getTransactionReference()
        ));
    }

    @PostMapping("/razorpay/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload,
                                           @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        boolean isValid = paymentGatewayService.verifyWebhookSignature(payload, signature);
        if (!isValid) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid webhook signature"));
        }
        // Webhook acknowledged idempotently
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}

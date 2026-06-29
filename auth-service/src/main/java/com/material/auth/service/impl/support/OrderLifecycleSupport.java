package com.material.auth.service.impl.support;

import com.material.auth.entity.OrderAcceptance;
import com.material.auth.entity.OrderPayment;
import com.material.auth.entity.PurchaseOrder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

public final class OrderLifecycleSupport {
    public static final String ACCEPTANCE_ACCEPTED = "ACCEPTED";
    public static final String ACCEPTANCE_EXCEPTION = "EXCEPTION";
    public static final String PAYMENT_PENDING = "PENDING";
    public static final String PAYMENT_PAID = "PAID";
    public static final String PAYMENT_TIMEOUT = "TIMEOUT";

    private static final Duration PAYMENT_TIMEOUT_DURATION = Duration.ofHours(1);
    private static final Set<String> PAYMENT_METHODS = Set.of("BANK_TRANSFER", "CORPORATE_CARD", "OFFLINE");

    private OrderLifecycleSupport() {
    }

    public static String normalizeAcceptanceResult(String result) {
        if (!hasText(result)) {
            return ACCEPTANCE_ACCEPTED;
        }
        String normalized = result.trim().toUpperCase();
        if (ACCEPTANCE_ACCEPTED.equals(normalized) || ACCEPTANCE_EXCEPTION.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("验收结果只能是 ACCEPTED 或 EXCEPTION");
    }

    public static String acceptanceStatusText(String result) {
        return ACCEPTANCE_EXCEPTION.equals(result) ? "异常验收" : "已验收";
    }

    public static String acceptanceSummary(OrderAcceptance acceptance) {
        return acceptanceStatusText(acceptance.getAcceptanceResult())
                + " · 签收人 " + acceptance.getSignerName()
                + " · " + acceptance.getRemark();
    }

    public static BigDecimal requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额必须大于 0");
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public static String normalizePaymentMethod(String method) {
        String normalized = hasText(method) ? method.trim().toUpperCase() : "BANK_TRANSFER";
        if (!PAYMENT_METHODS.contains(normalized)) {
            throw new IllegalArgumentException("付款方式只能是 BANK_TRANSFER、CORPORATE_CARD 或 OFFLINE");
        }
        return normalized;
    }

    public static String paymentStatusText(String status) {
        if (PAYMENT_PAID.equals(status)) {
            return "已付款";
        }
        if (PAYMENT_TIMEOUT.equals(status)) {
            return "支付超时";
        }
        return "待付款";
    }

    public static String paymentMethodText(String method) {
        return switch (method) {
            case "CORPORATE_CARD" -> "企业卡";
            case "OFFLINE" -> "线下付款";
            default -> "对公转账";
        };
    }

    public static String paymentSummary(OrderPayment payment) {
        if (PAYMENT_TIMEOUT.equals(payment.getStatus())) {
            return "支付超时 · 付款单已超过1小时，请联系管理员重新开启付款";
        }
        if (PAYMENT_PENDING.equals(payment.getStatus())) {
            return "待付款 · 请在1小时内完成付款"
                    + (payment.getExpiresAt() == null ? "" : " · 截止 " + payment.getExpiresAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        return paymentStatusText(payment.getStatus())
                + " · ¥" + payment.getAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                + " · " + paymentMethodText(payment.getPaymentMethod())
                + " · 流水号 " + payment.getPaymentReference()
                + " · " + payment.getRemark();
    }

    public static OrderPayment pendingPaymentFor(PurchaseOrder order, Long purchaserId, LocalDateTime createTime) {
        OrderPayment payment = new OrderPayment();
        payment.setOrderId(order.getId());
        payment.setPurchaserId(purchaserId);
        payment.setAmount(parseOrderAmount(order.getAmount()));
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setPaymentReference("WAITING-" + order.getId());
        payment.setStatus(PAYMENT_PENDING);
        payment.setRemark("验收完成，请在1小时内完成付款");
        payment.setExpiresAt(createTime.plus(PAYMENT_TIMEOUT_DURATION));
        payment.setCreateTime(createTime);
        payment.setUpdateTime(createTime);
        return payment;
    }

    public static boolean paymentExpired(OrderPayment payment, LocalDateTime now) {
        return payment.getExpiresAt() != null && !payment.getExpiresAt().isAfter(now);
    }

    private static BigDecimal parseOrderAmount(String amount) {
        String normalized = amount == null ? "" : amount.replaceAll("[^0-9.]", "");
        if (!hasText(normalized)) {
            return BigDecimal.ONE;
        }
        return requirePositiveAmount(new BigDecimal(normalized));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

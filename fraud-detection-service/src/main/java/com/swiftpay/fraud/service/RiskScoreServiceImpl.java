package com.swiftpay.fraud.service;

import com.swiftpay.common.enums.RiskLevel;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoreServiceImpl
        implements RiskScoreService {

    private static final int MAX_RISK_SCORE = 100;

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD =
            new BigDecimal("100000");

    private static final BigDecimal MEDIUM_AMOUNT_THRESHOLD =
            new BigDecimal("50000");

    List<FraudRule> rules;

    private static final long HIGH_VELOCITY_THRESHOLD = 5;

    private static final long MEDIUM_VELOCITY_THRESHOLD = 3;

    private static final Duration VELOCITY_WINDOW =
            Duration.ofMinutes(1);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public int calculateRiskScore(PaymentInitiatedEvent event) {

        int riskScore = 0;

        /*
         * Rule 1:
         * High-value transactions receive a higher risk score.
         */
        if (event.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {

            riskScore += 50;

            log.debug(
                    "High amount rule triggered. transactionId={}, amount={}",
                    event.getTransactionId(),
                    event.getAmount()
            );

        } else if (
                event.getAmount()
                        .compareTo(MEDIUM_AMOUNT_THRESHOLD) > 0) {

            riskScore += 25;

            log.debug(
                    "Medium amount rule triggered. transactionId={}, amount={}",
                    event.getTransactionId(),
                    event.getAmount()
            );
        }

        /*
         * Rule 2:
         * Transaction velocity.
         *
         * We maintain a counter for each sender in Redis.
         */
        long transactionCount =
                incrementSenderVelocity(event.getSenderId());

        if (transactionCount > HIGH_VELOCITY_THRESHOLD) {

            riskScore += 40;

            log.debug(
                    "High velocity rule triggered. senderId={}, count={}",
                    event.getSenderId(),
                    transactionCount
            );

        } else if (
                transactionCount > MEDIUM_VELOCITY_THRESHOLD) {

            riskScore += 20;

            log.debug(
                    "Medium velocity rule triggered. senderId={}, count={}",
                    event.getSenderId(),
                    transactionCount
            );
        }

        /*
         * Rule 3:
         * Sender and receiver are identical.
         */
        if (event.getSenderId()
                .equals(event.getReceiverId())) {

            riskScore += 30;

            log.debug(
                    "Self-transfer rule triggered. transactionId={}",
                    event.getTransactionId()
            );
        }

        /*
         * Keep score between 0 and 100.
         */
        return Math.min(
                riskScore,
                MAX_RISK_SCORE
        );
    }

    @Override
    public RiskLevel determineRiskLevel(int riskScore) {

        if (riskScore < 40) {
            return RiskLevel.LOW;
        }

        if (riskScore < 70) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.HIGH;
    }

    @Override
    public String buildRiskReason(
            PaymentInitiatedEvent event,
            int riskScore) {

        long transactionCount =
                getCurrentSenderVelocity(
                        event.getSenderId()
                );

        if (event.getAmount()
                .compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {

            return "HIGH_TRANSACTION_AMOUNT";
        }

        if (transactionCount > HIGH_VELOCITY_THRESHOLD) {

            return "HIGH_TRANSACTION_VELOCITY";
        }

        if (event.getSenderId()
                .equals(event.getReceiverId())) {

            return "SELF_TRANSFER";
        }

        if (riskScore >= 40) {

            return "MEDIUM_RISK_TRANSACTION";
        }

        return "LOW_RISK_TRANSACTION";
    }

    private long incrementSenderVelocity(
            String senderId) {

        String key =
                "fraud:velocity:sender:" + senderId;

        Long count =
                redisTemplate.opsForValue().increment(key);

        /*
         * Set expiration only when the key is created.
         *
         * This gives us a rolling one-minute transaction
         * velocity window.
         */
        if (count != null && count == 1) {

            redisTemplate.expire(
                    key,
                    VELOCITY_WINDOW
            );
        }

        return count == null ? 0 : count;
    }

    private long getCurrentSenderVelocity(
            String senderId) {

        String key =
                "fraud:velocity:sender:" + senderId;

        Object value =
                redisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(
                    value.toString()
            );
        } catch (NumberFormatException exception) {

            log.warn(
                    "Invalid Redis velocity value. senderId={}, value={}",
                    senderId,
                    value
            );

            return 0;
        }
    }
}
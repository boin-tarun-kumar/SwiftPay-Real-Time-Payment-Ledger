package com.swiftpay.fraud.rule;

import com.swiftpay.common.event.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class VelocityRule implements FraudRule {

    private static final int HIGH_VELOCITY_RISK_POINTS = 40;

    private static final int MEDIUM_VELOCITY_RISK_POINTS = 20;

    private static final long HIGH_VELOCITY_THRESHOLD = 5;

    private static final long MEDIUM_VELOCITY_THRESHOLD = 3;

    private static final Duration VELOCITY_WINDOW =
            Duration.ofMinutes(1);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public RuleResult evaluate(PaymentInitiatedEvent event) {

        if (event == null || event.getSenderId() == null) {
            return RuleResult.notTriggered();
        }

        String key =
                "fraud:velocity:sender:"
                        + event.getSenderId();

        Long transactionCount =
                redisTemplate.opsForValue().increment(key);

        if (transactionCount == null) {
            return RuleResult.notTriggered();
        }

        /*
         * Set TTL only when the counter is first created.
         *
         * Example:
         *
         * Transaction 1 -> count 1 -> TTL 60 sec
         * Transaction 2 -> count 2
         * Transaction 3 -> count 3
         * ...
         */
        if (transactionCount == 1) {

            redisTemplate.expire(
                    key,
                    VELOCITY_WINDOW
            );
        }

        log.debug(
                "Sender transaction velocity. senderId={}, count={}",
                event.getSenderId(),
                transactionCount
        );

        if (transactionCount > HIGH_VELOCITY_THRESHOLD) {

            return RuleResult.triggered(
                    HIGH_VELOCITY_RISK_POINTS,
                    "HIGH_TRANSACTION_VELOCITY"
            );
        }

        if (transactionCount > MEDIUM_VELOCITY_THRESHOLD) {

            return RuleResult.triggered(
                    MEDIUM_VELOCITY_RISK_POINTS,
                    "MEDIUM_TRANSACTION_VELOCITY"
            );
        }

        return RuleResult.notTriggered();
    }

    @Override
    public String getRuleName() {
        return "VELOCITY_RULE";
    }
}
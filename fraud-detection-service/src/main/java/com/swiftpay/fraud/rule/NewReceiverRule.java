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
public class NewReceiverRule implements FraudRule {

    private static final int NEW_RECEIVER_RISK_POINTS = 15;

    private static final Duration RECEIVER_HISTORY_TTL =
            Duration.ofDays(30);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public RuleResult evaluate(PaymentInitiatedEvent event) {

        if (event == null ||
                event.getSenderId() == null ||
                event.getReceiverId() == null) {

            return RuleResult.notTriggered();
        }

        /*
         * We maintain the receiver history for each sender.
         *
         * Redis Set:
         *
         * fraud:receivers:{senderId}
         *
         * Example:
         *
         * fraud:receivers:USER-1001
         *      -> USER-2001
         *      -> USER-2002
         *      -> USER-2005
         */
        String key =
                "fraud:receivers:"
                        + event.getSenderId();

        Boolean receiverExists =
                redisTemplate.opsForSet()
                        .isMember(
                                key,
                                event.getReceiverId()
                        );

        if (Boolean.FALSE.equals(receiverExists)) {

            /*
             * Store the receiver so future payments
             * from the same sender are recognized.
             */
            redisTemplate.opsForSet()
                    .add(
                            key,
                            event.getReceiverId()
                    );

            redisTemplate.expire(
                    key,
                    RECEIVER_HISTORY_TTL
            );

            log.debug(
                    "New receiver detected. senderId={}, receiverId={}",
                    event.getSenderId(),
                    event.getReceiverId()
            );

            return RuleResult.triggered(
                    NEW_RECEIVER_RISK_POINTS,
                    "NEW_RECEIVER"
            );
        }

        return RuleResult.notTriggered();
    }

    @Override
    public String getRuleName() {
        return "NEW_RECEIVER_RULE";
    }
}
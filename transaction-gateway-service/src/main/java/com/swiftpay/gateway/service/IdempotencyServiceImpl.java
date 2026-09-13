package com.swiftpay.gateway.service;

import com.swiftpay.common.constants.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl
        implements IdempotencyService {

    private final RedisTemplate<String, Object>
            redisTemplate;

    @Override
    public boolean isDuplicate(
            String transactionId
    ) {

        String key =
                RedisKeys.IDEMPOTENCY_KEY
                        + transactionId;

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key)
        );
    }

    @SuppressWarnings("null")
@Override
    public void saveTransaction(
            String transactionId
    ) {

        String key =
                RedisKeys.IDEMPOTENCY_KEY
                        + transactionId;

        redisTemplate
                .opsForValue()
                .set(
                        key,
                        "PROCESSED",
                        Duration.ofHours(24)
                );
    }
}
package com.swiftpay.fraud.service;

import com.swiftpay.common.enums.RiskLevel;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskScoreServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RiskScoreServiceImpl riskScoreService;

    private PaymentInitiatedEvent event;

    @BeforeEach
    void setUp() {

        event = mock(PaymentInitiatedEvent.class);

        when(event.getTransactionId())
                .thenReturn("TXN-10001");

        when(event.getSenderId())
                .thenReturn("USER-1001");

        when(event.getReceiverId())
                .thenReturn("USER-2001");

        when(event.getCurrency())
                .thenReturn("INR");

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }

    @Test
    void shouldCalculateLowRiskScore() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("1000"));

        when(valueOperations.increment(anyString()))
                .thenReturn(1L);

        int score =
                riskScoreService.calculateRiskScore(event);

        assertEquals(0, score);
    }

    @Test
    void shouldAddHighAmountRiskPoints() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("150000"));

        when(valueOperations.increment(anyString()))
                .thenReturn(1L);

        int score =
                riskScoreService.calculateRiskScore(event);

        assertEquals(50, score);
    }

    @Test
    void shouldAddMediumAmountRiskPoints() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("75000"));

        when(valueOperations.increment(anyString()))
                .thenReturn(1L);

        int score =
                riskScoreService.calculateRiskScore(event);

        assertEquals(25, score);
    }

    @Test
    void shouldAddHighVelocityRiskPoints() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("1000"));

        when(valueOperations.increment(anyString()))
                .thenReturn(6L);

        int score =
                riskScoreService.calculateRiskScore(event);

        assertEquals(40, score);
    }

    @Test
    void shouldAddMediumVelocityRiskPoints() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("1000"));

        when(valueOperations.increment(anyString()))
                .thenReturn(4L);

        int score =
                riskScoreService.calculateRiskScore(event);

        assertEquals(20, score);
    }

    @Test
    void shouldAddSelfTransferRiskPoints() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("1000"));

        when(event.getReceiverId())
                .thenReturn("USER-1001");

        when(valueOperations.increment(anyString()))
                .thenReturn(1L);

        int score =
                riskScoreService.calculateRiskScore(event);

        assertEquals(30, score);
    }

    @Test
    void shouldCapRiskScoreAt100() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("150000"));

        when(event.getReceiverId())
                .thenReturn("USER-1001");

        when(valueOperations.increment(anyString()))
                .thenReturn(6L);

        int score =
                riskScoreService.calculateRiskScore(event);

        assertEquals(100, score);
    }

    @Test
    void shouldDetermineLowRisk() {

        assertEquals(
                RiskLevel.LOW,
                riskScoreService.determineRiskLevel(20)
        );
    }

    @Test
    void shouldDetermineMediumRisk() {

        assertEquals(
                RiskLevel.MEDIUM,
                riskScoreService.determineRiskLevel(50)
        );
    }

    @Test
    void shouldDetermineHighRisk() {

        assertEquals(
                RiskLevel.HIGH,
                riskScoreService.determineRiskLevel(80)
        );
    }

    @Test
    void shouldBuildHighAmountReason() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("150000"));

        when(valueOperations.get(anyString()))
                .thenReturn(1L);

        String reason =
                riskScoreService.buildRiskReason(
                        event,
                        50
                );

        assertEquals(
                "HIGH_TRANSACTION_AMOUNT",
                reason
        );
    }

    @Test
    void shouldBuildHighVelocityReason() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("1000"));

        when(valueOperations.get(anyString()))
                .thenReturn(6L);

        String reason =
                riskScoreService.buildRiskReason(
                        event,
                        40
                );

        assertEquals(
                "HIGH_TRANSACTION_VELOCITY",
                reason
        );
    }

    @Test
    void shouldBuildSelfTransferReason() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("1000"));

        when(event.getReceiverId())
                .thenReturn("USER-1001");

        when(valueOperations.get(anyString()))
                .thenReturn(1L);

        String reason =
                riskScoreService.buildRiskReason(
                        event,
                        30
                );

        assertEquals(
                "SELF_TRANSFER",
                reason
        );
    }
}
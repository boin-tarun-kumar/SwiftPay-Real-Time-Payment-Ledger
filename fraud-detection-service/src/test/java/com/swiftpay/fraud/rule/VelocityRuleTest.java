package com.swiftpay.fraud.rule;

import com.swiftpay.common.event.PaymentInitiatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VelocityRuleTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private PaymentInitiatedEvent event;

    private VelocityRule velocityRule;

    @BeforeEach
    void setUp() {

        velocityRule =
                new VelocityRule(redisTemplate);

        when(event.getSenderId())
                .thenReturn("USER-1001");

        when(event.getTransactionId())
                .thenReturn("TXN-10001");

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }

    @Test
    void shouldNotTriggerForNormalVelocity() {

        when(valueOperations.increment(anyString()))
                .thenReturn(1L);

        FraudRule.RuleResult result =
                velocityRule.evaluate(event);

        assertFalse(result.triggered());

        assertEquals(
                0,
                result.riskPoints()
        );

        verify(
                valueOperations,
                times(1)
        ).increment(
                "fraud:velocity:sender:USER-1001"
        );
    }

    @Test
    void shouldTriggerMediumVelocity() {

        when(valueOperations.increment(anyString()))
                .thenReturn(4L);

        FraudRule.RuleResult result =
                velocityRule.evaluate(event);

        assertTrue(result.triggered());

        assertEquals(
                20,
                result.riskPoints()
        );

        assertEquals(
                "MEDIUM_TRANSACTION_VELOCITY",
                result.reason()
        );
    }

    @Test
    void shouldTriggerHighVelocity() {

        when(valueOperations.increment(anyString()))
                .thenReturn(6L);

        FraudRule.RuleResult result =
                velocityRule.evaluate(event);

        assertTrue(result.triggered());

        assertEquals(
                40,
                result.riskPoints()
        );

        assertEquals(
                "HIGH_TRANSACTION_VELOCITY",
                result.reason()
        );
    }

    @Test
    void shouldSetTtlWhenCounterIsCreated() {

        when(valueOperations.increment(anyString()))
                .thenReturn(1L);

        velocityRule.evaluate(event);

        verify(redisTemplate)
                .expire(
                        eq("fraud:velocity:sender:USER-1001"),
                        any()
                );
    }

    @Test
    void shouldNotResetTtlForExistingCounter() {

        when(valueOperations.increment(anyString()))
                .thenReturn(4L);

        velocityRule.evaluate(event);

        verify(
                redisTemplate,
                never()
        ).expire(
                anyString(),
                any()
        );
    }

    @Test
    void shouldReturnNoRiskWhenRedisReturnsNull() {

        when(valueOperations.increment(anyString()))
                .thenReturn(null);

        FraudRule.RuleResult result =
                velocityRule.evaluate(event);

        assertFalse(result.triggered());

        assertEquals(
                0,
                result.riskPoints()
        );
    }

    @Test
    void shouldReturnNoRiskForNullEvent() {

        FraudRule.RuleResult result =
                velocityRule.evaluate(null);

        assertFalse(result.triggered());

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldReturnCorrectRuleName() {

        assertEquals(
                "VELOCITY_RULE",
                velocityRule.getRuleName()
        );
    }
}
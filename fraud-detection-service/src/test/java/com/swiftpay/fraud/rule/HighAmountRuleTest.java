package com.swiftpay.fraud.rule;

import com.swiftpay.common.event.PaymentInitiatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighAmountRuleTest {

    private HighAmountRule highAmountRule;

    @Mock
    private PaymentInitiatedEvent event;

    @BeforeEach
    void setUp() {

        highAmountRule =
                new HighAmountRule();

        /*
         * Since the production class uses @Value,
         * inject the values directly for the unit test.
         */
        org.springframework.test.util.ReflectionTestUtils.setField(
                highAmountRule,
                "highAmountThreshold",
                new BigDecimal("100000")
        );

        org.springframework.test.util.ReflectionTestUtils.setField(
                highAmountRule,
                "mediumAmountThreshold",
                new BigDecimal("50000")
        );
    }

    @Test
    void shouldTriggerHighAmountRule() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("150000"));

        FraudRule.RuleResult result =
                highAmountRule.evaluate(event);

        assertTrue(result.triggered());

        assertEquals(
                50,
                result.riskPoints()
        );

        assertEquals(
                "HIGH_TRANSACTION_AMOUNT",
                result.reason()
        );
    }

    @Test
    void shouldTriggerMediumAmountRule() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("75000"));

        FraudRule.RuleResult result =
                highAmountRule.evaluate(event);

        assertTrue(result.triggered());

        assertEquals(
                25,
                result.riskPoints()
        );

        assertEquals(
                "MEDIUM_TRANSACTION_AMOUNT",
                result.reason()
        );
    }

    @Test
    void shouldNotTriggerForNormalAmount() {

        when(event.getAmount())
                .thenReturn(new BigDecimal("10000"));

        FraudRule.RuleResult result =
                highAmountRule.evaluate(event);

        assertFalse(result.triggered());

        assertEquals(
                0,
                result.riskPoints()
        );

        assertNull(result.reason());
    }

    @Test
    void shouldNotTriggerForNullEvent() {

        FraudRule.RuleResult result =
                highAmountRule.evaluate(null);

        assertFalse(result.triggered());

        assertEquals(
                0,
                result.riskPoints()
        );
    }

    @Test
    void shouldNotTriggerForNullAmount() {

        when(event.getAmount())
                .thenReturn(null);

        FraudRule.RuleResult result =
                highAmountRule.evaluate(event);

        assertFalse(result.triggered());
    }

    @Test
    void shouldReturnCorrectRuleName() {

        assertEquals(
                "HIGH_AMOUNT_RULE",
                highAmountRule.getRuleName()
        );
    }
}
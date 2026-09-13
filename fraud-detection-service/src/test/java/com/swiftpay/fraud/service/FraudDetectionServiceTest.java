package com.swiftpay.fraud.service;

import com.swiftpay.common.enums.FraudStatus;
import com.swiftpay.common.enums.RiskLevel;
import com.swiftpay.common.event.FraudCheckFailedEvent;
import com.swiftpay.common.event.FraudCheckPassedEvent;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.fraud.producer.FraudResultProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private RiskScoreService riskScoreService;

    @Mock
    private FraudResultProducer fraudResultProducer;

    @InjectMocks
    private FraudDetectionServiceImpl fraudDetectionService;

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

        when(event.getAmount())
                .thenReturn(new BigDecimal("5000.00"));

        when(event.getCurrency())
                .thenReturn("INR");
    }

    @Test
    void shouldApproveLowRiskTransaction() {

        when(riskScoreService.calculateRiskScore(event))
                .thenReturn(20);

        when(riskScoreService.determineRiskLevel(20))
                .thenReturn(RiskLevel.LOW);

        when(riskScoreService.buildRiskReason(event, 20))
                .thenReturn("LOW_RISK_TRANSACTION");

        fraudDetectionService.evaluateTransaction(event);

        ArgumentCaptor<FraudCheckPassedEvent> captor =
                ArgumentCaptor.forClass(
                        FraudCheckPassedEvent.class
                );

        verify(fraudResultProducer)
                .publishPassed(captor.capture());

        FraudCheckPassedEvent result =
                captor.getValue();

        assertEquals(
                "TXN-10001",
                result.getTransactionId()
        );

        assertEquals(
                FraudStatus.APPROVED,
                result.getStatus()
        );

        assertEquals(
                20,
                result.getRiskScore()
        );

        assertEquals(
                RiskLevel.LOW,
                result.getRiskLevel()
        );

        verify(fraudResultProducer, never())
                .publishFailed(any());
    }

    @Test
    void shouldApproveMediumRiskTransaction() {

        when(riskScoreService.calculateRiskScore(event))
                .thenReturn(50);

        when(riskScoreService.determineRiskLevel(50))
                .thenReturn(RiskLevel.MEDIUM);

        when(riskScoreService.buildRiskReason(event, 50))
                .thenReturn("MEDIUM_RISK_TRANSACTION");

        fraudDetectionService.evaluateTransaction(event);

        verify(fraudResultProducer)
                .publishPassed(any(FraudCheckPassedEvent.class));

        verify(fraudResultProducer, never())
                .publishFailed(any());
    }

    @Test
    void shouldBlockHighRiskTransaction() {

        when(riskScoreService.calculateRiskScore(event))
                .thenReturn(85);

        when(riskScoreService.determineRiskLevel(85))
                .thenReturn(RiskLevel.HIGH);

        when(riskScoreService.buildRiskReason(event, 85))
                .thenReturn("HIGH_TRANSACTION_AMOUNT");

        fraudDetectionService.evaluateTransaction(event);

        ArgumentCaptor<FraudCheckFailedEvent> captor =
                ArgumentCaptor.forClass(
                        FraudCheckFailedEvent.class
                );

        verify(fraudResultProducer)
                .publishFailed(captor.capture());

        FraudCheckFailedEvent result =
                captor.getValue();

        assertEquals(
                "TXN-10001",
                result.getTransactionId()
        );

        assertEquals(
                FraudStatus.BLOCKED,
                result.getStatus()
        );

        assertEquals(
                85,
                result.getRiskScore()
        );

        assertEquals(
                RiskLevel.HIGH,
                result.getRiskLevel()
        );

        verify(fraudResultProducer, never())
                .publishPassed(any());
    }

    @Test
    void shouldRejectNullEvent() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> fraudDetectionService
                                .evaluateTransaction(null)
                );

        assertEquals(
                "PaymentInitiatedEvent cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                riskScoreService,
                fraudResultProducer
        );
    }

    @Test
    void shouldRejectBlankTransactionId() {

        when(event.getTransactionId())
                .thenReturn("");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> fraudDetectionService
                                .evaluateTransaction(event)
                );

        assertEquals(
                "Transaction ID cannot be blank",
                exception.getMessage()
        );

        verifyNoInteractions(
                riskScoreService,
                fraudResultProducer
        );
    }

    @Test
    void shouldRejectZeroAmount() {

        when(event.getAmount())
                .thenReturn(BigDecimal.ZERO);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> fraudDetectionService
                                .evaluateTransaction(event)
                );

        assertEquals(
                "Payment amount must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(
                riskScoreService,
                fraudResultProducer
        );
    }
}
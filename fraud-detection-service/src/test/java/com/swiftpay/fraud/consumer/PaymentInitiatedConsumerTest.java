package com.swiftpay.fraud.consumer;

import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.fraud.service.FraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInitiatedConsumerTest {

    @Mock
    private FraudDetectionService fraudDetectionService;

    @InjectMocks
    private PaymentInitiatedConsumer consumer;

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
    void shouldProcessPaymentInitiatedEvent() {

        doNothing()
                .when(fraudDetectionService)
                .evaluateTransaction(event);

        assertDoesNotThrow(
                () -> consumer.consume(event)
        );

        verify(fraudDetectionService, times(1))
                .evaluateTransaction(event);
    }

    @Test
    void shouldIgnoreNullEvent() {

        assertDoesNotThrow(
                () -> consumer.consume(null)
        );

        verify(
                fraudDetectionService,
                never()
        ).evaluateTransaction(any());
    }

    @Test
    void shouldPropagateProcessingException() {

        RuntimeException exception =
                new RuntimeException("Fraud service unavailable");

        doThrow(exception)
                .when(fraudDetectionService)
                .evaluateTransaction(event);

        RuntimeException thrown =
                org.junit.jupiter.api.Assertions.assertThrows(
                        RuntimeException.class,
                        () -> consumer.consume(event)
                );

        org.junit.jupiter.api.Assertions.assertSame(
                exception,
                thrown
        );

        verify(
                fraudDetectionService,
                times(1)
        ).evaluateTransaction(event);
    }
}
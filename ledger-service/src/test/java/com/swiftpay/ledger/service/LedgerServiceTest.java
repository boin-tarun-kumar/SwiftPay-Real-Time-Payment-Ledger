package com.swiftpay.ledger.service;

import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.ledger.producer.PaymentStatusProducer;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import com.swiftpay.ledger.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private PaymentStatusProducer paymentStatusProducer;

    @InjectMocks
    private LedgerServiceImpl ledgerService;

    @Test
    void shouldInitializeService() {

        @SuppressWarnings("unused")
        PaymentInitiatedEvent event =
                new PaymentInitiatedEvent();

        verifyNoInteractions(
                accountRepository,
                paymentRepository,
                ledgerEntryRepository,
                paymentStatusProducer
        );
    }
}
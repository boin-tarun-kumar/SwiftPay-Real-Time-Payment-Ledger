package com.swiftpay.fraud.service;

import com.swiftpay.common.event.PaymentInitiatedEvent;

public interface FraudDetectionService {

    void evaluateTransaction(PaymentInitiatedEvent event);
}
package com.swiftpay.fraud.service;

import com.swiftpay.common.enums.RiskLevel;
import com.swiftpay.common.event.PaymentInitiatedEvent;

public interface RiskScoreService {

    int calculateRiskScore(PaymentInitiatedEvent event);

    RiskLevel determineRiskLevel(int riskScore);

    String buildRiskReason(
            PaymentInitiatedEvent event,
            int riskScore);
}
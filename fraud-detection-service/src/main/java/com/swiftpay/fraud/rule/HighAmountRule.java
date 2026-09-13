package com.swiftpay.fraud.rule;

import com.swiftpay.common.event.PaymentInitiatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class HighAmountRule implements FraudRule {

    private static final int HIGH_AMOUNT_RISK_POINTS = 50;

    private static final int MEDIUM_AMOUNT_RISK_POINTS = 25;

    @Value("${swiftpay.fraud.rules.high-amount-threshold:100000}")
    private BigDecimal highAmountThreshold;

    @Value("${swiftpay.fraud.rules.medium-amount-threshold:50000}")
    private BigDecimal mediumAmountThreshold;

    @Override
    public RuleResult evaluate(PaymentInitiatedEvent event) {

        if (event == null || event.getAmount() == null) {
            return RuleResult.notTriggered();
        }

        BigDecimal amount = event.getAmount();

        if (amount.compareTo(highAmountThreshold) > 0) {

            log.debug(
                    "High amount fraud rule triggered. transactionId={}, amount={}",
                    event.getTransactionId(),
                    amount
            );

            return RuleResult.triggered(
                    HIGH_AMOUNT_RISK_POINTS,
                    "HIGH_TRANSACTION_AMOUNT"
            );
        }

        if (amount.compareTo(mediumAmountThreshold) > 0) {

            log.debug(
                    "Medium amount fraud rule triggered. transactionId={}, amount={}",
                    event.getTransactionId(),
                    amount
            );

            return RuleResult.triggered(
                    MEDIUM_AMOUNT_RISK_POINTS,
                    "MEDIUM_TRANSACTION_AMOUNT"
            );
        }

        return RuleResult.notTriggered();
    }

    @Override
    public String getRuleName() {
        return "HIGH_AMOUNT_RULE";
    }
}
package com.swiftpay.fraud.service;

import com.swiftpay.common.enums.FraudStatus;
import com.swiftpay.common.enums.RiskLevel;
import com.swiftpay.common.event.FraudCheckFailedEvent;
import com.swiftpay.common.event.FraudCheckPassedEvent;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.fraud.producer.FraudResultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionServiceImpl
        implements FraudDetectionService {

    private final RiskScoreService riskScoreService;
    private final FraudResultProducer fraudResultProducer;

    @Override
    public void evaluateTransaction(PaymentInitiatedEvent event) {

        validateEvent(event);

        String transactionId = event.getTransactionId();

        log.info(
                "Starting fraud evaluation. transactionId={}, senderId={}, receiverId={}, amount={}",
                transactionId,
                event.getSenderId(),
                event.getReceiverId(),
                event.getAmount()
        );

        int riskScore = riskScoreService.calculateRiskScore(event);

        RiskLevel riskLevel =
                riskScoreService.determineRiskLevel(riskScore);

        FraudStatus fraudStatus =
                determineFraudStatus(riskLevel);

        String reason =
                riskScoreService.buildRiskReason(event, riskScore);

        log.info(
                "Fraud evaluation result. transactionId={}, riskScore={}, riskLevel={}, status={}, reason={}",
                transactionId,
                riskScore,
                riskLevel,
                fraudStatus,
                reason
        );

        if (fraudStatus == FraudStatus.APPROVED) {

            FraudCheckPassedEvent passedEvent =
                    FraudCheckPassedEvent.builder()
                            .transactionId(transactionId)
                            .senderId(event.getSenderId())
                            .receiverId(event.getReceiverId())
                            .amount(event.getAmount())
                            .currency(event.getCurrency())
                            .status(FraudStatus.APPROVED)
                            .riskScore(riskScore)
                            .riskLevel(riskLevel)
                            .reason(reason)
                            .build();

            fraudResultProducer.publishPassed(passedEvent);

        } else {

            FraudCheckFailedEvent failedEvent =
                    FraudCheckFailedEvent.builder()
                            .transactionId(transactionId)
                            .senderId(event.getSenderId())
                            .receiverId(event.getReceiverId())
                            .amount(event.getAmount())
                            .currency(event.getCurrency())
                            .status(fraudStatus)
                            .riskScore(riskScore)
                            .riskLevel(riskLevel)
                            .reason(reason)
                            .build();

            fraudResultProducer.publishFailed(failedEvent);
        }
    }

    private FraudStatus determineFraudStatus(
            RiskLevel riskLevel) {

        return switch (riskLevel) {

            case LOW, MEDIUM -> FraudStatus.APPROVED;

            case HIGH -> FraudStatus.BLOCKED;
        };
    }

    private void validateEvent(PaymentInitiatedEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "PaymentInitiatedEvent cannot be null"
            );
        }

        if (isBlank(event.getTransactionId())) {
            throw new IllegalArgumentException(
                    "Transaction ID cannot be blank"
            );
        }

        if (isBlank(event.getSenderId())) {
            throw new IllegalArgumentException(
                    "Sender ID cannot be blank"
            );
        }

        if (isBlank(event.getReceiverId())) {
            throw new IllegalArgumentException(
                    "Receiver ID cannot be blank"
            );
        }

        if (event.getAmount() == null ||
                event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }

        if (isBlank(event.getCurrency())) {
            throw new IllegalArgumentException(
                    "Currency cannot be blank"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
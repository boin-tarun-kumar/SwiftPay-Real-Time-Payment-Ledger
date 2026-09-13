package com.swiftpay.fraud.consumer;

import com.swiftpay.common.event.PaymentInitiatedEvent;
import com.swiftpay.fraud.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentInitiatedConsumer {

    private static final String PAYMENT_INITIATED_TOPIC = "payment-initiated";

    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(
            topics = PAYMENT_INITIATED_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "fraudKafkaListenerContainerFactory"
    )
    public void consume(PaymentInitiatedEvent event) {

        if (event == null) {
            log.warn("Received null PaymentInitiatedEvent");
            return;
        }

        log.info(
                "Received PaymentInitiated event. transactionId={}, senderId={}, receiverId={}, amount={}",
                event.getTransactionId(),
                event.getSenderId(),
                event.getReceiverId(),
                event.getAmount()
        );

        try {

            fraudDetectionService.evaluateTransaction(event);

            log.info(
                    "Fraud evaluation completed successfully. transactionId={}",
                    event.getTransactionId()
            );

        } catch (Exception exception) {

            log.error(
                    "Fraud evaluation failed. transactionId={}",
                    event.getTransactionId(),
                    exception
            );

            /*
             * Re-throwing the exception is important.
             *
             * Spring Kafka's DefaultErrorHandler from
             * KafkaConsumerConfig will then perform retries.
             */
            throw exception;
        }
    }
}
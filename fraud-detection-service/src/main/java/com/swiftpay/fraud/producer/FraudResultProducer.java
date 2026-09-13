package com.swiftpay.fraud.producer;

import com.swiftpay.common.event.FraudCheckFailedEvent;
import com.swiftpay.common.event.FraudCheckPassedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudResultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${swiftpay.kafka.topics.fraud-result:fraud-check-result}")
    private String fraudResultTopic;

    public void publishPassed(FraudCheckPassedEvent event) {

        publish(
                event.getTransactionId(),
                event
        );

        log.info(
                "Published FraudCheckPassed event. transactionId={}, riskScore={}",
                event.getTransactionId(),
                event.getRiskScore()
        );
    }

    public void publishFailed(FraudCheckFailedEvent event) {

        publish(
                event.getTransactionId(),
                event
        );

        log.info(
                "Published FraudCheckFailed event. transactionId={}, riskScore={}",
                event.getTransactionId(),
                event.getRiskScore()
        );
    }

    private void publish(
            String transactionId,
            Object event) {

        kafkaTemplate
                .send(
                        fraudResultTopic,
                        transactionId,
                        event
                )
                .whenComplete(
                        (result, exception) -> {

                            if (exception != null) {

                                log.error(
                                        "Failed to publish fraud result. transactionId={}",
                                        transactionId,
                                        exception
                                );

                                return;
                            }

                            log.debug(
                                    "Fraud result published. transactionId={}, topic={}, partition={}, offset={}",
                                    transactionId,
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset()
                            );
                        }
                );
    }
}
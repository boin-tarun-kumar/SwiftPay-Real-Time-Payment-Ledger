package com.swiftpay.fraud.integration;

import com.swiftpay.common.event.FraudCheckFailedEvent;
import com.swiftpay.common.event.FraudCheckPassedEvent;
import com.swiftpay.common.event.PaymentInitiatedEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "payment-initiated",
                "fraud-check-result"
        },
        brokerProperties = {
                "auto.create.topics.enable=true"
        }
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FraudDetectionFlowIT {

    private static final String PAYMENT_INITIATED_TOPIC =
            "payment-initiated";

    private static final String FRAUD_RESULT_TOPIC =
            "fraud-check-result";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName("swiftpay")
                    .withUsername("swiftpay")
                    .withPassword("swiftpay");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                    "redis:7-alpine"
            )
                    .withExposedPorts(6379);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, Object> resultConsumer;

    @DynamicPropertySource
    static void registerProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.data.redis.host",
                REDIS::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () -> REDIS
                        .getMappedPort(6379)
        );

        /*
         * The @EmbeddedKafka annotation exposes the broker
         * through the Spring Kafka bootstrap property.
         */
        registry.add(
                "spring.kafka.bootstrap-servers",
                () -> "localhost:"
                        + embeddedKafkaBroker
                        .getKafkaServer(0)
                        .config()
                        .getString(
                                "listeners"
                        )
                        .split(":")[2]
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "false"
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );
    }

    @BeforeAll
    void setUpConsumer() {

        Map<String, Object> consumerProperties =
                KafkaTestUtils.consumerProps(
                        "fraud-it-consumer",
                        "false",
                        embeddedKafkaBroker
                );

        consumerProperties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        JsonDeserializer<Object> deserializer =
                new JsonDeserializer<>();

        deserializer.addTrustedPackages(
                "com.swiftpay.common.event",
                "com.swiftpay.common.enums"
        );

        resultConsumer =
                new org.apache.kafka.clients.consumer.KafkaConsumer<>(
                        consumerProperties,
                        new StringDeserializer(),
                        deserializer
                );

        resultConsumer.subscribe(
                java.util.List.of(
                        FRAUD_RESULT_TOPIC
                )
        );
    }

    @AfterAll
    void tearDownConsumer() {

        if (resultConsumer != null) {
            resultConsumer.close();
        }
    }

    @Test
    void shouldProcessLowRiskPayment() {

        PaymentInitiatedEvent event =
                PaymentInitiatedEvent.builder()
                        .transactionId("TXN-IT-001")
                        .senderId("USER-1001")
                        .receiverId("USER-2001")
                        .amount(new BigDecimal("1000.00"))
                        .currency("INR")
                        .build();

        kafkaTemplate.send(
                PAYMENT_INITIATED_TOPIC,
                event.getTransactionId(),
                event
        );

        kafkaTemplate.flush();

        ConsumerRecord<String, Object> record =
                waitForFraudResult(
                        "TXN-IT-001"
                );

        assertNotNull(record);

        assertEquals(
                "TXN-IT-001",
                record.key()
        );

        assertInstanceOf(
                FraudCheckPassedEvent.class,
                record.value()
        );

        FraudCheckPassedEvent result =
                (FraudCheckPassedEvent) record.value();

        assertEquals(
                "TXN-IT-001",
                result.getTransactionId()
        );

        assertEquals(
                0,
                result.getRiskScore()
        );
    }

    @Test
    void shouldBlockHighRiskPayment() {

        PaymentInitiatedEvent event =
                PaymentInitiatedEvent.builder()
                        .transactionId("TXN-IT-002")
                        .senderId("USER-1002")
                        .receiverId("USER-2002")
                        .amount(new BigDecimal("150000.00"))
                        .currency("INR")
                        .build();

        kafkaTemplate.send(
                PAYMENT_INITIATED_TOPIC,
                event.getTransactionId(),
                event
        );

        kafkaTemplate.flush();

        ConsumerRecord<String, Object> record =
                waitForFraudResult(
                        "TXN-IT-002"
                );

        assertNotNull(record);

        assertInstanceOf(
                FraudCheckFailedEvent.class,
                record.value()
        );

        FraudCheckFailedEvent result =
                (FraudCheckFailedEvent) record.value();

        assertEquals(
                "TXN-IT-002",
                result.getTransactionId()
        );

        assertTrue(
                result.getRiskScore() >= 70
        );
    }

    private ConsumerRecord<String, Object> waitForFraudResult(
            String transactionId) {

        long timeout =
                System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < timeout) {

            var records =
                    resultConsumer.poll(
                            Duration.ofMillis(500)
                    );

            for (ConsumerRecord<String, Object> record : records) {

                if (transactionId.equals(
                        record.key()
                )) {

                    return record;
                }
            }
        }

        fail(
                "Timed out waiting for fraud result. transactionId="
                        + transactionId
        );

        return null;
    }
}
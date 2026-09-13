package com.swiftpay.fraud.config;

import com.swiftpay.common.event.PaymentInitiatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, PaymentInitiatedEvent> fraudConsumerFactory() {

        JsonDeserializer<PaymentInitiatedEvent> jsonDeserializer =
                new JsonDeserializer<>(PaymentInitiatedEvent.class);

        jsonDeserializer.addTrustedPackages(
                "com.swiftpay.common.event"
        );

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        /*
         * Disable automatic offset commits.
         *
         * Kafka offset will be committed only after
         * successful processing by the consumer.
         */
        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        /*
         * Prevent excessive records from being pulled
         * by a slow fraud-processing consumer.
         */
        properties.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                100
        );

        properties.put(
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
                15000
        );

        properties.put(
                ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
                5000
        );

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentInitiatedEvent>
    fraudKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, PaymentInitiatedEvent>
                factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(fraudConsumerFactory());

        /*
         * Retry failed messages three times.
         *
         * Example:
         * Attempt 1 -> fail
         * Attempt 2 -> fail
         * Attempt 3 -> fail
         * Then the record is recovered/skipped.
         *
         * For a production system, this can later be
         * replaced with a Dead Letter Topic strategy.
         */
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        new FixedBackOff(2000L, 2L)
                );

        factory.setCommonErrorHandler(errorHandler);

        /*
         * Manual acknowledgement is intentionally avoided here.
         * With enable.auto.commit=false and Spring's default
         * RECORD acknowledgement mode, successful listener
         * processing results in offset commit.
         */
        factory.getContainerProperties()
                .setAckMode(
                        org.springframework.kafka.listener.ContainerProperties.AckMode.RECORD
                );

        return factory;
    }
}
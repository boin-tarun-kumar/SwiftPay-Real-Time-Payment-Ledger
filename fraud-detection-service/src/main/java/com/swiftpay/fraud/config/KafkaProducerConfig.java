package com.swiftpay.fraud.config;

import com.swiftpay.common.event.FraudCheckFailedEvent;
import com.swiftpay.common.event.FraudCheckPassedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> fraudProducerFactory() {

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );

        /*
         * Wait for all in-sync replicas.
         *
         * This gives stronger durability than
         * acks=1.
         */
        properties.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        /*
         * Retry transient Kafka failures.
         */
        properties.put(
                ProducerConfig.RETRIES_CONFIG,
                5
        );

        /*
         * Prevent duplicate records during producer retries.
         */
        properties.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        /*
         * Limits the number of unacknowledged requests.
         * Required to maintain ordering with idempotence.
         */
        properties.put(
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                5
        );

        properties.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                120000
        );

        properties.put(
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                30000
        );

        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, Object> fraudKafkaTemplate() {
        return new KafkaTemplate<>(fraudProducerFactory());
    }
}
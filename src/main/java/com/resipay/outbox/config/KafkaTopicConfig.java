package com.resipay.outbox.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String PAYMENT_EVENTS_TOPIC = "resipay.payment-events";

    @Bean
    @ConditionalOnProperty(name = "resipay.kafka.admin.auto-create-topics", havingValue = "true", matchIfMissing = false)
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(PAYMENT_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

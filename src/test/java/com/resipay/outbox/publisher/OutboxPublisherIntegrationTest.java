package com.resipay.outbox.publisher;

import com.resipay.common.AbstractPostgresIntegrationTest;
import com.resipay.outbox.config.KafkaTopicConfig;
import com.resipay.outbox.domain.OutboxEvent;
import com.resipay.outbox.domain.OutboxStatus;
import com.resipay.outbox.repository.OutboxRepository;
import com.resipay.payment.api.dto.CreatePaymentRequest;
import com.resipay.payment.api.dto.PaymentResponse;
import com.resipay.payment.service.PaymentService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 3, topics = {KafkaTopicConfig.PAYMENT_EVENTS_TOPIC})
class OutboxPublisherIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers", "localhost:9092"));
    }

    @Test
    @DisplayName("Outbox publisher selects PENDING events, delivers to Kafka, and marks PUBLISHED")
    void shouldPublishOutboxEventsToKafka() {
        // 1. Create payment (which stages an outbox event atomically)
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .customerId("cust_kafka_pub")
                .amountCents(8800L)
                .currency("USD")
                .idempotencyKey("idem-pub-" + System.currentTimeMillis())
                .build();

        PaymentResponse payment = paymentService.createPayment(request);
        String paymentId = payment.id().toString();

        List<OutboxEvent> staged = outboxRepository.findByAggregateId(paymentId);
        assertThat(staged).hasSize(1);
        assertThat(staged.getFirst().getStatus()).isEqualTo(OutboxStatus.PENDING);

        // 2. Set up test Kafka consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group-" + System.currentTimeMillis(), "true", embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(KafkaTopicConfig.PAYMENT_EVENTS_TOPIC));

            // 3. Trigger outbox publisher relay
            int published = outboxPublisherService.publishPendingEvents(10);
            assertThat(published).isGreaterThanOrEqualTo(1);

            // 4. Verify outbox record transitioned to PUBLISHED
            OutboxEvent updated = outboxRepository.findById(staged.getFirst().getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(updated.getPublishedAt()).isNotNull();

            // 5. Verify event was received on Kafka with matching partition key
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
            assertThat(records).isNotEmpty();

            boolean foundMatchingRecord = false;
            for (ConsumerRecord<String, String> record : records) {
                if (paymentId.equals(record.key())) {
                    foundMatchingRecord = true;
                    assertThat(record.value()).contains("PAYMENT_CREATED");
                    assertThat(record.value()).contains("8800");
                    break;
                }
            }
            assertThat(foundMatchingRecord).isTrue();
        }
    }
}

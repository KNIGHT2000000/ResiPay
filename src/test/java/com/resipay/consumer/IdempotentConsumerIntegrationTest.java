package com.resipay.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resipay.common.AbstractPostgresIntegrationTest;
import com.resipay.consumer.domain.ProcessedEvent;
import com.resipay.consumer.listener.PaymentEventConsumer;
import com.resipay.consumer.repository.ProcessedEventRepository;
import com.resipay.outbox.config.KafkaTopicConfig;
import com.resipay.outbox.event.PaymentCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EmbeddedKafka(partitions = 3, topics = {KafkaTopicConfig.PAYMENT_EVENTS_TOPIC})
class IdempotentConsumerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentEventConsumer consumer;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers", "localhost:9092"));
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Test
    @DisplayName("Kafka consumer processes new event once and safely deduplicates redelivered duplicate")
    void shouldProcessOnceAndDeduplicateRedelivery() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .eventId(eventId)
                .paymentId(paymentId)
                .customerId("cust_consumer_test")
                .amountCents(19900L)
                .currency("USD")
                .idempotencyKey("idem-consumer-" + System.currentTimeMillis())
                .build();

        String payload = objectMapper.writeValueAsString(event);

        int initialProcessed = consumer.getProcessedCount().get();
        int initialSkipped = consumer.getDuplicateSkippedCount().get();

        // 1. First delivery
        kafkaTemplate.send(KafkaTopicConfig.PAYMENT_EVENTS_TOPIC, paymentId.toString(), payload).get(5, TimeUnit.SECONDS);

        // Wait for first delivery to be processed
        await().atMost(10, TimeUnit.SECONDS).until(() ->
                consumer.getProcessedCount().get() >= initialProcessed + 1
        );

        Optional<ProcessedEvent> record = processedEventRepository.findById(eventId);
        assertThat(record).isPresent();
        assertThat(record.get().getAggregateId()).isEqualTo(paymentId.toString());

        // 2. Redelivered duplicate delivery with identical eventId
        kafkaTemplate.send(KafkaTopicConfig.PAYMENT_EVENTS_TOPIC, paymentId.toString(), payload).get(5, TimeUnit.SECONDS);

        // Wait for duplicate to be acknowledged and skipped
        await().atMost(10, TimeUnit.SECONDS).until(() ->
                consumer.getDuplicateSkippedCount().get() >= initialSkipped + 1
        );

        // Verify total processed executions remain strictly 1 (no duplicate side-effects!)
        assertThat(consumer.getProcessedCount().get()).isEqualTo(initialProcessed + 1);
        assertThat(consumer.getDuplicateSkippedCount().get()).isEqualTo(initialSkipped + 1);
    }
}

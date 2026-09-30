package org.example.bookingservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.booking-created:booking-created}")
    private String bookingCreatedTopic;

    public void sendBookingCreatedEvent(String customerEmail) {
        kafkaTemplate.send(bookingCreatedTopic, customerEmail);
    }
}

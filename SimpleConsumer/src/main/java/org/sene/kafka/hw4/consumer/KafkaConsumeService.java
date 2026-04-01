package org.sene.kafka.hw4.consumer;

import org.apache.avro.generic.GenericRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumeService {

    @KafkaListener(topics = {"customers.public.orders", "customers.public.users"}, groupId = "orderAndUserProcessingGroup")
    public void consume(GenericRecord record) {
        System.out.println("Received message: " + record.toString());
    }
}

package com.navercorp.pinpoint.plugin.kafka;

import com.navercorp.pinpoint.bootstrap.plugin.test.ExpectedTrace;
import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifier;
import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifierHolder;
import info.batey.kafka.unit.KafkaUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.pinpoint.plugin.kafka.TestConsumerRecordEntryPoint;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.annotation;
import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.event;

public class KafkaClientITBase {

    static KafkaUnit kafkaUnitServer = null;

    @BeforeClass
    public static void before() throws IOException {
        kafkaUnitServer = new KafkaUnit(2189, 9092);
        kafkaUnitServer.startup();
        kafkaUnitServer.createTopic("pinpoint-plugin-it");
        ProducerRecord<String, String> keyedMessage = new ProducerRecord<>("pinpoint-plugin-it", "key", "yshwang value");
        kafkaUnitServer.sendMessages(keyedMessage);
    }

    @AfterClass
    public static void after() {
        kafkaUnitServer.shutdown();
    }

    @Test
    public void producerSendTest() throws NoSuchMethodException, InterruptedException {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "plugin-test");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        Random random = new Random();
        int messageCount = random.nextInt(5) + 1;
        System.out.println(">> messageCount : " + messageCount);
        for(int i = 0; i < messageCount; i++){
            ProducerRecord<String, String> record = new ProducerRecord<>("pinpoint-plugin-it", "todareistodo");
            producer.send(record);
        }
        producer.flush();
        producer.close();

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.awaitTraceCount(messageCount, 100, 5000);
        System.out.println(">> print cache");
        verifier.printCache();
        System.out.println("print cache >>");

        Method sendMethod = KafkaProducer.class.getDeclaredMethod("send", ProducerRecord.class, Callback.class);

        ExpectedTrace producerSendExpected = event(
                "KAFKA_CLIENT",
                sendMethod,
                null,
                "localhost:9092",
                "localhost:9092",
                annotation("kafka.topic", "pinpoint-plugin-it")
        );

        for (int i = 0; i < messageCount; i++) {
            verifier.verifyDiscreteTrace(producerSendExpected);
        }

        verifier.verifyTraceCount(0);

    }

    @Test
    public void recordEntryPointTest(){
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(() -> startConsumer());
        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.awaitTraceCount(2, 100, 5000);
        verifier.printCache();
    }

    private void startConsumer() {
        Properties consumerConfigs = new Properties();
        consumerConfigs.put("bootstrap.servers", "localhost:9092");
        consumerConfigs.put("session.timeout.ms", "10000");
        consumerConfigs.put("group.id", "test");
        consumerConfigs.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerConfigs.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerConfigs);
        consumer.subscribe(Arrays.asList("pinpoint-plugin-it"));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(2000L);
            for (ConsumerRecord<String, String> record : records) {
                TestConsumerRecordEntryPoint entryPoint = new TestConsumerRecordEntryPoint();
                entryPoint.consumeRecord(record);
            }
        }
    }


}

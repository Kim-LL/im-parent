package com.paas.im.config;

import com.paas.im.codec.KafkaDeserializer;
import com.paas.im.codec.KafkaSerializer;
import com.paas.im.enums.EnvEnum;
import com.paas.im.model.pojo.KafkaConfig;
import com.paas.im.model.pojo.KafkaTransportObject;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import tools.jackson.databind.deser.jdk.StringDeserializer;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
@Configuration
@EnableAsync
public class ServerConfig {

    @Bean
    public ExecutorService executorService(){
        ThreadFactory threadFactory = Thread.ofVirtual().name("im:v:", 0).factory();
        // 每个任务创建一个线程
        return Executors.newThreadPerTaskExecutor(threadFactory);
    }

    @Bean
    public KafkaProducer<String, KafkaTransportObject> kafkaProducer(){
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ZKConfigManager.getInstance().getKafkaConfig().getKafkaServerUrl());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "IM-Task-Producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaSerializer.class.getName());
        // 请求超时时间
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        // 【新增】消息确认级别（默认1，可选0/1/all，all表示所有副本确认，最高可靠性）
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        // 【新增】重试次数（默认Integer.MAX_VALUE，可根据业务调整）
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        KafkaProducer<String, KafkaTransportObject> producer = new KafkaProducer<>(props);
        log.info("init kafka producer!");
        return producer;
    }

    @Bean
    public KafkaConsumer<String, KafkaTransportObject> kafkaConsumer(){
        KafkaConfig kafkaConfig = ZKConfigManager.getInstance().getKafkaConfig();

        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaConfig.getKafkaServerUrl());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "IM-Msg-Consumer-" + EnvEnum.getCurrentEnv().getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, Integer.MAX_VALUE - 1000);

        KafkaConsumer<String, KafkaTransportObject> consumer = new KafkaConsumer<>(props);
        List<String> topicList = List.of(
                kafkaConfig.getChatTopicName(),
                kafkaConfig.getGroupTopicName(),
                kafkaConfig.getTopicRoom(),
                kafkaConfig.getSysMsgTopicName(),
                kafkaConfig.getOtherDataRoomTopicName()
        );
        consumer.subscribe(topicList);
        return consumer;
    }

}

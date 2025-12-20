package com.paas.im.config;

import com.paas.im.codec.KafkaDeserializer;
import com.paas.im.codec.KafkaSerializer;
import com.paas.im.enums.EnvEnum;
import com.paas.im.model.pojo.KafkaConfig;
import com.paas.im.model.pojo.KafkaTransportObject;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import com.paas.im.utils.PathMatcherUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import tools.jackson.databind.deser.jdk.StringDeserializer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE) // 拦截器，优先级最高（数值越小，执行越早）
@Configuration
@EnableAsync
public class ServerConfig implements WebFilter{

    @Value(value = "#{'${web.filter.include-paths:/api/**,/user/**}'.split(',')}")
    private List<String> includePaths;

    @Value(value = "#{'${web.filter.exclude-paths:/**/login,/**/register}'.split(',')}")
    private List<String> excludePaths;

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

    /**
     * 拦截器
     */
    @Override
    public @NonNull Mono<@NonNull Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        // 2. 记录请求开始时间
        LocalDateTime startTime = LocalDateTime.now();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().toString();

        // 放行请求，放行列表 excludePaths
        if(PathMatcherUtils.match(path, excludePaths)){
            return chain.filter(exchange)
                    .doAfterTerminate(()-> logEnd(path, response, startTime))
                    .onErrorResume(e -> handleError(path, e, startTime));
        }

        // 非拦截请求
        if(!PathMatcherUtils.match(path, includePaths)){
            return chain.filter(exchange)
                    .doAfterTerminate(()-> logEnd(path, response, startTime))
                    .onErrorResume(e -> handleError(path, e, startTime));
        }

        // 剩下都是拦截请求，需要自定义拦截逻辑
        return chain.filter(exchange)
                .doAfterTerminate(() -> logEnd(path, response, startTime))
                .onErrorResume(e -> handleError(path, e, startTime));
    }

    // 提取重复的日志逻辑，简化代码
    private void logEnd(String path, ServerHttpResponse response, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = Duration.between(startTime, endTime).toMillis();
        log.info("【请求结束】路径：{}，响应状态码：{}，耗时：{}ms", path, response.getStatusCode(), duration);
    }

    private @NonNull Mono<@NonNull Void> handleError(String path, Throwable e, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = Duration.between(startTime, endTime).toMillis();
        log.error("【请求异常】路径：{}，异常：{}，耗时：{}ms", path, e.getMessage(), duration, e);
        return Mono.error(e);
    }

}

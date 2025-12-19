package com.paas.im.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableAsync
public class ServerConfig {

    @Bean
    public ExecutorService executorService(){
        ThreadFactory threadFactory = Thread.ofVirtual().name("im:v:", 0).factory();
        // 每个任务创建一个线程
        return Executors.newThreadPerTaskExecutor(threadFactory);
    }

}

package com.paas.im;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ChatServer20041 implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ChatServer20041.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("ChatServer20041 -- args: {}", JSON.toJSONString(args));
    }
}

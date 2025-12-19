package com.paas.im;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.paas.im.dao.ChatMsgDAO;
import com.paas.im.model.pojo.KafkaTransportObject;
import com.paas.im.model.proto.MessageBuf;
import com.paas.im.tool.data.DataSourceManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

@Slf4j
@SpringBootApplication
public class RoomServer20061 implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(RoomServer20061.class, args);
    }

    @Resource
    private ExecutorService executorService;

    @Resource
    private KafkaConsumer<String, KafkaTransportObject> consumer;

    @Override
    public void run(String... args) throws Exception {
        log.info("RoomServer20061 -- args: {}", JSON.toJSONString(args));

        // 定时拉取 kafka 消息
        Thread.ofPlatform().start(this::startPollKafka);
    }

    /**
     * 定时拉取 kafka 消息
     */
    private void startPollKafka(){
        for (;;) {
            final ConsumerRecords<String, KafkaTransportObject> records = consumer.poll(Duration.ofMillis(100));
            if(records.isEmpty()){
                continue;
            }
            executorService.execute(()->{
                for(ConsumerRecord<String, KafkaTransportObject> record : records){
                    try {
                        KafkaTransportObject obj = record.value();
                        MessageBuf.IMMessage msg = MessageBuf.IMMessage.parseFrom(obj.getPacket().getBody());
                        if(StrUtil.isEmpty(msg.getContent())){
                            continue;
                        }
                        log.info("partition={}, offset={}, message={}",
                                record.partition(), record.offset(), msg);
                        String appId = msg.getAppId();
                        ChatMsgDAO chatMsgDAO = DataSourceManager.getInstance().getChatMsgDataSourceByAppId(appId);
                        chatMsgDAO.saveMessage(msg);
                    }catch (Exception e){
                        log.error("GroupChatMsgConsumerHandler error:{}", e.getMessage());
                    }
                }
            });
        }
    }
}

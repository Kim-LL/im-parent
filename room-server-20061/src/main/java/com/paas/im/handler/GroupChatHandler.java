package com.paas.im.handler;

import cn.hutool.core.util.RandomUtil;
import com.paas.im.model.pojo.KafkaConfig;
import com.paas.im.model.pojo.KafkaTransportObject;
import com.paas.im.model.proto.Packet;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class GroupChatHandler implements IDataHandler {

    @Resource
    private ExecutorService executorService;

    @Resource
    private KafkaProducer<String, KafkaTransportObject> kafkaProducer;

    private long flagTime = 0;

    @Override
    public void execute(ChannelHandlerContext ctx, Packet packet) {
        executorService.execute(() -> saveMsgToKafka(packet));
    }

    /**
     * 保存群聊消息至 Kafka
     */
    private void saveMsgToKafka(Packet packet) {
        KafkaConfig kafkaConfig = ZKConfigManager.getInstance().getKafkaConfig();
        String groupTopicName = kafkaConfig.getGroupTopicName();
        int partitionNum = kafkaConfig.getPartitionNum();
        int partitionKey = RandomUtil.randomInt(partitionNum);
        long time = System.currentTimeMillis();
        KafkaTransportObject kfObj = new KafkaTransportObject(packet, time);
        try {
            long minutes = time / 3000 / 60;
            ProducerRecord<String, KafkaTransportObject> record = new ProducerRecord<>(groupTopicName, partitionKey, "", kfObj);
            if(minutes == flagTime) {
                kafkaProducer.send(record);
            }else{
                this.flagTime = time;
                kafkaProducer.send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if(exception == null){
                            long waste = System.currentTimeMillis() - time;
                            log.info("group chat msg save kafka success waste={} ms, kafka offset={}, partition={}", waste, metadata.offset(), metadata.partition());
                            if(waste > 5000) {
                                log.error("group chat msg kafka slow msg! waste={}", waste);
                            }
                        }else {
                            log.error("group chat msg 保存Kafka失败, errorType:{}, errorMsg:{} error:{}", exception.getClass().getName(), exception.getMessage(), exception);
                        }
                    }
                });
            }
        }catch (Exception e){
            log.error("group chat msg 保存Kafka失败,errorType:{},errorMsg:{} error:{}", e.getClass().getName(), e.getMessage(), e);
        }
    }
}

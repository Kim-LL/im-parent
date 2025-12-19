package com.paas.im.model.pojo;

import lombok.Data;

/**
 * Kafka配置
 */
@Data
public class KafkaConfig {

    private String zkConnect;

    private String groupIdRoomDiy;

    private String topicRoom;

    private String topicNotify;

    private String roomGroupId;

    /**
     * 房间消息日志统计groupId
     * **/
    private String roomMessageStatisticGroupId;

    private String chatTopicName;

    private String groupTopicName;

    private int partitionNum;

    private int everyServerHasConsumersNum;

    private String kafkaServerUrl;

    //消息发送最大尝试次数
    private String  maxTryTimes;

    private String connectionTimeout;

    private String zkSyncTimeMs;

    private String reconnectInterval;

    //每次尝试增加的额外的间隔时间
    private String retryBackoffMs;


    //消费grpRelation的kafka配置
    private int grpRelationWorkNum;
    private String grpRelationTopic;
    private String grpRelationBootstrapServers;
    private String grpRelationZkServers;

    //推送大批量的消息的系统消息的Topic
    private int sysMsgTopicNum;
    private String sysMsgTopicName;

    //其它数据中心Kafka
    private String otherDataKafkaServer;
    private String otherDataRoomTopicName;
    private String otherDataRoomGroupId;
    private int otherDataRoomPartitionNum;
}

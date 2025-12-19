package com.paas.im.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.paas.im.model.proto.MessageBuf;
import io.micrometer.common.util.StringUtils;

public class MessageUtils {


    private static long lastTimestamp = -1L;

    /**
     * 节点 ID 默认取1
     */
    private static long workerId = 1;

    /**
     * 数据中心的ID 默认取1
     */
    private static long dataCenterId = 1;

    /**
     * 起始纪元
     */
    private static final long idEpoch = 1379314066953L;

    /**
     * 毫秒内自增位
     */
    private static final long sequenceBits = 12L;

    /**
     * 序列id 默认取1
     */
    private static long sequence = 1;

    private static final long sequenceMask = ~(-1L << sequenceBits);

    /**
     * 数据中心标识位数
     */
    private static final long datacenterIdBits = 5L;

    /**
     * 机器标识位数
     */
    private static final long workerIdBits = 5L;

    /**
     * 机器ID偏左移12位
     */
    private static final long workerIdShift = sequenceBits;

    /**
     * 数据中心ID左移17位
     */
    private static final long dataCenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间毫秒左移22位
     */
    private static final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 重构消息
     * 生成消息ID， sequence
     */
    public static MessageBuf.IMMessage convertMessage(MessageBuf.IMMessage message){
        long serverTime = System.currentTimeMillis();
        MessageBuf.IMMessage.Builder msgBuilder = MessageBuf.IMMessage.newBuilder();
        msgBuilder.setAppId(message.getAppId());
        msgBuilder.setFrom(message.getFrom());
        msgBuilder.setTo(message.getTo());
        msgBuilder.setDeviceId(message.getDeviceId());
        msgBuilder.setType(message.getType());
        msgBuilder.setSubType(message.getSubType());
        msgBuilder.setTitle(message.getTitle());

        // 如果原来有MsgId，则保留，如果没有msgId，则生成
        long msgId = message.getMsgId() > 0 ? message.getMsgId() : MessageUtils.getId();

        msgBuilder.setMsgId(msgId);
        msgBuilder.setSequence(SequenceUtils.getNextId());

        msgBuilder.setFlag(message.getFlag());
        msgBuilder.setContent(message.getContent());
        msgBuilder.setClientTime(message.getClientTime());

        // 服务器时间
        msgBuilder.setServerTime(serverTime);

        msgBuilder.setClientMsgId(message.getClientMsgId());
        msgBuilder.setBizStatus(message.getBizStatus());
        msgBuilder.setDeviceType(message.getDeviceType());
        msgBuilder.setMsgUnreadNum(message.getMsgUnreadNum());

        msgBuilder.setRequestId(message.getRequestId());
        msgBuilder.setSaveDB(message.getSaveDB());
        msgBuilder.setPriority(message.getPriority());

        return msgBuilder.build();
    }


    private static synchronized long getId(){
        long timestamp = System.currentTimeMillis();
        if(timestamp < lastTimestamp){
            throw new IllegalStateException("Clock moved backwards.");
        }
        if(lastTimestamp == timestamp){
            sequence = (sequence + 1) & sequence;
            if(sequence == 0){
                timestamp = tilNextMillis(lastTimestamp);
            }
        }else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        // ID偏移组合生成最终的ID，并返回ID
        long id = ((timestamp - idEpoch) << timestampLeftShift) | (dataCenterId << dataCenterIdShift) | (workerId << workerIdShift) | sequence;
        if (workerId == 0)
            workerId = dataCenterId;
        String str = Long.toString(workerId).concat(Long.toString(dataCenterId));
        if (str.length() > 4) {
            str = StrUtil.sub(str, 0, 4);
        } else {
            while (str.length() != 4) {
                String randomStr = RandomUtil.randomNumbers(1);
                str = str.concat(randomStr);
            }
        }
        return Long.parseLong(str.concat(StrUtil.subSuf(Long.toString(id), 7)));
    }

    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}

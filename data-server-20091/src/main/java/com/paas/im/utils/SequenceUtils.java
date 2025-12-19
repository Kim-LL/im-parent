package com.paas.im.utils;

public class SequenceUtils {

    /**
     * 节点 ID 默认取1
     */
    private static long workerId = 1;

    /**
     * 序列id 默认取1
     */
    private static long sequence = 1;

    /**
     * 起始纪元
     */
    private static final long idEpoch = 1379314066953L;

    /**
     * 机器标识位数
     */
    private final static long workerIdBits = 5L;

    /**
     * 数据中心标识位数
     */
    private static final long datacenterIdBits = 5L;

    /**
     * 机器ID最大值
     */
    private static final long maxWorkerId = ~(-1L << workerIdBits);

    /**
     * 数据中心ID最大值
     */
    private static final long maxDatacenterId = ~(-1L << datacenterIdBits);

    /**
     * 毫秒内自增位
     */
    private final static long sequenceBits = 12L;

    /**
     * 机器ID偏左移12位
     */
    private static final long workerIdShift = sequenceBits;

    /**
     * 时间毫秒左移22位
     */
    private static final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    private static final long sequenceMask = ~(-1L << sequenceBits);

    private static long lastTimestamp = -1L;

    /**
     * 按时间生成，保证有序
     */
    public static synchronized long getNextId(){
        long timestamp = System.currentTimeMillis();
        // 时间错误
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards.");
        }
        // 当前毫秒内，则+1
        if (lastTimestamp == timestamp) {
            // 当前毫秒内计数满了，则等待下一秒
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        // ID偏移组合生成最终的ID，并返回ID
        return ((timestamp - idEpoch) << timestampLeftShift)| (workerId << workerIdShift) | sequence;
    }

    /**
     * 等待下一个毫秒的到来
     */
    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();;
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}

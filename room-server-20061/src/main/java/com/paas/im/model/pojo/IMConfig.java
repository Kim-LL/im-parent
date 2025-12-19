package com.paas.im.model.pojo;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 动态配置
 */
@Data
public class IMConfig {

    //白名单信息
    private ConcurrentHashMap<Integer, String> whiteMessageMap = new ConcurrentHashMap<>();

    //低优先级消息
    private ConcurrentHashMap<Integer, String> commonMessageMap = new ConcurrentHashMap<>();

    //特定直播间
    private ConcurrentHashMap<Integer, String> specialRoomMap = new ConcurrentHashMap<>();

    //桶白名单消息
    private ConcurrentHashMap<Integer, String> bucketWhiteMsgMap = new ConcurrentHashMap<>();

    //桶低优先级消息
    private ConcurrentHashMap<Integer, String> bucketCommonMsgMap = new ConcurrentHashMap<>();

    //桶高优先级消息
    private ConcurrentHashMap<Integer, String> bucketHighMsgMap = new ConcurrentHashMap<>();

    //共享用户 appId 列表, 用来快速的索引 数据
    private ConcurrentHashMap<String, String> shareUserAppIdMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Long> appConnectionsThreshold =  new ConcurrentHashMap<>();

    private int webImPort;

    private int imPort;

    private int compressThreshold = 2048;

    //RPC心跳时间
    private int rpcKeepaliveTime;

    //重试次数
    private int rpcRetryTimes;

    //RPC长连接连接池大小
    private int rpcPoolSize;

    //WEBIM设备前缀
    private String deviceIdPrev;

    //桶数目
    private int roomBucketNum;

    //消息发送重试次数
    private int sendMsgRetryTimes;

    //消息重发等待时间间隔
    private int sendMsgWaitTime;

    //消息推送方式:true 为推, false 为拉
    private AtomicBoolean isPushMessage;

    //是否拉取离线消息(断网使用)
    private AtomicBoolean isPullOfflineMsg;

    //是否拉取直播间历史消息
    private AtomicBoolean isPullRoomHisMsg;

    // tob 的 push 地址
    private String tobPushUrl;

    //白名单消息拉取条数
    private int gatewayTimeout;

    //白名单消息拉取条数
    private int pullLimit0;

    //高优先级单消息拉取条数
    private int pullLimit1;

    //低优先级消息拉取条数
    private int pullLimit2;

    //单聊队列开启标识
    private boolean chatQueueAccess = false;

    //群聊队列开启标识
    private boolean groupChatQueueAccess = false;

    //系统消息队列开启的标志
    private boolean systemQueueAccess = false;

    //GroupChat包含Client uuid的情况下
    private boolean groupChatPushApns = false;

    //直播间PV/UV日志记录
    private boolean closeRoomUvLog;

    //批量发送消息条数
    private int batchSendLimit;

    //IM服务是否开启限流
    private boolean imRateLimiter;

    //直播间消息限流速率
    private double rateLimiterNum;

    //白名单消息限流速率
    private double rateLimiterNumWhite;

    //高优先级消息限流速率
    private double rateLimiterNumHigh;

    //低优先级消息限流速率
    private double rateLimiterNumCommon;

    //白名单消息限流速率
    private double specialRateLimiterNumWhite;

    //批量发送开关，如果关闭则走远直播间逻辑
    private boolean batchSend = true;

    //特定高优先级消息限流速率
    private double specialRateLimiterNumHigh;

    //特定低优先级消息限流速率
    private double specialRateLimiterNumCommon;

    //IM服务是否开启限流
    private AtomicBoolean singleRoomRouter;

    //关闭消息push日志，一分钟显示一次
    private AtomicBoolean closeMsgLog;

    //查看是否判断缓存区可写状态
    private AtomicBoolean channelWritable;

    //白名单消息
    private String whiteMessage;

    //高优先级消息
    private String highMessage;

    //低优先级消息
    private String commonMessage;

    //桶白名单消息
    private String bucketWhiteMsg;

    //桶高优先级消息
    private String bucketHighMsg;

    //桶低优先级消息
    private String bucketCommonMsg;

    //在线的时候是否推送离线消息
    private boolean sendIgnoreOnline;

    //活动线程警告阈值
    private int activeThreadWarning = 500;

    //群成员列表 redis 超时时间 默认为30  单位为秒
    private int groupHasUserTimeout = 60;

    //直播间禁言用户数目阈值
    private long roomUserCountThreshold = 500;

    //http限流阈值控制 默认：不限流
    private Integer httpLimitCount = -1;

    //特定直播间
    private String specialRoom;

    //客服人员
    private String vips;

    //直播间历史消息abTest
    private String abTestHisMsg;

    //历史消息时延
    private int hisMsgDelay;

    //直播间是否开启限流
    private boolean roomRateLimiter;

    //直播回看消息过期时间：2小时
    private int lookBackLogExpireSeconds;

    //直播间消息是否经过Kafka
    private boolean roomMsgUseKafka;

    //直播开启禁言模式
    private boolean roomMsgUseGag = false;

    //直播间开启拉黑模式
    private boolean roomMsgUseBlack = false;

    //是否刷新连接的配置
    private int refreshConfig;

    //S3生成短连接的生效的时间,默认的情况下为1小时
    private long overdue = 1000 * 60 * 60;

    private boolean findCountCodeByIp;

    //消息是否加密
    private boolean messageEncrypt;

    //数美敏感词地址
    private String shuMeiURL;

    //单聊敏感词开关
    private boolean shuMeiWordChat;

    //群聊敏感词开关
    private boolean shuMeiWordGroupChat;

    //直播间敏感词开关
    private boolean shuMeiWordRoom;

    //不打印 Error 日志
    private boolean printError;

    //系统消息 redis 保留时长
    private int systemMsgSaveDays;

    //聊天消息redis保留时长
    private int chatMsgSaveDays;

    //接口的盐值
    private String salt;

    //gateway长连接数据是否可用开关 .  1:可用, 0:不可用
    //gateway=1, gatewayHost/gatewayPort为null,则使用第一条连接传输数据
    private String gatewayEnable;

    private int connectionsLimit = -1;

    /**
     * 白名单消息 MAP
     * 限流用
     */
    public ConcurrentHashMap<Integer, String> getWhiteMessageMap() {
        if(!whiteMessageMap.isEmpty()){
            return whiteMessageMap;
        }
        if(StrUtil.isEmpty(whiteMessage)){
            return whiteMessageMap;
        }
        String[] diyTypeArr = whiteMessage.split(",");
        for(String diyTypeStr: diyTypeArr){
            Integer diyType = Integer.valueOf(diyTypeStr);
            whiteMessageMap.put(diyType, "");
        }
        return whiteMessageMap;
    }

    /**
     * 低优先级消息 MAP
     */
    public ConcurrentHashMap<Integer, String> getCommonMessageMap() {
        if(!commonMessageMap.isEmpty()){
            return commonMessageMap;
        }
        if(StrUtil.isEmpty(commonMessage)){
            return commonMessageMap;
        }
        String[] diyTypeArr = commonMessage.split(",");
        for(String diyTypeStr: diyTypeArr){
            Integer diyType = Integer.valueOf(diyTypeStr);
            commonMessageMap.put(diyType, "");
        }
        return commonMessageMap;
    }

    /**
     * 桶白名单消息
     * 直播间历史消息用
     */
    public ConcurrentHashMap<Integer, String> getBucketWhiteMsgMap() {
        if(!bucketWhiteMsgMap.isEmpty()){
            return bucketWhiteMsgMap;
        }
        if(StrUtil.isEmpty(bucketWhiteMsg)){
            return bucketWhiteMsgMap;
        }
        String[] diyTypeArr = bucketWhiteMsg.split(",");
        for(String diyTypeStr: diyTypeArr){
            Integer diyType = Integer.valueOf(diyTypeStr);
            bucketWhiteMsgMap.put(diyType, "");
        }
        return bucketWhiteMsgMap;
    }

    /**
     * 桶高优先消息
     * 直播间历史消息用
     */
    public ConcurrentHashMap<Integer, String> getBucketHighMsgMap() {
        if(!bucketHighMsgMap.isEmpty()){
            return bucketHighMsgMap;
        }
        if(StrUtil.isEmpty(bucketHighMsg)){
            return bucketHighMsgMap;
        }
        String[] diyTypeArr = bucketHighMsg.split(",");
        for(String diyTypeStr: diyTypeArr){
            Integer diyType = Integer.valueOf(diyTypeStr);
            bucketHighMsgMap.put(diyType, "");
        }
        return bucketHighMsgMap;
    }

    /**
     * 桶低优先消息
     * 直播间历史消息用
     */
    public ConcurrentHashMap<Integer, String> getBucketCommonMsgMap() {
        if(!bucketCommonMsgMap.isEmpty()){
            return bucketCommonMsgMap;
        }
        if(StrUtil.isEmpty(bucketCommonMsg)){
            return bucketCommonMsgMap;
        }
        String[] diyTypeArr = bucketCommonMsg.split(",");
        for(String diyTypeStr: diyTypeArr){
            Integer diyType = Integer.valueOf(diyTypeStr);
            bucketCommonMsgMap.put(diyType, "");
        }
        return bucketCommonMsgMap;
    }

    /**
     * 特殊直播间MAP
     */
    public ConcurrentHashMap<Integer, String> getSpecialRoomMap() {
        if(!specialRoomMap.isEmpty()){
            return specialRoomMap;
        }
        if(StrUtil.isEmpty(specialRoom)){
            return specialRoomMap;
        }
        String[] diyTypeArr = specialRoom.split(",");
        for(String diyTypeStr: diyTypeArr){
            Integer diyType = Integer.valueOf(diyTypeStr);
            specialRoomMap.put(diyType, "");
        }
        return specialRoomMap;
    }
}

package com.paas.im.constant;

public class Constants {

    // 缓存分隔符
    public static String SEQ = "__";

    public final static int WEB_IM_RPC_PORT = 10021;
    public final static int CHAT_RPC_PORT = 10041;
    public final static int GROUP_RPC_PORT = 10051;
    public final static int ROOM_RPC_PORT = 10061;
    public final static int API_RPC_PORT = 10071;
    public final static int PUSH_RPC_PORT = 10081;
    public final static int TASK_RPC_PORT = 10081;
    public final static int DATA_RPC_PORT = 10100;

    // 在线用户(hash)前缀
    public static String CONN_PREFIX = "conn:";

    // 保存消息前缀
    public static final String IM_MSG_PREFIX = "im:msg:";

    // 服务节点
    public static final String WEB_IM_CLUSTER = "/im/web/cluster";
    public static final String CHAT_CLUSTER = "/chat/cluster";
    public static final String GROUP_CHAT_CLUSTER = "/group/chat/cluster";
    public static final String ROOM_CLUSTER = "/room/cluster";
    public static final String DATA_CLUSTER = "/data/cluster";
    public static final String PUSH_CLUSTER = "/push/cluster";
    public static final String API_CLUSTER = "/api/cluster";
    public static final String TASK_CLUSTER = "/task/cluster";

    public static String getUnreadMsgPrefix(String to, String appId) {
        // 未读消息(zset)
        return "im:unread:msg:" + to + ":" + appId;
    }
}

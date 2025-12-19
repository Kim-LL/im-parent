package com.paas.im.tool.channel;

import io.netty.util.AttributeKey;

/**
 * Netty常量
 */
public interface AttributeKeys {

    /**
     * 用户名称
     */
    AttributeKey<String> USER_ID = AttributeKey.valueOf("userId");

    /**
     * 用户登录设备DeviceId + ChannelId
     */
    AttributeKey<String> DEVICE_ID = AttributeKey.valueOf("deviceChannelId");

    /**
     * 连接池连接标记key
     */
    AttributeKey<String> RPC_POOL_KEY = AttributeKey.valueOf("RPCPoolKey");

    /**
     * RPC服务器IP
     */
    AttributeKey<String> RPC_SERVER = AttributeKey.valueOf("RPCServer");

    /**
     * RPC Port
     */
    AttributeKey<Integer> RPC_PORT = AttributeKey.valueOf("RPCPort");

    /**
     * RPC连接编号
     */
    AttributeKey<Integer> RPC_INDEX = AttributeKey.valueOf("RPCIndex");
}

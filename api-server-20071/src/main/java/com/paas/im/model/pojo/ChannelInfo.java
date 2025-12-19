package com.paas.im.model.pojo;

import lombok.Data;

/**
 * 用户连接设备信息
 * (缓存结构如下,返回用户deviceId/Ip数据列表)
 *  +--------------+------------+---------+
 *  |              |  deviceId1 |   Ip1   |
 *  +              |------------+---------+
 *  | userId_appId |  deviceId2 |   Ip1   |
 *  +              |------------+---------+
 *  |              |  deviceId3 |   Ip3   |
 *  +--------------+------------+---------+
 */
@Data
public class ChannelInfo {

    // 当前用户登陆的设备id
    private String deviceId;

    // 当前用户连接的哪个IM服务地址
    private String ip;

    // 当前用户 userId
    private String userId;

    // 当前用户的 appId
    private String appId;
}

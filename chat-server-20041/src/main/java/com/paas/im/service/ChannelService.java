package com.paas.im.service;

import com.paas.im.model.pojo.ChannelInfo;

import java.util.List;
import java.util.Map;

/**
 * 用户连接状态
 */
public interface ChannelService {

    /**
     *  返回用户登录IM DeviceId/IP列表
     */
    List<ChannelInfo> getUserChannelList(String userId, String appId);

    /**
     * 返回deviceId相关的设备信息
     */
    List<ChannelInfo> getUserChannelList(String userId, String appId, String deviceId);

    /**
     * 保存用户信息
     */
    void saveUserImIp(String userId, String appId, String deviceId, String channelId, String ip);

    /**
     * 删除用户 channel
     */
    void deleteUserChannel(String userId, String appId, String deviceId, String ip);

    /**
     * 取得用户登录IM IP列表
     */
    Map<String, String> getUserImIpList(String userId, String appId);
}

package com.paas.im.service;

import com.paas.im.model.pojo.BaseResult;

import java.util.Map;

public interface ApiService {

    /**
     * http 登陆
     */
    String login(String clientIP, String countryCode, String version, String versionDetail, Map<String, String> params);

    /**
     * 获取登陆token
     */
    String getToken(String userId, String clientIP, String signature, String appKey, String timestamp, String nonce);

    /**
     * web 登陆
     */
    String webLogin(String clientIP, String countryCode, String version, String versionDetail, Map<String, String> params);

    /**
     *
     */
    BaseResult<?> pushMsg(String from, String to, String content, String appId, String deviceId, Integer deviceType, Integer type, Integer subType, String title, String diyTypeName, Integer diyType, Long clientMsgId, String pushData, String requestId);


}

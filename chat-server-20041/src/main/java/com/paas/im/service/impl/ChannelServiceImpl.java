package com.paas.im.service.impl;

import com.paas.im.constant.Constants;
import com.paas.im.model.pojo.ChannelInfo;
import com.paas.im.service.ChannelService;
import io.netty.util.internal.StringUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChannelServiceImpl implements ChannelService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ChannelInfo> getUserChannelList(String userId, String appId) {
        return this.getUserChannelList(userId, appId, null);
    }

    @Override
    public List<ChannelInfo> getUserChannelList(String userId, String appId, String deviceId) {
        List<ChannelInfo> ipList = new ArrayList<>();
        Map<String, String> imIpMap = this.getUserImIpList(userId, appId);
        Iterator<String> it = imIpMap.keySet().iterator();
        ChannelInfo ci = null;
        while (it.hasNext()) {
            ci = new ChannelInfo();
            //${deviceId}__${channelId}
            String key = it.next();
            if(deviceId != null && !key.startsWith(deviceId)){
                // 有可能不同应用之间的 userId 是相同的，要过滤出来
                continue;
            }
            String value = imIpMap.get(key);
            ci.setDeviceId(deviceId);
            ci.setIp(value);
            ci.setUserId(userId);
            ci.setAppId(appId);
            ipList.add(ci);
        }
        return ipList;
    }

    @Override
    public void saveUserImIp(String userId, String appId, String deviceId, String channelId, String ip) {
        Map<String, String> ipMap = new HashMap<>();
        if(StringUtil.isNullOrEmpty(channelId)){
            //HashMap key 为 ${deviceId}
            ipMap.put(deviceId, ip);
        } else {
            //HashMap key 为 ${deviceId}__${channelId}
            ipMap.put(deviceId + Constants.SEQ + channelId, ip);
        }
        log.info("保存用户信息:addUserImIP userId:{}, appId:{}, deviceId:{}, ip:{}", userId, appId, deviceId, ip);
        // conn:{userId}:{appId}
        String key = Constants.CONN_PREFIX + userId + ":" + appId;
        stringRedisTemplate.opsForHash().putAll(key, ipMap);
    }

    @Override
    public void deleteUserChannel(String userId, String appId, String deviceId, String ip) {

    }

    /**
     * 返回用户的登录ip信息 Map<String, String>
     * key: ${deviceId}__${channelId}
     * value: ip
     */
    @Override
    public Map<String, String> getUserImIpList(String userId, String appId) {
        // conn:${userid}:${appId}
        String key = Constants.CONN_PREFIX + userId + ":" + appId;
        Map<Object, Object> rawMap = stringRedisTemplate.opsForHash().entries(key);
        return rawMap.entrySet().stream()
                .collect(Collectors.toMap(
                        // 键：Object -> String（强转，因为StringRedisTemplate存储的是String）
                        entry -> (String) entry.getKey(),
                        // 值：Object -> String（同理）
                        entry -> (String) entry.getValue(),
                        // 解决键重复的冲突策略（Redis哈希字段唯一，实际不会触发）
                        (_, newValue) -> newValue,
                        // 指定返回HashMap（可选，默认是LinkedHashMap）
                        HashMap::new
                ));
    }
}

package com.paas.im.service.impl;

import com.paas.im.constant.Constants;
import com.paas.im.model.proto.MessageBuf;
import com.paas.im.service.ChatService;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Set<String> getServers(String cluster) {
        return Set.of();
    }

    @Override
    public boolean saveMessage(MessageBuf.IMMessage message) {

        int chatMsgSaveDays = ZKConfigManager.getInstance().getImConfig().getChatMsgSaveDays();
        int systemMsgSaveDays = ZKConfigManager.getInstance().getImConfig().getSystemMsgSaveDays();
        long expireSeconds =  24 * 60 * 60L;
        String key = Constants.IM_MSG_PREFIX + message.getRequestId();
        if(message.getSaveDB()){
            //即时消息保存5秒
            try {
                redisTemplate.opsForValue().set(key, message.toByteArray(), 5, TimeUnit.SECONDS);
                return true;
            } catch (Exception e){
                log.error("saveMessage error: {}", e.getMessage());
            }
            return false;
        } else {
            if(message.getIsSystemMsg()){
                // 系统消息保存 目前7天
                redisTemplate.opsForValue().set(key, message.toByteArray(), systemMsgSaveDays * expireSeconds, TimeUnit.SECONDS);
            }else{
                // 私信消息保存 目前7天
                redisTemplate.opsForValue().set(key, message.toByteArray(), chatMsgSaveDays * expireSeconds, TimeUnit.SECONDS);
            }
        }
        // 保存用户未读消息
        String unreadMsgPrefix = Constants.getUnreadMsgPrefix(message.getTo(), message.getAppId());
        double score = message.getSequence();
        String val = String.valueOf(message.getMsgId());
        // redis 只存 msgId
        return stringRedisTemplate.opsForZSet().add(unreadMsgPrefix, val, score);
    }
}

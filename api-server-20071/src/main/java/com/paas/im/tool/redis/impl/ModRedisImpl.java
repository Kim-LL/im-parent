package com.paas.im.tool.redis.impl;

import com.paas.im.tool.redis.IRedisCache;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ModRedisImpl implements IRedisCache {

    @Override
    public String multiSet(String key, Map<String, String> hash) {
        return "";
    }
}

package com.paas.im.model.pojo;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态配置
 */
@Data
public class TCPConfig {

    //tcp限流阈值 默认放行
    private int tcpThresholdCount = -1;

    //tcp限流 时间阈值 单位s 默认1s
    private Long tcpThresholdTime = 1L;

    //tcp限流开关 默认不开启 流量放行
    private boolean tcpRateLimitOnOff = false;;

    //http限流阈值控制 默认：不限流
    private Integer httpLimitCount = -1;

    //http限流 时间阈值 单位s 默认1s
    private Long httpThresholdTime = 1L;

    //http限流开关 默认不开启 流量放行
    private boolean httpRateLimitOnOff = false;

    //tcp限流配置
    private ConcurrentHashMap<String, Integer> tcpLimitConfig = new ConcurrentHashMap<String, Integer>();

    //http限流配置,配置appid的按配置走，没配的走默认
    private ConcurrentHashMap<String, HttpLimitConfig> httpLimitConfig = new ConcurrentHashMap<>();

}

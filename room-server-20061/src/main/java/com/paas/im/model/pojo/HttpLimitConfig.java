package com.paas.im.model.pojo;

import lombok.Data;

@Data
public class HttpLimitConfig {

    private int httpLimitCount;

    private long httpThresholdTime;

    private boolean httpRateLimitOnOff;
}

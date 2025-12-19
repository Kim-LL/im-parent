package com.paas.im.model.pojo;

import com.paas.im.tool.country.Router;
import lombok.Data;

@Data
public class AreaRouterConfig {

    private String scanPort;
    private String connectTimeout;
    private String retryMaxTimes;
    private Router[] router;
}

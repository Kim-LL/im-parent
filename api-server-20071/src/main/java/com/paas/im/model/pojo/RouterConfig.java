package com.paas.im.model.pojo;

import com.paas.im.tool.country.Router;
import lombok.Data;

@Data
public class RouterConfig {

    private Router[] router;
    private String routerRingMinInterval;
    private String routerRingMaxInterval;
    private String routerRingRetryInterval;
    private String connRetryInterval;
    private ConfigRefreshInterval configRefreshInterval;
    private Heartbeat[] heartbeats;
    private CountriesConfig countriesConfig;
    private int scanTimeMax;
}

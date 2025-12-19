package com.paas.im.tool.country;

import lombok.Data;

import java.util.List;

@Data
public class Router {

    private int port;
    private String scanPort;
    private String host;
    private List<String> randomIps;
    private String httpDnsUrl;
    private String httpDnsType;
    private String httpDnsName;
    private String connectTimeout;
    private String retryMaxTimes;
    private String parseTimeout;
    private String parseTTL;
    private int type;
    private int routerNum;
    private String name;
}

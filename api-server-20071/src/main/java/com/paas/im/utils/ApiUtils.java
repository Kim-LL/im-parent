package com.paas.im.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.paas.im.tool.country.Router;
import com.paas.im.tool.zookeeper.ZKConfigManager;

public class ApiUtils {

    public static JSONObject convert(Router object, String clientIP) {
        if (object != null) {
            JSONObject router = new JSONObject();
            router.put("retryMaxTimes", object.getRetryMaxTimes());
            router.put("scanPort", object.getScanPort());
            router.put("port", object.getPort());
            router.put("host", object.getHost());
            boolean gatewayHostPortEnable = ZKConfigManager.getInstance().isGatewayHostPortEnable();
            if(gatewayHostPortEnable) {
                router.put("gatewayPort", object.getPort());
                router.put("gatewayHost", object.getHost());
            }
            router.put("connectTimeout", object.getConnectTimeout());
            String hds = object.getHttpDnsUrl();

            if (StrUtil.isNotEmpty(hds) && StrUtil.isNotEmpty(clientIP)) {
                hds = hds + "&ip=" + clientIP;
                router.put("httpDnsUrl", hds);
            }
            if (StrUtil.isNotEmpty(object.getHttpDnsName())) {
                router.put("httpDnsName", object.getHttpDnsName());
            }
            if (StrUtil.isNotBlank(object.getHttpDnsType())) {
                router.put("httpDnsType", object.getHttpDnsType());
            }
            if (StrUtil.isNotBlank(object.getParseTimeout())) {
                router.put("parseTimeout", object.getParseTimeout());
            }
            if (StrUtil.isNotBlank(object.getParseTTL())) {
                router.put("parseTTL", object.getParseTTL());
            }

            if (StrUtil.isNotBlank(object.getName())) {
                router.put("name", object.getName());
            }
            return router;
        }
        return null;
    }
}

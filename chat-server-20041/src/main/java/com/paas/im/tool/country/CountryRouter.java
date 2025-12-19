package com.paas.im.tool.country;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.paas.im.model.pojo.AreaRouterConfig;
import com.paas.im.model.pojo.CountryRouterConfig;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountryRouter {

    @Getter
    private CountryRouterConfig[] countryRouterConfigs;

    //根据国家，寻找对应的地区
    private final Map<String, String> routers = new HashMap<>();

    //该地区对应的路由节点
    @Getter
    private final Map<String, List<Router>> areaRouters = new HashMap<>();

    public Map<String, String> getRouters() {
        if(!routers.isEmpty()){
            return routers;
        }
        if(countryRouterConfigs == null){
            return routers;
        }
        for(CountryRouterConfig crc : countryRouterConfigs){
            String key = crc.getCountryCode();
            String area = crc.getCountryArea();
            String[] detail = area.split("[,]");
            for (String d : detail) {
                this.routers.put(d, key);
            }
        }
        return routers;
    }

    public void init(){
        if(countryRouterConfigs == null){
            return;
        }
        for(CountryRouterConfig crc : countryRouterConfigs){
            String key = crc.getCountryCode();
            String area = crc.getCountryArea();
            String[] detail = area.split("[,]");
            for (String d : detail) {
                this.routers.put(d, key);
            }
        }
    }

    public void initCountryRouters(String countryCode, String value){
        List<Router> routerList = this.areaRouters.getOrDefault(countryCode, new ArrayList<>());
        routerList.clear();

        JSONObject job = JSONObject.parseObject(value);
        if(job == null){
            this.areaRouters.put(countryCode, routerList);
            return;
        }
        String routerConfigContent = job.getString("routerConfig");
        AreaRouterConfig areaRouterConfig = JSON.parseObject(routerConfigContent, AreaRouterConfig.class);
        Router[] tmp = areaRouterConfig.getRouter();
        if(tmp != null){
            for(Router r : tmp){
                if(StrUtil.isEmpty(r.getScanPort())){
                    r.setScanPort(areaRouterConfig.getScanPort());
                }
                if(StrUtil.isEmpty(r.getConnectTimeout())){
                    r.setConnectTimeout(areaRouterConfig.getConnectTimeout());
                }
                if(StrUtil.isEmpty(r.getRetryMaxTimes())){
                    r.setRetryMaxTimes(areaRouterConfig.getRetryMaxTimes());
                }
                routerList.add(r);
            }
        }
        this.areaRouters.put(countryCode, routerList);
    }
}

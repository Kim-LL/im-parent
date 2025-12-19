package com.paas.im.model.pojo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 屏蔽消息
 */
@Slf4j
@Data
public class ScreenOutMessage {

    /**
     * 屏蔽设备类型
     *
     */
    private List<String> screenOutAppIds;

    /**
     * 某渠道屏蔽的消息类型
     */
    private Map<String,List<Integer>> screenOutMessageTypeOfAppIds;

    /**
     * 把 screenOutMessageTypeOfAppIds 中的 集合转成  Map 方便数据匹配
     */
    private Map<String, ConcurrentHashMap<Integer, String>> screenOutMessageTypeOfAppIdsContent;

    public void setScreenOutMessageTypeOfAppIds(Map<String, List<Integer>> screenOutMessageTypeOfAppIds) {
        this.screenOutMessageTypeOfAppIds = screenOutMessageTypeOfAppIds;
        this.screenOutMessageTypeOfAppIdsContent = new ConcurrentHashMap<>();
        if(null != screenOutMessageTypeOfAppIds) {
            for(String appid: screenOutMessageTypeOfAppIds.keySet()) {
                ConcurrentHashMap<Integer,String> screenOutMessageTypeMap= new ConcurrentHashMap<>();
                this.screenOutMessageTypeOfAppIdsContent.put(appid,screenOutMessageTypeMap);
                for(Integer screenOutMessageType: screenOutMessageTypeOfAppIds.get(appid)) {
                    screenOutMessageTypeMap.put(screenOutMessageType, screenOutMessageType.toString());
                    log.info("set screenOutMessage appid: {},messageType: {}", appid, screenOutMessageType);
                }
            }
        }else {
            log.warn("未配置，屏蔽appid");
        }
    }
}

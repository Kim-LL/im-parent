package com.paas.im.tool.zookeeper;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.zkclient.IZkDataListener;
import com.paas.im.model.pojo.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class ZKConfigManager {

    public static final String CONFIG_ZK_PATH = "/config";

    public static final String CONFIG_ZK_PATH_TCP = "/config/tcpConfig";

    @Getter
    private KafkaConfig kafkaConfig = null;

    @Getter
    private IMConfig imConfig = null;

    @Getter
    private TCPConfig tcpConfig = new TCPConfig();

    @Getter
    private TokenServiceConfig tokenServiceConfig = null;

    @Getter
    private ChatMillionConfig chatMillionConfig = null;

    @Getter
    private GroupChatPushConfig groupChatPushConfig = null;

    private final ZKHelp zk = ZKHelp.getInstance();

    @Getter
    private final ScreenOutMessage screenOutMessage = new ScreenOutMessage();

    private static class InstanceHolder {
        private static final ZKConfigManager INSTANCE = new ZKConfigManager();
    }

    public static ZKConfigManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void updateConfig(String configZKPath, String value){
        zk.setPathData(configZKPath, value);
    }

    private ZKConfigManager() {
        /**
         * zookeeper 节点内容发生变化时 hook
         */
        // read KafkaConfig
        // read IMConfig
        //添加按appid类型的消息统计,和日志消息输出
        /**
         * zookeeper 节点被删除时 hook
         */
        IZkDataListener listenerGlobal = new IZkDataListener() {

            /**
             * zookeeper 节点内容发生变化时 hook
             */
            @Override
            public void handleDataChange(String dataPath, byte[] data) throws Exception {
                log.info("!!! configZkPath node data has been changed !!!: {}", dataPath);
                String rtData = null;
                if (data != null && data.length > 0) {
                    rtData = new String(data, StandardCharsets.UTF_8);

                    JSONObject json = JSONObject.parseObject(rtData);

                    // read KafkaConfig
                    kafkaConfig = json.getObject("kafkaConfig", KafkaConfig.class);

                    // read IMConfig
                    imConfig = json.getObject("imConfig", IMConfig.class);

                    chatMillionConfig = json.getObject("chatMillionConfig", ChatMillionConfig.class);

                    tokenServiceConfig = json.getObject("tokenServiceConfig", TokenServiceConfig.class);

                    groupChatPushConfig = json.getObject("groupChatPushConfig", GroupChatPushConfig.class);

                    //添加按appid类型的消息统计,和日志消息输出
                    if (json.containsKey("StatisticMessageConfig")) {
                        JSONObject statisticMessageConfigJsonObject = json.getJSONObject("StatisticMessageConfig");
                        if (statisticMessageConfigJsonObject.containsKey("isOpenAppMessageLogWrite")) {
                            StatisticMessageConfig.setIsOpenAppMessageLogWrite(statisticMessageConfigJsonObject.getBoolean("isOpenAppMessageLogWirte"));
                        }
                    }
                }
                log.info("!!! configZkPath node data has been changed ok !!!{}, kafkaConfig=[{}]", rtData, kafkaConfig);
            }

            /**
             * zookeeper 节点被删除时 hook
             */
            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                log.info("!!! configZkPath node dataPath has been delete !!!{}, kafkaConfig=[{}]", dataPath, kafkaConfig);
            }
        };

        IZkDataListener listenerGlobalTCP = new IZkDataListener() {
            @Override
            public void handleDataChange(String dataPath, byte[] data) throws Exception {
                log.info("!!! configZkPath node data has been changed !!!" + dataPath);
                String rtdata = null;
                if (data != null && data.length > 0) {
                    rtdata = new String(data, StandardCharsets.UTF_8);
                    if (StrUtil.isNotEmpty(rtdata)) {
                        tcpConfig = JSON.parseObject(rtdata, TCPConfig.class);
                    }
                }
                log.info("!!! configZkPath node data has been changed ok !!!{}, tcpConfig=[{}]", rtdata, JSONObject.toJSONString(tcpConfig));
            }

            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                log.info("!!! configZkPath node dataPath has been delete !!!{}, tcpConfig=[{}]", dataPath, tcpConfig);
            }
        };

        //添加节点监控
        zk.subscribeDataChange(CONFIG_ZK_PATH, listenerGlobal);
        zk.subscribeDataChange(CONFIG_ZK_PATH_TCP, listenerGlobalTCP);

        try {
            String rtData = zk.getValue(CONFIG_ZK_PATH);
            JSONObject json = JSON.parseObject(rtData);

            // read KafkaConfig
            kafkaConfig = json.getObject("kafkaConfig", KafkaConfig.class);

            // read imConfig
            imConfig = json.getObject("imConfig", IMConfig.class);

            // read tcpConfig
            String tcpConfigStr = zk.getValue(CONFIG_ZK_PATH_TCP);
            tcpConfig = JSON.parseObject(tcpConfigStr, TCPConfig.class);

            // read tokenServiceConfig
            tokenServiceConfig = json.getObject("tokenServiceConfig", TokenServiceConfig.class);

            groupChatPushConfig = json.getObject("groupChatPushConfig", GroupChatPushConfig.class);

            //添加按 appId 类型的消息统计和日志消息输出
            if(json.containsKey("StatisticMessageConfig")){
                JSONObject statisticMessageConfigJsonObject = json.getJSONObject("StatisticMessageConfig");
                // 是否开启消息日志输出
                if(statisticMessageConfigJsonObject.containsKey("isOpenAppMessageLogWrite")){
                    StatisticMessageConfig.setIsOpenAppMessageLogWrite(statisticMessageConfigJsonObject.getBoolean("isOpenAppMessageLogWrite"));
                }
            }
        }catch (Exception e){
            log.error("get value error!", e);
        }
        log.info("===================init ZKConfigManager ok================");
    }
}

package com.paas.im.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.paas.im.model.pojo.BaseResult;
import com.paas.im.model.pojo.ConfigRefreshInterval;
import com.paas.im.model.pojo.Heartbeat;
import com.paas.im.model.vo.ResultVO;
import com.paas.im.service.ApiService;
import com.paas.im.service.RPCService;
import com.paas.im.tool.cluster.ClusterManager;
import com.paas.im.tool.country.CountryRouter;
import com.paas.im.tool.country.Router;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import com.paas.im.utils.ApiUtils;
import com.paas.im.utils.IpUtils;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ApiServiceImpl implements ApiService {

    private static final String defaultKey = "default";

    private static final String findTencentCDNHostIP = "http://169.47.30.16:5380/?dn=penguinracing.tencent-cloud.com&client=";

    private static final int defaultPort = 443;

    @Resource
    private RPCService rpcService;

    @Override
    public String login(String clientIP, String countryCode, String version, String versionDetail, Map<String, String> params) {
        try {
            JSONObject json = new JSONObject();
            json.put("serverTime", String.valueOf(System.currentTimeMillis()));
            json.put("clientIP", clientIP);
            String appKey = params.get("appKey");
            if(StrUtil.isNotEmpty(appKey)){
                json.put("appKey", appKey);
            }
            JSONArray routerArray = new JSONArray();

            //确定该此调用，属于哪一个国家
            boolean findCountryCodeByIP = ZKConfigManager.getInstance().getImConfig().isFindCountCodeByIP();
            if(!findCountryCodeByIP && StrUtil.isNotBlank(clientIP)){
                // 通过 IP 判断真正的 countryCode
                String countryCodeTmp = IpUtils.getCountryCodeByIP(clientIP);
                if(StrUtil.isEmpty(countryCodeTmp) || !countryCode.equals(countryCodeTmp)){
                    countryCode = countryCodeTmp;
                }
            }
            log.info("clientIP: {}, countryCode: {}", clientIP, countryCode);

            //根据国家码确定router
            CountryRouter countryRouter = ZKConfigManager.getInstance().getCountryRouter();
            //获取直链IP
            String directLinkHost = ClusterManager.getInstance().getPublicGatewayCluster(System.currentTimeMillis());
            List<Router> countryRouters = dealCountryRouters(countryRouter, directLinkHost, countryCode);
            if(CollectionUtil.isEmpty(countryRouters)){
                countryRouters = dealDefaultCountryRouters(countryRouter);
            }

            //特殊路由处理
            for (Router r: countryRouters) {
                //特殊类型的router，标识可以进行修改，添加，后废弃
                if(r.getType() != 3){
                    JSONObject router = ApiUtils.convert(r, clientIP);
                    routerArray.add(router);
                }
            }
            json.put("routers", routerArray);

            String routerRingRetryInterval = ZKConfigManager.getInstance().getRouterConfig().getRouterRingRetryInterval();
            if(StrUtil.isNotEmpty(routerRingRetryInterval)){
                String[] routerRingRetryIntervalTmp = routerRingRetryInterval.split(",");
                JSONArray array = new JSONArray();
                array.addAll(List.of(routerRingRetryIntervalTmp));
                json.put("routerRingRetryInterval", array);
            }

            String connRetryInterval = ZKConfigManager.getInstance().getRouterConfig().getConnRetryInterval();
            json.put("connRetryInterval", connRetryInterval);

            ConfigRefreshInterval configRefreshInterval = ZKConfigManager.getInstance().getRouterConfig().getConfigRefreshInterval();
            JSONObject configRefreshIntervalJson = JSON.parseObject(JSON.toJSONString(configRefreshInterval));
            json.put("configRefreshInterval", configRefreshIntervalJson);

            Heartbeat[] heartbeats = ZKConfigManager.getInstance().getRouterConfig().getHeartbeats();
            JSONArray heartbeatArray = new JSONArray();
            for (Heartbeat heartbeat : heartbeats) {
                heartbeatArray.add(JSON.parseObject(JSON.toJSONString(heartbeat)));
            }
            json.put("heartbeats", heartbeatArray);
            return json.toJSONString();
        }catch (Exception e){
            log.error("", e);
            ResultVO<Map<String, String>> resultVO = new ResultVO<>(5001, "http登录异常", new HashMap<>());
            return JSON.toJSONString(resultVO);
        }
    }

    @Override
    public String getToken(String userId, String clientIP, String signature, String appKey, String timestamp, String nonce) {
        return "";
    }

    @Override
    public String webLogin(String clientIP, String countryCode, String version, String versionDetail, Map<String, String> params) {
        return "";
    }

    @Override
    public BaseResult<?> pushMsg(String from, String to, String content, String appId, String deviceId, Integer deviceType, Integer type, Integer subType, String title, String diyTypeName, Integer diyType, Long clientMsgId, String pushData, String requestId) {
        return null;
    }

    private List<Router> dealDefaultCountryRouters(CountryRouter countryRouter) {
        List<Router> routerResult = new ArrayList<>();
        List<Router> routers = countryRouter.getAreaRouters().get(defaultKey);
        for (Router router : routers) {
            String directLinkHost = ClusterManager.getInstance().getPublicGatewayCluster(System.currentTimeMillis());
            if(StringUtils.isBlank(router.getHost()) && (router.getRandomIps() == null || router.getRandomIps().isEmpty())){
                router.setHost(directLinkHost);
                routerResult.add(router);
                continue;
            }

            List<String> randomIps = router.getRandomIps();
            if(randomIps != null && !randomIps.isEmpty()){
                if(randomIps.size() > 1){
                    Collections.shuffle(randomIps);
                }
                String ip =  randomIps.getFirst();
                if(ip.equalsIgnoreCase(defaultKey)){
                    router.setHost(directLinkHost);
                    router.setPort(defaultPort);
                    routerResult.add(router);
                }else{
                    router.setHost(ip);
                    routerResult.add(router);
                }
            }
        }
        Collections.shuffle(routerResult);
        return routerResult;
    }

    private List<Router> dealCountryRouters(CountryRouter countryRouter, String directLinkHost, String countryCode) {
        List<Router> routerResult = new ArrayList<>();
        if(StrUtil.isNotBlank(countryCode)){
            String area = countryRouter.getRouters().get(countryCode);
            log.info("countryCode:{}, area:{}",countryCode, area);
            if(StrUtil.isNotBlank(area)){
                List<Router> routers = countryRouter.getAreaRouters().get(area);
                for (Router router : routers) {
                    if(StringUtils.isBlank(router.getHost()) && (router.getRandomIps() == null || router.getRandomIps().isEmpty())){
                        router.setHost(directLinkHost);
                        routerResult.add(router);
                        continue;
                    }
                    List<String> randomIps = router.getRandomIps();
                    if(randomIps != null && !randomIps.isEmpty()){
                        if(randomIps.size() > 1){
                            Collections.shuffle(randomIps);
                        }
                        String tmpHost =  randomIps.getFirst();
                        router.setHost(tmpHost);
                    }
                    routerResult.add(router);
                }
            }
        }
        return routerResult;
    }
}

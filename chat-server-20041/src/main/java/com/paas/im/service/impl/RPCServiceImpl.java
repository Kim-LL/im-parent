package com.paas.im.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.paas.im.constant.Constants;
import com.paas.im.model.proto.Packet;
import com.paas.im.service.ChannelService;
import com.paas.im.service.RPCService;
import com.paas.im.tool.rpc.RPCClient;
import com.paas.im.tool.rpc.RPCClientManager;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RPCServiceImpl implements RPCService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ChannelService channelService;

    /**
     * 发送 RPC 消息到指定服务
     */
    @Override
    public void sendMessage(String server, int port, Packet packet){
        int rpcPoolSize = ZKConfigManager.getInstance().getImConfig().getRpcPoolSize();
        int index = RandomUtil.randomInt(rpcPoolSize);

        // RPC KEY
        String rpcKey = server + Constants.SEQ + port + Constants.SEQ + index;
        RPCClient rpcClient = RPCClientManager.getInstance().getClientMap().get(rpcKey);

        if(rpcClient == null){
            log.error("RPC连接异常！rpcClient: {}, rpcKey: {}, packet: {}", rpcClient, rpcKey, packet.toString());
            throw new RuntimeException("Cannot get rpc connection!");
            //TODO 消息保存至 kafka
        }
        rpcClient.sendMessage(packet);
    }
}

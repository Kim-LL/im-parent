package com.paas.im.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.paas.im.constant.Constants;
import com.paas.im.model.proto.Packet;
import com.paas.im.service.RPCService;
import com.paas.im.tool.rpc.RPCClient;
import com.paas.im.tool.rpc.RPCClientManager;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RPCServiceImpl implements RPCService {

    @Override
    public void sendMessage(String server, int port, Packet packet) {
        int rpcPoolSize = ZKConfigManager.getInstance().getImConfig().getRpcPoolSize();

        // 随机出连接池中的某个连接索引数
        int index = RandomUtil.randomInt(rpcPoolSize);

        // RPC KEY
        String rpcKey = server + Constants.SEQ + port + Constants.SEQ + index;

        RPCClient rpcClient = RPCClientManager.getInstance().getClientMap().get(rpcKey);

        if(rpcClient == null){
            log.error("RPC连接异常! rpcKey: {}, package: {}", rpcKey, packet.toString());
            // TODO 保存到 Kafka 中（通过 dataServer 保存）
            throw new RuntimeException("Cannot get rpc connection!");
        }
        rpcClient.sendMessage(packet);
    }
}

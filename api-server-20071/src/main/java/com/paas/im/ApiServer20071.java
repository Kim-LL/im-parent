package com.paas.im;

import com.alibaba.fastjson2.JSON;
import com.paas.im.constant.Constants;
import com.paas.im.tool.cluster.ClusterManager;
import com.paas.im.tool.data.DataManager;
import com.paas.im.tool.data.DataSourceManager;
import com.paas.im.tool.rpc.RPCClientManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ApiServer20071 implements CommandLineRunner {

    public static void main() {
        SpringApplication.run(ApiServer20071.class);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("ApiServer20071 -- args: {}", JSON.toJSONString(args));

        try {
            //添加chat 服务 zookeeper监听
            // 通过 Constants.CHAT_CLUSTER 获取 zk 上的 ip 列表
            // 然后循环ip列表跟这个 Constants.CHAT_RPC_PORT 初始化 RPCClient
            // 将初始化好的 RPCClient 列表 放到 map 里，key 为 ip + "__" + port + "__" + index
            // 配置里定义好的总数，从0 到 总数 是这个 index
            RPCClientManager.getInstance().syncRPCServer(Constants.CHAT_CLUSTER, Constants.CHAT_RPC_PORT);
            ClusterManager.getInstance().initHotUpdateChatRPC();

            RPCClientManager.getInstance().syncRPCServer(Constants.GROUP_CHAT_CLUSTER, Constants.GROUP_CHAT_RPC_PORT);
            ClusterManager.getInstance().initHotUpdateGroupChatRPC();

            RPCClientManager.getInstance().syncRPCServer(Constants.ROOM_CLUSTER, Constants.ROOM_RPC_PORT);
            ClusterManager.getInstance().initHotUpdateRoomRPC();

            RPCClientManager.getInstance().syncRPCServer(Constants.DATA_CLUSTER, Constants.DATA_RPC_PORT);
            ClusterManager.getInstance().initHotUpdateDataRPC();

            //初始化 mongodb 链接
            DataSourceManager.getInstance().initDataSource();

        }catch (Exception e){
            log.error("ApiServer20071 -- error: {}", e.getMessage());
        }

    }
}

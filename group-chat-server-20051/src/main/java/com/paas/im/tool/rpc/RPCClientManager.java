package com.paas.im.tool.rpc;

import com.paas.im.constant.Constants;
import com.paas.im.tool.hash.HashCircle;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import com.paas.im.tool.zookeeper.ZKHelp;
import com.paas.im.utils.IpUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * RPC管理类
 */
@Slf4j
public class RPCClientManager {

    public boolean isConnected = false;

    @Getter
    private final ConcurrentMap<String, RPCClient> clientMap = new ConcurrentHashMap<>();

    private static final String localIp = IpUtils.getLocalIP();

    private static class InstanceHolder{
        private static final RPCClientManager INSTANCE = new RPCClientManager();
    }

    public static RPCClientManager getInstance(){
        return InstanceHolder.INSTANCE;
    }

    private RPCClientManager() {}

    /**
     * 初始化 RPCClient，并放入内存里，不要在外边直接 new RPCClient
     * @param server: 要链接对方服务的ip
     * @param port: 对方服务的端口
     * @param index: 索引
     */
    public void initRPCClient(String server, int port, int index) {
        int rpcRetryTimes = ZKConfigManager.getInstance().getImConfig().getRpcRetryTimes();
        int i = 0;
        String key = server + Constants.SEQ + port + Constants.SEQ + index;
        while(!isConnected){
            i++;
            if(i > rpcRetryTimes){
                break;
            }
            log.info("##########开始对 {} 进行第 {}/{} 次连接...", key,  i, rpcRetryTimes);
            try {
                RPCClient rpcClient = clientMap.get(key);
                log.info("开始连接IM key: {}, imServerIp:{}, localIp: {}, client0: {}, clientMap.get(key)): {}, clientMap.size(): {}", key, server, localIp, rpcClient,  RPCClientManager.getInstance().clientMap.get(key),  RPCClientManager.getInstance().clientMap.size());
                if(rpcClient != null){
                    log.info("{} 连接已存在,停止连接  client={}", key, rpcClient);
                    break;
                }
                synchronized (key.intern()) {
                    rpcClient = new RPCClient(server, port, index);   //服务端IP， 端口， 连接池索引
                    clientMap.putIfAbsent(key, rpcClient);
                }
                isConnected = true;
            } catch (Exception e){
                clientMap.remove(key);
                log.error("重连失败! 继续尝试...  key: {}, e:{}",  key, e.toString());
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                //nothing to do
            }
        }
        for(String k: clientMap.keySet()){
            log.info("RPCClient key: {}", k);
        }
    }

    /**
     * 初始化RPC链接
     * 所有初始化链接 原来的 LiveMe RPC，Live ZK 都是通过这个方法初始化
     * param: zkPath 通过这个 zkPath 获取下面数据子节点列表
     */
    public void syncRPCServer(String zkPath, int rpcPort){
        int rpcPoolSize = ZKConfigManager.getInstance().getImConfig().getRpcPoolSize();
        ZKHelp zk = ZKHelp.getInstance();
        // egg: /im/web/
        List<String> rpcServers = zk.getChildren(zkPath);
        // 如果包含了 /room/cluster
        if(zkPath.contains(Constants.ROOM_CLUSTER)){
            HashCircle.getInstance().init(zkPath, rpcServers);
        }

        log.info("开始注册rpc长连接服务...	localIp: {},  rpcServerList: {},  zkPath: {}", localIp, rpcServers, zkPath);

        for(final String serverIp: rpcServers){
            log.info("serverIp: {}", serverIp);
            try {
                // 创建链接池
                for (int i=0; i<rpcPoolSize; i++){
//                    Thread.ofVirtual().start(new RPCClientManager(serverIp, rpcPort, index));
                    final int index = i;
                    Thread.ofVirtual().start(() -> RPCClientManager.getInstance().initRPCClient(serverIp, rpcPort, index));
                }
            }catch (Exception e){
                log.error("RPC create error! 服务创建失败! host: {}",serverIp, e);
            }
        }
    }
}

package com.paas.im.tool.cluster;

import com.github.zkclient.IZkChildListener;
import com.paas.im.constant.Constants;
import com.paas.im.tool.hash.HashCircle;
import com.paas.im.tool.rpc.RPCClientManager;
import com.paas.im.tool.zookeeper.ZKHelp;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClusterManager {

    private static final ZKHelp zk = ZKHelp.getInstance();

    private static class InstanceHolder {
        private static final ClusterManager INSTANCE = new ClusterManager();
    }

    private ClusterManager() {}

    public static ClusterManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // 服务网关ip 列表
    private List<String> gatewayClusterList = new ArrayList<>();

    // zk 上记录的 ip 列表
    private List<String> webIMRpcList;

    private List<String> webIMClusterList;

    // zk 上记录的 ip 列表
    private List<String> chatRpcList;

    private List<String> chatClusterList;

    // zk 上记录的 ip 列表
    private List<String> groupChatRpcList;

    private List<String> groupChatClusterList;

    // zk 上记录的 ip 列表
    private List<String> roomRpcList;

    private List<String> roomClusterList;

    // zk 上记录的 ip 列表
    private List<String> apiRpcList;

    private List<String> apiClusterList;

    // zk 上记录的 ip 列表
    private List<String> dataRpcList;

    /**
     * 热更新 data server ip 列表
     */
    public void initHotUpdateDataRPC(){
        dataRpcList = zk.getChildren(Constants.DATA_CLUSTER);

        IZkChildListener dataRpcListener = new IZkChildListener() {

            @Override
            public void handleChildChange(String parentPath, List<String> currentChildren) throws Exception {
                log.info("handleChildChange parentPath:{},currentChildren:{}", parentPath, currentChildren);
                dataRpcList = currentChildren;
                RPCClientManager.getInstance().syncRPCServer(Constants.DATA_CLUSTER, Constants.DATA_RPC_PORT);
            }
        };

        zk.subscribeChildChanges(Constants.DATA_CLUSTER, dataRpcListener);
    }

    /**
     * 热更新 webIM ip 列表
     */
    public void initHotUpdateWebIMRPC(){
        webIMRpcList = zk.getChildren(Constants.WEB_IM_CLUSTER);
        log.info("webIMRpcList: {}", webIMRpcList);
        IZkChildListener webIMListener = new IZkChildListener() {

            @Override
            public void handleChildChange(String parentPath, List<String> currentChildren) throws Exception {
                log.info("----->>>>> Starting handle children change {}/{}, size: {}", parentPath, currentChildren, currentChildren.size());
                webIMRpcList = currentChildren;
                RPCClientManager.getInstance().syncRPCServer(Constants.WEB_IM_CLUSTER, Constants.WEB_IM_RPC_PORT);
            }
        };
        //监控节点变更
        zk.subscribeChildChanges(Constants.WEB_IM_CLUSTER, webIMListener);
    }

    /**
     * 获取 WebIM 服务IP
     */
    public String getWebIMServerIP(int flag){
        int num = Math.abs(flag) % webIMRpcList.size();
        return webIMRpcList.get(num);
    }

    public List<String> getWebIMRPCList(){
        return webIMRpcList;
    }

    /**
     * 热更新 Chat RPC ip 列表
     */
    public void initHotUpdateChatRPC(){
        chatRpcList = zk.getChildren(Constants.CHAT_CLUSTER);
        log.info("chatRpcList:{}", chatRpcList);
        IZkChildListener chatListener = new IZkChildListener() {

            @Override
            public void handleChildChange(String parentPath, List<String> currentChildren) throws Exception {
                // 监听到子节点变化 更新cluster
                log.info("----->>>>> Starting handle children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
                chatRpcList = currentChildren;
                RPCClientManager.getInstance().syncRPCServer(Constants.CHAT_CLUSTER, Constants.CHAT_RPC_PORT);
            }
        };
        zk.subscribeChildChanges(Constants.CHAT_CLUSTER, chatListener);
    }

    public String getChatServerIP(int flag){
        int num = Math.abs(flag) % chatRpcList.size();
        return chatRpcList.get(num);
    }

    public List<String> getChatRPCList(){
        return chatRpcList;
    }

    /**
     * 热更新 group chat rpc ip 列表
     */
    public void initHotUpdateGroupChatRPC(){
        groupChatRpcList = zk.getChildren(Constants.GROUP_CHAT_CLUSTER);
        log.info("groupChatRpcList:{}", groupChatRpcList);
        IZkChildListener groupChatListener = new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChildren) throws Exception {
                // 监听到子节点变化 更新cluster
                log.info("----->>>>> Starting handle children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
                groupChatRpcList = currentChildren;
                RPCClientManager.getInstance().syncRPCServer(Constants.GROUP_CHAT_CLUSTER, Constants.GROUP_CHAT_RPC_PORT);
            }
        };
        zk.subscribeChildChanges(Constants.GROUP_CHAT_CLUSTER, groupChatListener);
    }

    public String getGroupChatServerIP(int flag){
        int num = Math.abs(flag) % groupChatRpcList.size();
        return groupChatRpcList.get(num);
    }

    public List<String> getGroupChatRPCList(){
        return groupChatRpcList;
    }

    /**
     * 热更新 room rpc ip 列表
     */
    public void initHotUpdateRoomRPC(){
        roomRpcList = zk.getChildren(Constants.ROOM_CLUSTER);
        log.info("roomRpcList:{}", roomRpcList);
        IZkChildListener roomListener = new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChildren) throws Exception {
                // 监听到子节点变化 更新cluster
                log.info("----->>>>> Starting handle children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
                roomRpcList = currentChildren;
                HashCircle.getInstance().init(Constants.ROOM_CLUSTER, roomRpcList);
                RPCClientManager.getInstance().syncRPCServer(Constants.ROOM_CLUSTER, Constants.ROOM_RPC_PORT);
            }
        };
        zk.subscribeChildChanges(Constants.ROOM_CLUSTER, roomListener);
    }

    public String getRoomServerIP(int flag){
        int num = Math.abs(flag) % roomRpcList.size();
        return roomRpcList.get(num);
    }

    /**
     * 热更新 api rpc 链接
     */
    public void initHotUpdateApiRPC(){
        apiRpcList = zk.getChildren(Constants.API_CLUSTER);
        log.info("apiRpcList:{}", apiRpcList);
        IZkChildListener apiListener = new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChildren) throws Exception {
                // 监听到子节点变化 更新cluster
                log.info("----->>>>> Starting handle children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
                apiRpcList = currentChildren;

                RPCClientManager.getInstance().syncRPCServer(Constants.API_CLUSTER, Constants.API_RPC_PORT);
            }
        };
        zk.subscribeChildChanges(Constants.API_CLUSTER, apiListener);
    }

    public String getApiServerIP(int flag){
        int num = Math.abs(flag) % apiRpcList.size();
        return apiRpcList.get(num);
    }

    public List<String> getApiRPCList(){
        return apiRpcList;
    }


    /**
     * 热更新 网关配置信息
     */
    public void intiHotUpdatePublicGatewayCluster(){
        gatewayClusterList = zk.getChildren(Constants.GATEWAY_CLUSTER);
        log.info("gatewayClusterList: {}", gatewayClusterList);
        IZkChildListener gatewayListener = (parentPath, currentChildren) -> {
            // 监听到子节点变化 更新cluster
            log.info("----->>>>> Starting handle children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
            gatewayClusterList = currentChildren;
        };
        zk.subscribeChildChanges(Constants.GATEWAY_CLUSTER, gatewayListener);
    }

    /**
     * flag: 一般传时间戳
     */
    public String getPublicGatewayCluster(long flag){
        int num = Math.toIntExact(Math.abs(flag) % gatewayClusterList.size());
        return gatewayClusterList.get(num);
    }
}

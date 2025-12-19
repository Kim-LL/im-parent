package com.paas.im.tool.data;

import com.github.zkclient.IZkChildListener;
import com.paas.im.constant.Constants;
import com.paas.im.tool.hash.HashCircle;
import com.paas.im.tool.rpc.RPCClientManager;
import com.paas.im.tool.zookeeper.ZKHelp;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class DataManager {

    private static final ZKHelp zk = ZKHelp.getInstance();

    private static class InstanceHolder{
        private static final DataManager INSTANCE = new DataManager();
    }

    public static DataManager getInstance(){
        return InstanceHolder.INSTANCE;
    }

    private List<String> imRpcList = null;

    private List<String> webIMRpcList = null;

    private List<String> webIMPublickClusterList = null;

    private List<String> chatRpcList = null;

    private List<String> groupChatRpcList = null;

    private List<String> roomRpcList = null;

    private List<String> apiRpcList = null;

    private List<String> pushRpcList = null;

    private List<String> taskRpcList = null;

    private List<String> dataServerRpcList = null;

    /**
     * WEB IM RPC连接
     */
    public void initWebIMRPC(){
        webIMRpcList = zk.getChildren(Constants.WEB_IM_CLUSTER);
        log.info("initWebIMRPC webIMRpcList:{}", webIMRpcList);
        IZkChildListener listListener = (parentPath, currentChildren) -> {
            // 监听到子节点变化 更新cluster
            log.info("----->>>>> Starting handle web im children change {}/{} size={}", parentPath, currentChildren, currentChildren.size());
            webIMRpcList = currentChildren;
            RPCClientManager.getInstance().syncRPCServer(Constants.WEB_IM_CLUSTER, Constants.WEB_IM_RPC_PORT);
        };
        zk.subscribeChildChanges(Constants.WEB_IM_CLUSTER, listListener);
    }

    /**
     * Chat RPC连接
     */
    public void initChatRPC(){
        chatRpcList = zk.getChildren(Constants.CHAT_CLUSTER);
        log.info("chatRpcList:{}", chatRpcList);
        IZkChildListener listListener = (parentPath, currentChildren) -> {
            // 监听到子节点变化 更新cluster
            log.info("----->>>>> Starting handle chat children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
            chatRpcList = currentChildren;
            RPCClientManager.getInstance().syncRPCServer(Constants.CHAT_CLUSTER, Constants.CHAT_RPC_PORT);
        };
        zk.subscribeChildChanges(Constants.CHAT_CLUSTER, listListener);
    }

    /**
     * GroupChat RPC连接
     */
    public void initGroupChatRPC(){
        groupChatRpcList = zk.getChildren(Constants.GROUP_CHAT_CLUSTER);
        log.info("groupChatRpcList:{}", groupChatRpcList);
        IZkChildListener listListener = (parentPath, currentChildren) -> {
            // 监听到子节点变化 更新cluster
            log.info("----->>>>> Starting handle group chat children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
            groupChatRpcList = currentChildren;
            RPCClientManager.getInstance().syncRPCServer(Constants.GROUP_CHAT_CLUSTER, Constants.GROUP_CHAT_RPC_PORT);
        };
        zk.subscribeChildChanges(Constants.GROUP_CHAT_CLUSTER, listListener);
    }

    public void initRoomTobRPC() {
        roomRpcList = zk.getChildren(Constants.ROOM_CLUSTER);
        log.info("roomRpcList:{}", roomRpcList);
        IZkChildListener listener = (parentPath, currentChildren) -> {
            // 监听到子节点变化 更新cluster
            log.info("----->>>>> Starting handle room children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
            roomRpcList = currentChildren;
            HashCircle.getInstance().init(Constants.ROOM_CLUSTER, roomRpcList);
            RPCClientManager.getInstance().syncRPCServer(Constants.ROOM_CLUSTER, Constants.ROOM_RPC_PORT);
        };
        // 监控节点变更
        zk.subscribeChildChanges(Constants.ROOM_CLUSTER, listener);
    }

    /**
     * Push RPC连接
     */
    public void initPushRPC() {
        pushRpcList = zk.getChildren(Constants.PUSH_CLUSTER);
        log.info("pushRpcList:{}", pushRpcList);
        IZkChildListener listener = (parentPath, currentChildren) -> {
            // 监听到子节点变化 更新cluster
            log.info("----->>>>> Starting handle push rpc children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
            pushRpcList = currentChildren;
            RPCClientManager.getInstance().syncRPCServer(Constants.PUSH_CLUSTER, Constants.PUSH_RPC_PORT);
        };
        // 监控节点变更
        zk.subscribeChildChanges(Constants.PUSH_CLUSTER, listener);
    }

    /**
     * Api RPC连接
     */
    public void initApiRPC() {
        apiRpcList = zk.getChildren(Constants.API_CLUSTER);
        log.info("apiRpcList:{}", apiRpcList);
        IZkChildListener listener = (parentPath, currentChildren) -> {
            // 监听到子节点变化 更新cluster
            log.info("----->>>>> Starting handle api children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
            apiRpcList = currentChildren;
            RPCClientManager.getInstance().syncRPCServer(Constants.API_CLUSTER, Constants.API_RPC_PORT);
        };
        // 监控节点变更
        zk.subscribeChildChanges(Constants.API_CLUSTER, listener);
    }

    /**
     * Task RPC连接
     */
	public void initTaskRPC() {
		taskRpcList = zk.getChildren(Constants.TASK_CLUSTER);
		log.info("taskRpcList:{}", taskRpcList);
		IZkChildListener listener = (parentPath, currentChildren) -> {
            // 监听到子节点变化 更新cluster
            log.info("----->>>>> Starting handle task children change {}/{} size: {}", parentPath, currentChildren, currentChildren.size());
            taskRpcList = currentChildren;
            RPCClientManager.getInstance().syncRPCServer(Constants.TASK_CLUSTER, Constants.TASK_RPC_PORT);
        };
		// 监控节点变更
		zk.subscribeChildChanges(Constants.TASK_CLUSTER, listener);
	}

    /**
     * 初始化数据服务zk监听
     */
    public void initDataServerRPC(){
        dataServerRpcList = zk.getChildren(Constants.DATA_CLUSTER);
        log.info("initDataServerRpc dataServerRpcList:{}", dataServerRpcList);
        IZkChildListener dataServerListener = (parentPath, currentChildren) -> {
            log.info("handleChildChange parentPath:{},currentChildren:{}",parentPath,currentChildren);
            dataServerRpcList = currentChildren;
            RPCClientManager.getInstance().syncRPCServer(Constants.DATA_CLUSTER, Constants.DATA_RPC_PORT);
        };
        zk.subscribeChildChanges(Constants.DATA_CLUSTER, dataServerListener);
    }

    /**
     * 获取WebIM服务IP
     * 根据hashCode取余
     */
    public String getWebIMServerIP(long flag){
        long num = Math.abs(flag) % webIMRpcList.size();
        return webIMRpcList.get((int)num);
    }

    /**
     * 获取ROOM服务IP
     */
    public String getRoomServerIP(long flag) {
        long num = Math.abs(flag)% roomRpcList.size();
        return roomRpcList.get((int) num);
    }

    /**
     * 获取CHAT服务IP
     */
    public String getChatServerIP(long flag){
        long num = Math.abs(flag) % chatRpcList.size();
        return chatRpcList.get((int)num);
    }

    /**
     * 获取GroupChat服务IP
     */
    public String getGroupChatServerIP(long flag){
        long num = Math.abs(flag) % groupChatRpcList.size();
        return groupChatRpcList.get((int) num);
    }

    /**
     * 获取Push服务IP
     */
    public String getPushServerIP(long flag) {
        long num = Math.abs(flag) % pushRpcList.size();
        return pushRpcList.get((int) num);
    }

    /**
     * 获取Task服务IP
     */
    public String getTaskServerIP(long flag) {
        long num = Math.abs(flag) % taskRpcList.size();
        return taskRpcList.get((int) num);
    }

    /**
     * 获取 data 服务IP
     */
    public String getDataServerIP(long flag){
        long num = Math.abs(flag) % dataServerRpcList.size();
        return dataServerRpcList.get((int)num);
    }

    /**
     * 获取Api服务IP
     */
    public String getApiServerIP(long flag) {
        long num = Math.abs(flag) % apiRpcList.size();
        return apiRpcList.get((int) num);
    }
}

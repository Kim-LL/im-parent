package com.paas.im.tool.zookeeper;

import com.paas.im.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZKRegister {

    private static final ZKHelp zk = ZKHelp.getInstance();

    private ZKRegister() {}

    private static class ZKRegisterHolder {
        private static final ZKRegister INSTANCE = new ZKRegister();
    }

    public static ZKRegister getInstance() {
        return  ZKRegisterHolder.INSTANCE;
    }

    /**
     * 1. 查询 zookeeper 上指定 cluster 下子节点信息列表
     * 2. 查看列表里是否包含了本地私有ip,包含则直接退出
     * 3.
     * @param cluster: egg /data/cluster
     */
    public void registerIp(String cluster) {
        String privateIp = IpUtils.getAWSLocalIP();
        Thread.ofVirtual().start(() -> {
            boolean isFound = false;
            while(true){
                // /data/cluster
                List<String> servers = zk.getChildren(cluster);
                for (String server : servers) {
                    if (server.equals(privateIp)) {
                        isFound = true;
                        break;
                    }
                }
                if(isFound){
                    // 已经写入到 zookeeper 里
                    break;
                }
                // 正常为1，表示新增了1个, 所以只要小于等于0 就在重新执行一次，中间休眠1s
                while (!zk.registerInCluster(cluster, privateIp)) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("注册[{} -- {}]失败: {}", privateIp, cluster, e.getMessage());
                    }
                }

            }
        });
    }
}

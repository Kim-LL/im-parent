package com.paas.im.tool.hash;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class HashCircle {

    private static final MD5 md5 = MD5.create();

    private static final int numberOfReplicas = 64;

    // hash 槽位
    private static final int circleNodes = 10240;

    /**
     * 用于存储不同类型的集群对应的哈希环 例如 : /chat/cluster -> circle    /room/cluster -> circle
     */
    private final Map<String,String[]> maps = new ConcurrentHashMap<>(5);

    private static volatile HashCircle instance = null;

    private HashCircle(){}

    public static HashCircle getInstance(){
        if(instance == null){
            synchronized (HashCircle.class){
                if(instance == null){
                    instance = new HashCircle();
                }
            }
        }
        return instance;
    }

    public synchronized void init(String cluster, List<String> serverList){
        if(CollectionUtil.isEmpty(serverList)){
            return;
        }
        String[] circle = new String[circleNodes];
        List<String> totalNodes = new ArrayList<>();
        for(String server : serverList){
            if(StrUtil.isEmpty(server)){
                continue;
            }
            /**
             * 每个服务节点生成 numberOfReplicas 个虚拟节点
             * 比如 192.168.13.22#0, 192.168.13.22#1 ... 192.168.13.22#63
             */
            List<String> virtualNodes = generateVirtualNodes(server);
            totalNodes.addAll(virtualNodes);
        }

        // 将这些服务节点 通过md5后的 hash
        hashNodesToCircle(totalNodes, circle);

        // 更新之前的哈希环
        maps.put(cluster, circle);
    }

    /**
     * 将所有服务节点均匀分布到环上
     * @param totalNodes: 所有的服务节点
     * @param circle: 10240 长度数组 hash 环
     */
    private static void hashNodesToCircle(List<String> totalNodes, String[] circle){
        for(String node : totalNodes){
            String crypt = md5.digestHex(node, StandardCharsets.UTF_8);
            // 因为 md5 后的 hashCode 有可能有负数，所以需要 Math.abs
            int abs = Math.abs(crypt.hashCode());
            int index = (abs % circleNodes);
            circle[index] = node;
        }
    }

    private static List<String> generateVirtualNodes(String server){
        List<String> objects = new ArrayList<>(numberOfReplicas);
        for(int i = 0; i < numberOfReplicas; i++){
            objects.add(server + "#" + i);
        }
        return objects;
    }

    /**
     * 根据 cluster 和 key 获取映射后的结点
     * @param cluster: 集群zk路径
     * @param key： hashKey
     */
    public String get(String cluster, String key){
        String[] circle = maps.get(cluster);
        if(circle == null || circle.length == 0){
            return null;
        }
        // 获取 md5 值
        String crypt = md5.digestHex(key, StandardCharsets.UTF_8);
        int index = Math.abs(crypt.hashCode()) % circleNodes;
        for(int i=index; i< circle.length; i++){
            String selectNode = circle[i];
            if(selectNode != null){
                return selectNode.split("#")[0];
            }
        }
        for(int j=0;j<index;j++){
            String selectNode = circle[j];
            if(selectNode != null){
                return selectNode.split("#")[0];
            }
        }
        return null;
    }

    static void main() {
        HashCircle instance = HashCircle.getInstance();
        String chatCluster = "/im/chat/cluster";
        instance.init(chatCluster, List.of("127.0.0.1,127.0.0.2,127.0.0.3,127.0.0.4".split(",")));

        int total = 10;

        Map<String, String> map = new HashMap<>();
        for(int i=0;i<total;i++){
            String node = "test_" + i + "_node";
            String result = instance.get(chatCluster, node);
            map.putIfAbsent(node, result);
        }

        // 移除节点
        instance.init(chatCluster, List.of("127.0.0.2,127.0.0.3".split(",")));
        int num = 0;
        Map<String, String> map2 = new HashMap<>();
        for (int i = 0; i < total; i++) {
            String node = "test_" + i + "_node";
            String result = instance.get(chatCluster, node);
            map2.putIfAbsent(node, result);
            if(!map.get(node).equals(result)){
                num++;
            }
        }

        Map<String, List<String>> collect2 = map2.values().stream().collect(Collectors.groupingBy(String::intern));
        System.out.println(num);

        // 添加节点
        instance.init(chatCluster, List.of("127.0.0.1,127.0.0.2,127.0.0.3,127.0.0.4,127.0.0.5".split(",")));

        num = 0;
        Map<String, String> map3 = new HashMap<>();
        for (int i = 0; i < total; i++) {
            String node = "test_" + i + "_node";
            String result = instance.get(chatCluster, node);
            map3.putIfAbsent(node, result);
            if(!map.get(node).equals(result)){
                num++;
            }
        }

        Map<String, List<String>> collect3  = map3.values().stream().collect(Collectors.groupingBy(String::intern));
        log.info("num: {}", num);
    }
}

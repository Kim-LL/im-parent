package com.paas.im.tool.mongo;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.github.zkclient.IZkDataListener;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.paas.im.tool.zookeeper.ZKHelp;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * mongoDB 链接池
 * mongoClientList 里便是所有的链接
 */
@Slf4j
@Getter
public class DBPool {

    private static final Map<String, DBPool> map = new HashMap<>();

    private final List<MongoClient> mongoClientList = new ArrayList<>();

    private final int poolSize = 500;

    private final int blockSize = 20;

    // 在建立（打开）套接字连接时的超时时间（ms），默认为0（无限）
    private final int connectTimeout = 100000;

    //套接字超时时间;该值会被传递给Socket.setSoTimeout(int)。默认为0（无限）
    private final int socketTimeout = 100000;

    //被阻塞线程从连接池获取连接的最长等待时间（ms）
    private final int maxWaitTime = 20000;

    private String dbName;

    private String username;

    private String pwd;

    private String authDB = null;

    private final static ZKHelp zk = ZKHelp.getInstance();

    private DBPool(){}

    // zkPath egg: "/app/config/mongo"
    public synchronized static DBPool getInstance(String zkPath){
        DBPool instance = map.get(zkPath);
        if(instance != null){
            return instance;
        }
        final DBPool dbPool = new DBPool();
        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String dataPath, byte[] data) throws Exception {
                log.info("!!! mongo node [{}] data has been changed !!!", dataPath);
                String redisServerInfo = new String(data, StandardCharsets.UTF_8);
                dbPool.initialPool(dataPath);
                log.info("!!! mongo node[{}] connection pool has been rebuild !!! {}", redisServerInfo, dataPath);
            }

            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                log.info("!!! mongo node [{}] data has been deleted !!!", dataPath);
            }
        };
        // 节点天津爱监控
        zk.subscribeDataChange(zkPath, listener);
        // 初始化公用 redis 集群
        instance.initialPool(zkPath);
        map.put(zkPath, dbPool);
        return instance;
    }

    public synchronized static DBPool getInstance(String uri, int port, String username, String password, String dbName){
        final DBPool dbPool = new DBPool();
        dbPool.dbName = dbName;
        dbPool.username = username;
        dbPool.initialPool(uri, port, username, password, dbName);
        return dbPool;
    }

    /**
     * 初始化连接池，设置参数。 读zookeeper
     */
    public void initialPool(@NonNull String zkPath){
        log.info("start init connection pools:{}", zkPath);
        // 如果不为 null 先清理在建立
        if(!mongoClientList.isEmpty()){
            // 里面有 mongoClientList.clear();
            releaseRs();
        }
        List<List<ServerAddressExtends>> serverAddressList = null;
        try {
            // mongodb 连接串中间用空，开，多个实例用|分割
            // ${ip}:${port}:${dbname}:${username}:${pwd},${ip}:${port}:${dbname}:${username}:${pwd},${ip}:${port}:${dbname}:${username}:${pwd}|${ip}:${port}:${dbname}:${username}:${pwd},${ip}:${port}:${dbname}:${username}:${pwd}
            serverAddressList = convertConnectAddress(zkPath);

            log.info("start init connection pools, list:{}", serverAddressList);
            for (List<ServerAddressExtends> serverAddressExtendsList : serverAddressList) {
                MongoClient mongoClient = null;
                // 其他参数根据实际情况添加
                MongoClientOptions mco = new MongoClientOptions.Builder()
                        .writeConcern(WriteConcern.MAJORITY)
                        .connectionsPerHost(poolSize)
                        .connectTimeout(connectTimeout)
                        .socketTimeout(socketTimeout)
                        .maxWaitTime(maxWaitTime).build();
                List<ServerAddress> tmp = new ArrayList<>();
                MongoCredential credential = null;

                for (ServerAddressExtends serverAddressextends : serverAddressExtendsList) {
                    tmp.add(serverAddressextends.getServerAddress());
                    if (StrUtil.isEmpty(this.authDB)) {
                        credential = MongoCredential.createCredential(
                                serverAddressextends.getUsername(),
                                serverAddressextends.getDbName(),
                                serverAddressextends.getPwd().toCharArray()
                        );
                    } else {
                        credential = MongoCredential.createCredential(
                                serverAddressextends.getUsername(),
                                this.authDB,
                                serverAddressextends.getPwd().toCharArray()
                        );
                    }
                }
                // tmp MongoDB 集群的节点地址列表,egg: 192.168.1.100:27017、192.168.1.101:27017
                // mongodb 集群分为 主节点，从节点，仲裁节点,
                // mongo 链接任意一个节点，自动发现整个副本集的所有节点
                // 当主节点故障时，副本集会自动选举新的主节点，MongoClient会自动感知并切换到新主节点，无需修改客户端代码
                // 只传单个ServerAddress 若该节点故障（比如该节点当时挂了，或者该节点挂了以后服务重启），MongoClient无法发现其他节点
                mongoClient = new MongoClient(tmp, credential, mco);
                mongoClientList.add(mongoClient);
            }
            log.info("end init mongo connection pools, mongoClientList:{}", mongoClientList);
        }catch (Exception e){
            log.error("构造连接池出错 {}, error:{}", serverAddressList,e);
            log.info("设置zk节点");
            if(!zk.exists(zkPath)){
                zk.addNode(zkPath, false);
                log.info("创建zk节点:{}", zkPath);
            }
        }
    }

    public void initialPool(String uri, int port, String username, String password, String dbName){
        // 不为 null，先清理在建立
        if(!mongoClientList.isEmpty()){
            releaseRs();
        }
        ServerAddressExtends serverAddressExtends = null;
        try {
            serverAddressExtends = convertConnectAddress(uri, port, username, password, dbName);
            log.info("start init connection pools, serverAddressExtends:{}", serverAddressExtends);
            MongoClient mongoClient = null;
            MongoClientOptions mco = new MongoClientOptions.Builder()
                    .writeConcern(WriteConcern.MAJORITY)
                    .connectionsPerHost(poolSize)
                    .connectTimeout(connectTimeout)
                    .socketTimeout(socketTimeout)
                    .maxWaitTime(maxWaitTime)
                    .build();
            List<ServerAddress> tmp = new ArrayList<>();
            tmp.add(serverAddressExtends.getServerAddress());
            MongoCredential credential = null;
            if(StrUtil.isBlank(this.authDB)) {
                credential = MongoCredential.createCredential(serverAddressExtends.getUsername(), serverAddressExtends.getDbName(), serverAddressExtends.getPwd().toCharArray());
            }else {
                credential = MongoCredential.createCredential(serverAddressExtends.getUsername(), this.authDB, serverAddressExtends.getPwd().toCharArray());
            }
            mongoClient = new MongoClient(tmp, credential, mco);
            mongoClientList.add(mongoClient);
            log.info("end init mongo connection pools, size:{}", mongoClientList.size());
        } catch (Exception e){
            log.error("构造连接池出错 {} ,error:{}", serverAddressExtends, e);
        }
    }

    public void releaseRs(){
        for(MongoClient mc : mongoClientList){
            Optional.ofNullable(mc).ifPresent(db -> {
                try {
                    db.close();
                }catch (Exception e){
                    log.error("close mongo client error!", e);
                }
            });
        }
        mongoClientList.clear();
    }

    /**
     * 构造 mongo connection list
     * zkPath: ${ip}:${port}:${dbname}:${username}:${pwd},${ip}:${port}:${dbname}:${username}:${pwd},${ip}:${port}:${dbname}:${username}:${pwd}|${ip}:${port}:${dbname}:${username}:${pwd},${ip}:${port}:${dbname}:${username}:${pwd}
     */
    private List<List<ServerAddressExtends>> convertConnectAddress(String zkPath) {
        List<List<ServerAddressExtends>> listServerAddressBig = new ArrayList<>();
        String[] mongoArrays = ZKHelp.getInstance().getValue(zkPath).split("[|]");
        try {
            // 切割 "|" 形成的列表循环
            for (String mongoArray : mongoArrays) {
                List<ServerAddressExtends> extendsList = new ArrayList<>();
                String[] addressesArray = mongoArray.split(",");
                // // 切割 "," 形成的列表循环
                for (String s : addressesArray) {
                    String[] addressArray = s.split(":");
                    ServerAddress serverAddress = new ServerAddress(addressArray[0], Integer.parseInt(addressArray[1]));
                    ServerAddressExtends serverAddressExtends = new ServerAddressExtends();
                    serverAddressExtends.setServerAddress(serverAddress);
                    if (addressArray.length > 4) {
                        this.dbName = addressArray[2];
                        this.username = addressArray[3];
                        this.pwd = addressArray[4];
                        serverAddressExtends.setDbName(addressArray[2]);
                        serverAddressExtends.setUsername(addressArray[3]);
                        serverAddressExtends.setPwd(addressArray[4]);
                    }
                    if (addressArray.length > 5) {
                        this.authDB = addressArray[5];
                    }
                    extendsList.add(serverAddressExtends);
                }
                listServerAddressBig.add(extendsList);
            }
        } catch (Exception e){
            log.error("初始化mongo list出错 {}", JSON.toJSONString(mongoArrays));
            throw new RuntimeException("获取mongo配置串出错");
        }
        return listServerAddressBig;
    }

    private ServerAddressExtends convertConnectAddress(String uri, Integer port, String username, String password,
                                                       String dbName){
        ServerAddressExtends serverAddressExtends = new ServerAddressExtends();
        serverAddressExtends.setServerAddress(new ServerAddress(uri, port));
        serverAddressExtends.setDbName(dbName);
        serverAddressExtends.setUsername(username);
        serverAddressExtends.setPwd(password);
        return serverAddressExtends;
    }

    public MongoClient getClient(String dbKey){
        int num = dbKey.hashCode();
        if(num == Integer.MIN_VALUE){
            num = 0;
        }
        num = Math.abs(num);
        int total = mongoClientList.size();
        if(total == 0){
            log.error("zk 数据节点没有写入 mongo信息");
            throw new RuntimeException("mongodb 没有注册到zk ");
        }
        int result = num % total;
        return mongoClientList.get(result);
    }

    /**
     * 获取 mongo 里的 collection
     * @param dbKey: 用来负载均衡，取模用的
     * @param collectionName: collection name
     * @return 返回 collection
     */
    public MongoCollection<Document> getCollection(String dbKey, String collectionName){
        MongoDatabase mongoDB = getMongoDB(dbKey);
        // collection 相当于关系数据库的表名
        // MongoDB 下面包含了多个 collection
        return mongoDB.getCollection(collectionName);
    }

    private MongoDatabase getMongoDB(String dbKey){
        int num = dbKey.hashCode();
        if(num == Integer.MIN_VALUE){
            num = 0;
        }
        num = Math.abs(num);
        int total = mongoClientList.size();
        if(total == 0){
            log.error("zk 数据节点没有记录 mongodb 配置信息");
            throw new RuntimeException("mongodb 没有注册到zk ");
        }
        int result = num % total;

        return mongoClientList.get(result).getDatabase(dbKey);
    }
}

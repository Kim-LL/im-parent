package com.paas.im.tool.zookeeper;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.github.zkclient.IZkChildListener;
import com.github.zkclient.IZkDataListener;
import com.github.zkclient.ZkClient;
import com.paas.im.enums.EnvEnum;
import com.paas.im.utils.RSACryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ZKHelp {

    private static class InstanceHolder {
        private static final ZKHelp INSTANCE = new ZKHelp();
    }

    public static ZKHelp getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private ZKHelp() {
        init();
    }

    // 环境标志
    private String environmentFlag = "";
    private final String scheme = "digest";
    private boolean isOp = false;
    private final String privateKeyFile = "/root/.ssh/cjet_pri.pkcs8";
    private String publicKeyFile = "";

    public ZkClient client = null;
    private final static String envRegex = "(/\\w+){2}";
    // zookeeper集群地址 开发环境
    public String zookeeperCluster = "";

    public int sessionTimeout = 60000;
    public int connectionTimeout = 60000;

    private void init(){
        try {
            zookeeperCluster = getZkCluster();
            client = new ZkClient(zookeeperCluster, sessionTimeout, connectionTimeout);
            // 取 System.getProperty("config.type")
            environmentFlag = EnvEnum.getEnvironmentFlag();
            log.info("environmentFlag: {}", environmentFlag);
            if(StrUtil.isEmpty(environmentFlag)){
                throw new RuntimeException("environmentFlag should not be empty");
            }
            if(!environmentFlag.matches(envRegex)){
                throw new RuntimeException(environmentFlag + " is not a right environmentFlag :" + envRegex);
            }

            ZooKeeper zooKeeper = client.getZooKeeper();
            // 授权
            String publicKeyFilePath = getPublicKeyFilePath();
            if(StrUtil.isNotEmpty(publicKeyFilePath)){
                if(!RSACryptoUtils.verify(publicKeyFilePath)){
                    throw new RuntimeException("invalid publicKeyFile:" + publicKeyFilePath);
                }
                isOp = true;
                this.publicKeyFile = publicKeyFilePath;
            }else{
                isOp = false;
            }
            // 设置当前session的权限
            String authInfo = "chanjetzk:" + environmentFlag.split("/")[1];
            if(isOp){
                authInfo = "chanjetop:chanjetop";
            }
            zooKeeper.addAuthInfo(scheme, authInfo.getBytes(StandardCharsets.UTF_8));

            String rootPath = environmentFlag;
            this.addNode(rootPath);
        } catch (Exception e){
            log.error("initZKHelp error:: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 添加zk节点,默认是不加密节点
     */
    public void addNode(String path) {
        this.addNode(path, false);
    }

    /**
     * 添加zk节点
     */
    public void addNode(String path, boolean isEncrypted) {
        try {
            if (!checkEnv(path))
                path = environmentFlag + path;
            ZooKeeper zooKeeper = client.getZooKeeper();
            if (client.exists(path)) {
                setAuth(zooKeeper, path, isEncrypted);
                return;
            }
            // 初始化权限信息

            String tmpPath = "";
            String[] array = path.split("/");
            for (String anArray : array) {
                if (StrUtil.isEmpty(anArray)) {
                    continue;
                }
                tmpPath = tmpPath + "/" + anArray;
                // 节点不存在先创建
                if (!client.exists(tmpPath)) {
                    client.createPersistent(tmpPath, null);
                }
            }
            setAuth(zooKeeper, path, isEncrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置路径访问权限
     *
     * @param isEncrypted 如果是加密节点，则应用只有读的权限
     */
    private void setAuth(ZooKeeper zooKeeper, String path, boolean isEncrypted) throws Exception {
        List<ACL> aclList = new ArrayList<>();
        Id id = new Id("world", "anyone");
        ACL acl = new ACL(ZooDefs.Perms.ALL, id);
        aclList.add(acl);
        if (isEncrypted) {
            // 运维有所有节点的读写权限
            String authInfo = "chanjetzk:" + EnvEnum.getEnvironmentFlag().split("/")[1];
            id = new Id(scheme, DigestAuthenticationProvider.generateDigest(authInfo));
            acl = new ACL(ZooDefs.Perms.READ, id);
            aclList.add(acl);
        }
        zooKeeper.setACL(path, aclList, -1);
        // 暂时取消节点加权限（需要时放开这段代码即可）
        // List<ACL> aclList = new ArrayList<>();
        // String authInfo = "chanjetzk:" + Env.getEnvironmentFlag().split("/")[1];
        // Id id = new Id(scheme,
        // DigestAuthenticationProvider.generateDigest(authInfo));
        //
        // ACL acl = new ACL(ZooDefs.Perms.ALL, id);;
        // if (!isEncrypted) {
        // acl = new ACL(ZooDefs.Perms.ALL, id);
        // } else {
        // acl = new ACL(ZooDefs.Perms.READ, id);
        // }
        // aclList.add(acl);
        // // 运维有所有节点的读写权限
        // authInfo = "chanjetop:chanjetop";
        // id = new Id(scheme,
        // DigestAuthenticationProvider.generateDigest(authInfo));
        // acl = new ACL(ZooDefs.Perms.ALL, id);
        // aclList.add(acl);
        // zooKeeper.setACL(path, aclList, -1);
        return;
    }

    /**
     * 获取path节点下的儿子节点列表
     */
    public List<String> getChildren(String path) {
        if (!checkEnv(path)){
            path = environmentFlag + path;
        }
        if (client.exists(path)) {
            /**
             * 如果传入的 path 是 /im/web/cluster ，而 zk 里面的内容如下
             * /im
             *   /web
             *     /cluster
             *       /node1       （子节点1）
             *       /node2       （子节点2）
             *       /node3       （子节点3）
             *         /subnode1  （孙子节点，不会被返回）
             * 返回的就是 ["node1", "node2", "node3"],  节点不存在的情况往外抛ZkNoNodeException，节点存在但是没有子节点，则返回一个空的 List（new ArrayList<>()
             */
            return client.getChildren(path);
        }
        log.info("zookeeper NoNode exists for :{}", path);
        return new ArrayList<>();
    }

    private boolean checkEnv(String path) {
        return path.startsWith(environmentFlag);
    }

    private String getZkCluster() {
        String zooKeeperCluster = System.getProperty("config.zkCluster");
        if (StrUtil.isEmpty(zooKeeperCluster)) {
            // 测试环境
            if (EnvEnum.isInte()) {
                zooKeeperCluster = "10.61.153.47:2181";
            } else if (EnvEnum.isMoni()) {// 模拟环境
                zooKeeperCluster = "10.66.100.243:2181,10.66.100.243:2182,10.66.100.243:2183";
            } else if (EnvEnum.isPre()) {// 预发布环境
                zooKeeperCluster = "10.66.100.243:2181,10.66.100.243:2182,10.66.100.243:2183";
            } else if (EnvEnum.isProd()) {// 生产环境
                zooKeeperCluster = "10.66.100.107:2181,10.66.100.177:2181,10.66.100.102:2181,10.66.102.84:2181,10.66.102.132:2181";
            } else if (EnvEnum.isCmShow()) {// 豹来电cm-show生产环境
                zooKeeperCluster = "10.46.122.54:2181,10.46.122.134:2181,10.46.122.136:2181";
            } else if (EnvEnum.isMeast()) {// 中东数据中心测试生产环境
                zooKeeperCluster = "52.58.139.118:2181";
            } else {
                zooKeeperCluster = "10.60.82.178:2181,10.60.82.179:2181,10.60.82.180:2181";
            }
        }
        log.info("zooKeeperCluster: {}, getEnvironmentFlag(): {}", zooKeeperCluster, EnvEnum.getEnvironmentFlag());
        return zooKeeperCluster;
    }

    /**
     * 私钥路径,默认当前用户.ssh目录下的id_rsa 允许某些没有加密节点的应用存在，则这些应用不必有私钥，所以不放在init里初始化
     *
     */
    private String getPrivateKeyFilePath() {
        String pkFile = System.getProperty("config.privateKeyFile");
        pkFile = StrUtil.isEmpty(pkFile)? privateKeyFile : pkFile;
        File file = new File(pkFile);
        if (!file.exists()) {
            throw new RuntimeException("privateKeyFile not exist:" + privateKeyFile);
        }
        return pkFile;
    }

    /**
     * 公钥路径,默认当前用户.ssh目录下的id_rsa
     *
     */
    private String getPublicKeyFilePath() {
        if (StrUtil.isNotEmpty(publicKeyFile)) {
            return publicKeyFile;
        }
        // 从JVM的系统属性中取值里取 config.publicKeyFile 对应的 value
        String pkFile = System.getProperty("config.publicKeyFile");
        pkFile = StrUtil.isEmpty(pkFile) ? publicKeyFile : pkFile;
        File file = new File(pkFile);
        if (!file.exists()) {
            return "";
        }
        return pkFile;
    }

    /**
     * 删除节点
     */
    public void delete(String path){
        try {
            if(!checkEnv(path)){
                path = environmentFlag + path;
            }
            if(isEncrypted(path) && !isOp){
                throw new RuntimeException("NoAuth to delete path:" + path);
            }
            if(client.exists(path) && !client.delete(path)){
                throw new RuntimeException("zk delete node failed:" + path);
            }
        } catch (Exception e) {
            log.error("zookeeper client  delete error!", e);
            throw new RuntimeException("delete exception:" + e.getMessage());
        }
    }

    /**
     * 判断节点是否加密
     */
    public boolean isEncrypted(String path) throws KeeperException, InterruptedException {
        List<ACL> list = client.getZooKeeper().getACL(path, new Stat());
        if (CollectionUtil.isNotEmpty(list)) {
            for (ACL acl : list) {
                Id id = acl.getId();
                if (scheme.equals(id.getScheme()) && "chanjetzk".equals(id.getId().split(":")[0])
                        && ZooDefs.Perms.READ == acl.getPerms()) {
                    return true;
                }
            }
        }
        return false;
    }

    public String decrypt(byte[] b){
        if (b == null) {
            return null;
        }
        try {
            return RSACryptoUtils.decryptByPrivateKey(new String(b, StandardCharsets.UTF_8), getPrivateKeyFilePath());
        }catch (Exception e){
            log.error("decrypt error!", e);
            throw new RuntimeException("decrypt exception:" + e.getMessage());
        }
    }

    public String encrypt(String value){
        if(StrUtil.isEmpty(this.publicKeyFile)){
            throw new RuntimeException("no publicKeyFile found.");
        }
        try {
            return RSACryptoUtils.encryptByPublicKey(value, this.publicKeyFile);
        } catch (Exception e){
          log.error("encrypt error!", e);
          throw new RuntimeException("decrypt exception:", e);
        }
    }

    public boolean registerInCluster(String path, String ip){
        String newPath = path + "/" + ip;
        log.info(">>>>> start register {} in cluster <<<<<", newPath);
        setPathData(path, null);
        boolean b = createEphemeral(newPath, ip);
        if(b){
            log.info("Servers: {}", getChildren(path));
            log.info(">>>>> register server {} ok <<<<<", ip);
        }
        return b;
    }

    /**
     * 创建临时节点
     */
    public boolean createEphemeral(String path, String value) {
        if (!checkEnv(path)){
            path = environmentFlag + path;
        }
        boolean b = false;
        try {
            client.createEphemeral(path, value.getBytes(StandardCharsets.UTF_8));
            b = true;
        } catch (Exception e) {
            log.error("!!!! register {} in cluster error !!!", path, e);
        }
        return b;
    }

    /**
     * 节点设置值
     *
     * @param path  路径
     * @param value 值
     */
    public void setPathData(String path, String value) {
        if (!checkEnv(path))
            path = environmentFlag + path;
        this.addNode(path);
        client.writeData(path, value == null ? null : value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 节点设置值
     *
     * @param path        路径
     * @param value       值
     * @param isEncrypted 是否加密，只有运维有公钥可以执行加密操作
     */
    public void setPathData(String path, String value, boolean isEncrypted) {
        if (!checkEnv(path))
            path = environmentFlag + path;
        this.addNode(path, isEncrypted);
        value = isEncrypted ? encrypt(value) : value;
        client.writeData(path, value == null ? null : value.getBytes(Charset.forName("UTF-8")));
    }

    public void subscribeDataChange(String path, IZkDataListener listener){
        if(EnvEnum.isUnit()){
            return;
        }
        if(!checkEnv(path)){
            path = environmentFlag + path;
        }
        client.subscribeDataChanges(path, listener);
    }

    public void subscribeChildChanges(String path, IZkChildListener listener){
        if(EnvEnum.isUnit()){
            return;
        }
        if(!checkEnv(path)){
            path = environmentFlag + path;
        }
        client.subscribeChildChanges(path, listener);
    }

    public String getValue(String path) {
        if(EnvEnum.isUnit()){
            return "";
        }
        if(!checkEnv(path)){
            path = environmentFlag + path;
        }
        try {
            if(client.exists(path)){
                byte[] data = client.readData(path);
                if(data == null){
                    return null;
                }
                if(isEncrypted(path)){
                    return decrypt(data);
                }else{
                    return new  String(data, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e){
            log.error("get value error!", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * 判断数据节点是否存在
     */
    public boolean exists(String path) {
        if(!checkEnv(path)){
            path = environmentFlag + path;
        }
        return client.exists(path);
    }
}

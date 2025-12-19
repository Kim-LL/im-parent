package com.paas.im.tool.data;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.paas.im.dao.*;
import com.paas.im.enums.DatabaseEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 初始化数据源连接池
 */
@Slf4j
public class DataSourceManager {

    private static class DataSourceManagerHolder {
        private static final DataSourceManager INSTANCE = new DataSourceManager();
    }

    public static DataSourceManager getInstance() {
        return DataSourceManagerHolder.INSTANCE;
    }

    /**
     * 基础数据源连接池
     */
    private static final Map<String, BaseDAO> dataSourceMap = new ConcurrentHashMap<>();

    /**
     * 管理 chatMsg 数据源连接池
     * key: appId
     */
    private static final Map<String, ChatMsgDAO> chatMsgDataSourceMap =  new ConcurrentHashMap<>();

    /**
     * 管理 sessionDAO 数据源连接池
     * key: appId
     */
    private static final Map<String, SessionDAO> sessionMsgDataSourceMap =  new ConcurrentHashMap<>();

    /**
     * 管理 GroupChatDAO 数据源连接池
     * key: appId
     */
    private static Map<String, GroupManageDAO> groupChatMsgDataSourceMap =  new ConcurrentHashMap<>();

    /**
     * 管理 UserRelationDAO 数据源连接池
     * key: appId
     */
    private static final Map<String, UserRelationDAO> userRelationDataSourceMap =  new ConcurrentHashMap<>();

    /**
     * 处理定时任务线程池
     */
    private static final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(
            1,
            Thread.ofVirtual().name("s:v:t-", 1).factory()
    );

    /**
     * 定时执行策略:暂定5分钟执行一次
     */
    private static final int delay = 5 * 60 * 1000;

    /**
     * 定时任务扫描
     */
    static {
        // 加载定时任务
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            getInstance().initDataSource();
        }, 0, delay, TimeUnit.MILLISECONDS);
    }


    private DataSourceManager() {
        initDataSource();
    }

    /**
     * 系统启动初始化
     * 1. 系统启动时，初始化一次，
     * 2. 之后每N分钟初始化一次
     */
    public void initDataSource() {
        log.info("###### 开始执行数据源初始化任务 ######");
        try {
            // 获取collection 为 database_config 的数据库配置数据
            List<JSONObject> jsonList = AppConfigDAO.getInstance().getDataBaseConfigList();

            // 循环配置数据，进行初始化数据源
            for (JSONObject job : jsonList) {

                String uri = job.getString("uri");
                String database = job.getString("database");
                // 校验 database 是否为空,如果为空表示刚刚创建，数据库还没有创建成功
                if(StrUtil.isEmpty(database)){
                    continue;
                }
                // 校验uri 是否为空,如果为空表示刚刚创建，数据库还没有创建成功
                if(StrUtil.isEmpty(uri)){
                    continue;
                }
                String appId = job.getString("appId");
                String dcId = job.getString("dcId");
                int port = job.getIntValue("port");
                String username = job.getString("username");
                String password = job.getString("password");
                long status = job.getLongValue("status");
                String databaseConfig = job.getString("database_config");
                String version = job.getString("version");
                log.info("########## query datasource pool ,appId: {},uri: {},port: {}," +
                                "username: {}, password: {}, database: {}",
                        appId, uri, port, username, password, database);

                //================================step1:初始化数据库数据源连接=====================================
                //根据appId 数据库名称 获取 BaseDao 基础数据源，连接数据库使用
                BaseDAO baseDAO = getBaseDataSourceByAppId(appId, database);
                if(baseDAO == null){
                    //初始化基础数据源
                    baseDAO = BaseDAO.getDataSourceInstance(uri, port, username, password, appId, database);
                    String baseDAOKey = getBaseDAOKey(appId, database);
                    //存储基础数据源
                    dataSourceMap.put(baseDAOKey, baseDAO);
                }

                //================================step2:初始化chat数据库ChatMsgDAO===============================
                if(database.equals(DatabaseEnum.CHAT_MSG.getName())){
                    //初始化 ChatMsgDAO
                    ChatMsgDAO chatMsgDAO = getChatMsgDataSourceByAppId(appId);
                    if(chatMsgDAO != null){
                        continue;
                    }
                    //获取 sessionDAO
                    SessionDAO sessionDAO = getSessionDataSourceByAppId(appId);
                    //初始化 ChatMsgDAO
                    chatMsgDAO = new ChatMsgDAO();
                    chatMsgDAO.initAwsDataSource(baseDAO, sessionDAO);
                    //初始化chatmsg
                    chatMsgDataSourceMap.put(appId, chatMsgDAO);
                    // 更新数据源为初始化数据源
                    updateDataSource(status, dcId, appId, uri, username, port, password, databaseConfig, version,
                            database);
                }else if(database.equals(DatabaseEnum.SESSION.getName())){
                    //============================== step3:初始化session数据库SessionMsgDAO ==============================
                    SessionDAO sessionDAO = getSessionDataSourceByAppId(appId);
                    if(sessionDAO != null){
                        continue;
                    }
                    sessionDAO = new SessionDAO();
                    sessionDAO.initDataSource(baseDAO);
                    // 初始化 session 数据源
                    sessionMsgDataSourceMap.put(appId, sessionDAO);


                    updateDataSource(status, dcId, appId, uri, username, port, password, databaseConfig,
                            version, database);
                } else if (database.equals(DatabaseEnum.GROUP_CHAT.getName())) {
                    //初始化 groupChatDAO
                    GroupManageDAO groupManageDAO = getGroupChatDataSourceByAppId(appId);
                    if(groupManageDAO != null){
                        continue;
                    }
                    groupManageDAO = new GroupManageDAO();
                    groupManageDAO.initDatabase(baseDAO);
                    //初始化 GroupChat 数据源
                    groupChatMsgDataSourceMap.put(appId, groupManageDAO);
                    log.info("Success init GroupChat datasource pool, appId: {}, uri: {}, port: {}, username: {}, password: {}, database: {} , dataSourceMapSize: {}",
                            appId, uri, port, username, password, database, dataSourceMap.size());
                    // 更新 collection: database_config 下的数据，只更新 dcId 和 database 这两个属性值
                    updateDataSource(status, dcId, appId, uri, username, port,
                            password, databaseConfig, version, database);
                }
            }
            // 初始化特殊配置
            for(Map.Entry<String, ChatMsgDAO> entry : chatMsgDataSourceMap.entrySet()){
                // 获取 SessionDAO
                SessionDAO sessionDAO = getSessionDataSourceByAppId(entry.getKey());
                entry.getValue().initAwsDataSource(sessionDAO);
                log.info("Success init ChatMsgDAO And SessionDAO appId:{}",
                        entry.getKey());
            }

        }catch (Exception e){
            log.error("数据源初始化出错：{}", e.getMessage(), e);
        }
    }

    public BaseDAO getBaseDataSourceByAppId(String appId, String database){
        if(existDataSourceByAppId(appId, database)){
            String baseDAOKey = getBaseDAOKey(appId, database);
            return dataSourceMap.get(baseDAOKey);
        }
        log.info("###### appId:{},database:{} datasource not exist!!!", appId, database);
        return null;
    }

    public ChatMsgDAO getChatMsgDataSourceByAppId(String appId){
        if(!existDataSourceByAppId(appId, DatabaseEnum.CHAT_MSG.getName()) || chatMsgDataSourceMap.containsKey(appId)){
            log.info("###### appId:{} chat datasource not exist!!!", appId);
            return null;
        }
        return chatMsgDataSourceMap.get(appId);
    }

    public SessionDAO getSessionDataSourceByAppId(String appId){
        if(!existDataSourceByAppId(appId, DatabaseEnum.SESSION.name()) || !sessionMsgDataSourceMap.containsKey(appId)){
            log.info("###### appId:{} session datasource not exist!!!", appId);
            return null;
        }
        return sessionMsgDataSourceMap.get(appId);
    }

    /**
     * 通过应用 appId 获取 GroupChatDAO 数据源
     */
    public GroupManageDAO getGroupChatDataSourceByAppId(String appId){
        if(!existDataSourceByAppId(appId, DatabaseEnum.GROUP_CHAT.name()) ||
                !userRelationDataSourceMap.containsKey(appId)){
            log.info("appId:{} group chat datasource not exist!!!", appId);
            return null;
        }
        return groupChatMsgDataSourceMap.get(appId);
    }


    public boolean existDataSourceByAppId(String appId, String database){
        String baseDAOKey = getBaseDAOKey(appId, database);
        return dataSourceMap.containsKey(baseDAOKey);
    }

    public String getBaseDAOKey(String appId, String database){
        return appId + "-" + database;
    }

    /**
     * 更新数据原状态为初始化数据源状态
     * status:
     */
    public void updateDataSource(long status, String dcId,
                                 String appId, String uri, String username, int port, String password,
                                 String databaseConfig, String version, String database){
        if(status != 2L){
            AppConfigDAO.getInstance().updateDatabaseConfig(dcId, appId, uri, port, username, password,
                    databaseConfig, version, 2, 1, DatabaseEnum.valueOf(database));
        }
    }
}

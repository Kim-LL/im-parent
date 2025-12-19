package com.paas.im.dao;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.paas.im.constant.Constants;
import com.paas.im.enums.DatabaseEnum;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class AppConfigDAO {

    private static final String DATABASE_CONFIG = "database_config";

    private static final BaseDAO baseDAO = BaseDAO.getInstance(Constants.APP_CONFIG_MONGO);

    private static class  SingletonHolder {
        private static final AppConfigDAO INSTANCE = new AppConfigDAO();
    }

    public static AppConfigDAO getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public List<JSONObject> getDataBaseConfigList() {
        List<Document> documentList = baseDAO.getDataBaseConfigList(DATABASE_CONFIG);
        List<JSONObject> result = new ArrayList<>();
        for (Document document : documentList) {
            JSONObject jsonObject = JSON.parseObject(document.toJson());
            result.add(jsonObject);
        }
        return result;
    }

    public static Document getDocument(String dcId, String appId, String uri, int port, String username, String password,
                                       String version, String databaseConfig, int status, int type, String database){
        Document document = new Document();
        if(StrUtil.isNotEmpty(appId)){
            document.put("appId", appId);
        }
        if(StrUtil.isNotEmpty(dcId)){
            document.put("dcId", dcId);
        }
        if(StrUtil.isNotEmpty(uri)){
            document.put("uri", uri);
        }
        document.put("port", port);
        if(StrUtil.isNotEmpty(username)){
            document.put("username", username);
        }
        if(StrUtil.isNotEmpty(password)){
            document.put("password", password);
        }
        if(StrUtil.isNotEmpty(version)){
            document.put("version", version);
        }
        if(StrUtil.isNotEmpty(databaseConfig)){
            document.put("database_config", databaseConfig);
        }
        document.put("status", status);
        document.put("type", type);
        if(StrUtil.isNotEmpty(database)){
            document.put("database", database);
        }
        document.put("create_time", System.currentTimeMillis());
        return document;
    }

    /**
     *
     * @param dcId 			  数据库id
     * @param port 			  数据库端口
     * @param appid			  应用id
     * @param url             数据库url
     * @param username        账户名
     * @param password        密码
     * @param status          状态
     * @param databaseConfig  数据库配置
     * @param version         数据库版本
     * @param type            0 免费版 1 收费版
     * @param database        数据库名称 chat/session
     */
    public int updateDatabaseConfig(String dcId, String appid, String url, int port, String username,
                                    String password, String version, String databaseConfig, Integer status,
                                    Integer type, DatabaseEnum database){
        Document update = getDocument(dcId, appid, url, port, username, password, version, databaseConfig, status, type,
                database.getName());

        Document query = new Document();
        query.put("dcId", dcId);
        query.put("database", database.getName());
        return baseDAO.updateOrAdd(appid, query, update, DATABASE_CONFIG);
    }
}

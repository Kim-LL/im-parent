package com.paas.im.dao;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.paas.im.tool.mongo.DBPool;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BaseDAO {

    private DBPool dbPool;

    public interface Callback<M, N> {

        /**
         * @return 0 继续, 1 不进行, -1 失败
         */
        int onCall(int cmd, DBObject obj, M m, N n, Object exObj);
    }

    public interface FindCallback {

        boolean stop = false;

        /**
         * @return 1 外部继续, 0 终止, 但是成功了, -1 失败, 内部转换失败
         */
        int parseFirst(DBObject obj);

        int parseSecond(BaseDAO dao, DBObject obj, int dbIndex, String tablePrefix, int tableIndex);
    }

    private static final Map<String, BaseDAO> map = new ConcurrentHashMap<>();

    public MongoClient getTranslationClient(String dbKey){
        return dbPool.getClient(dbKey);
    }

    private BaseDAO(String zkPath){
        dbPool = DBPool.getInstance(zkPath);
    }

    private BaseDAO(String uri, int port, String username, String password, String dbName){
        dbPool = DBPool.getInstance(uri, port, username, password, dbName);
    }

    // "/app/config/mongo"
    public synchronized static BaseDAO getInstance(String zkPath){
        BaseDAO baseDAO = map.get(zkPath);
        if(baseDAO == null){
            baseDAO = new BaseDAO(zkPath);
            map.put(zkPath, baseDAO);
        }
        return baseDAO;
    }

    public synchronized static BaseDAO getDataSourceInstance(String uri, int port, String userName, String password,
                                                             String appId, String dbName) {
        BaseDAO instance = map.get(appId + "-" + dbName);
        if (instance == null) {
            instance = new BaseDAO(uri, port, userName, password, dbName);
            map.put(appId + "-" + dbName, instance);
        }
        return instance;
    }

    public List<Document> getDataBaseConfigList(String collection) {
        MongoCollection<Document> coll = dbPool.getCollection(System.currentTimeMillis() + "", collection);
        try(MongoCursor<Document> cursor = coll.find().iterator()){
            // 获取指定集合中的全部文档，以DBObject数组的形式返回
            // 如果数据量大，直接 OOM
            List<Document> list = new ArrayList<>();
            while(cursor.hasNext()){
                Document doc = cursor.next();
                list.add(doc);
            }
            return list;
        }catch (Exception e){
            log.error("getDataBaseConfigList", e);
        }
        return null;
    }

    /**
     * 修改对象，当对象不存在的时候创建
     */
    public int updateOrAdd(String dbKey, Document query, Document update, String collectionName){
        int result = -1;
        try {
            MongoCollection<Document> collection = dbPool.getCollection(dbKey, collectionName);
            // upsert(true) 无匹配的时候新增
            UpdateResult updateResult = collection.updateMany(query, update, new UpdateOptions().upsert(true));
            result = (int) updateResult.getModifiedCount();
        }catch (Exception e){
            log.error("dbKey:{} update:{}, query:{}, collectionName:{}", dbKey, update, query, collectionName, e);
        }
        return result;
    }

    /**
     * 添加
     */
    public int add(String dbKey, Document obj, String collectionName){
        int result = -1;
        try {
            MongoCollection<Document> collection = dbPool.getCollection(dbKey, collectionName);
            InsertOneResult insertOneResult = collection.insertOne(obj);
            log.info("add -- dbKey: {}, collectionName: {}, id: {}", dbKey, collectionName, insertOneResult.getInsertedId());
            result = 1;
        }catch (Exception e){
            log.error("add: {}", e.getMessage(), e);
        }
        return result;
    }

}

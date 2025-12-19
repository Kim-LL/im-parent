package com.paas.im.dao;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SessionDAO {

    private BaseDAO baseDAO;

    private static final Map<String, String> MSG_USER_COLLECTION_MAP = new HashMap<>();

    //每个库最多maxTableCount张表
    private static final int MSG_USER_COLLECTION_MAX_TABLE_COUNT = 512;

    private static final String MSG_USER_COLLECTION = "msg-index";

    public SessionDAO() {
        MSG_USER_COLLECTION_MAP.clear();
        for (int i = 0; i < MSG_USER_COLLECTION_MAX_TABLE_COUNT; i++) {
            MSG_USER_COLLECTION_MAP.put(String.valueOf(i), MSG_USER_COLLECTION + "-" + i);
        }
    }

    public void initDataSource(BaseDAO baseDAO) {
        this.baseDAO = baseDAO;
    }

    public void saveChatMsg(Document document) {

        String from = document.getString("from");
        String to = document.getString("to");
        String collectionName = getCollectionNameByUserId(from);

        int add = baseDAO.add(from, document, collectionName);
        log.info("saveChatMsg 1 - from: {}, collectionName: {}, add: {}", from, collectionName, add);
        collectionName = getCollectionNameByUserId(to);
        add = baseDAO.add(to, document, collectionName);
        log.info("saveChatMsg 2 - to: {}, collectionName: {}, add: {}", to, collectionName, add);
    }

    private String getCollectionNameByUserId(String userId){
        int numCode = userId.hashCode();
        if(numCode == Integer.MIN_VALUE){
            numCode = Integer.MAX_VALUE;
        }
        int num = Math.abs(numCode);
        long i = num % MSG_USER_COLLECTION_MAP.size();
        return MSG_USER_COLLECTION_MAP.get(String.valueOf(i));
    }
}

package com.paas.im.dao;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GroupManageDAO {

    @Getter
    private BaseDAO baseDAO;

    private static final int TABLE_COUNT = 512;

    private static final int INFO_COUNT = 3;

    private static final Map<String, String> groupInfoCollection = new HashMap<>(INFO_COUNT);

    private static final String groupInfo = "group-info-";

    private static final Map<String, String> groupHasUserCollection = new HashMap<>(TABLE_COUNT);

    private static final String groupHasUser = "group-has-user-";

    private static final Map<String, String> userHasGroupCollection = new HashMap<>(TABLE_COUNT);

    private static final String userHasGroup = "user-has-group-";

    private static final Map<String, String> groupAttachCollection = new HashMap<>(TABLE_COUNT);

    private static final String groupAttach = "group-attach-";

    static {
        for (int i = 0; i < TABLE_COUNT; i++) {
            groupHasUserCollection.put(String.valueOf(i), groupHasUser + i);
            userHasGroupCollection.put(String.valueOf(i), userHasGroup + i);
            groupAttachCollection.put(String.valueOf(i), groupAttach + i);
        }
        for (int i = 0; i < INFO_COUNT; i++) {
            groupInfoCollection.put(String.valueOf(i), groupInfo + i);
        }
    }

    public void initDatabase(BaseDAO baseDAO) {
        this.baseDAO = baseDAO;
    }

    private String getGroupInfoCollection(String appId, String groupId) {
        String key = appId + groupId;
        int index = Math.abs(key.hashCode()) % INFO_COUNT;
        return getGroupInfoCollection(index);
    }

    private String getGroupHasInfoCollection(String appId, String groupId) {
        String key = appId + groupId;
        int index = Math.abs(key.hashCode()) % INFO_COUNT;
        String collection = groupHasUserCollection.get(String.valueOf(index));
        log.info("GroupHasUserCollection : {}", collection);
        return collection;
    }

    private String getUserHasGroupCollection(String appid, String userId){
        String key = appid + userId;
        int index = Math.abs(key.hashCode()) % TABLE_COUNT;
        String collection = userHasGroupCollection.get(String.valueOf(index));
        log.info("UserHasGroupCollection : {}", collection);
        return collection;
    }

    private String getGroupAttachCollection(String appid, String groupId) {
        String key = appid + groupId;
        int index = Math.abs(key.hashCode()) % TABLE_COUNT;
        String collection = groupAttachCollection.get(String.valueOf(index));
        log.info("GroupAttachCollection : {}", collection);
        return collection;
    }

    private String getGroupInfoCollection(int num) {
        String collection = groupInfoCollection.get(String.valueOf(num));
        log.info("GroupInfoCollection: {}", collection);
        return collection;
    }


}

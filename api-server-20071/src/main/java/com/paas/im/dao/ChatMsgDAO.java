package com.paas.im.dao;

import com.paas.im.enums.DatabaseEnum;
import com.paas.im.model.proto.MessageBuf;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ChatMsgDAO {

    private BaseDAO baseDAO;

    //zk上的库
    private SessionDAO sessionDAO;

    //aws 厂商数据源
    private SessionDAO sessionDAODataSource = null;

    private static final Map<String, String> CHAT_MSG_COLLECTION_MAP = new HashMap<>();

    //每个库最多 MAX_COLLECTION_COUNT 张表
    private static final int MAX_COLLECTION_COUNT = 512;

    public ChatMsgDAO() {
        CHAT_MSG_COLLECTION_MAP.clear();
        for(int i = 0; i < MAX_COLLECTION_COUNT; i++){
            CHAT_MSG_COLLECTION_MAP.put(String.valueOf(i), DatabaseEnum.CHAT_MSG.getName() + "-" + i);
        }
    }

    public void initAwsDataSource(BaseDAO baseDAO, SessionDAO sessionDAO) {
        this.baseDAO = baseDAO;
        this.sessionDAO = sessionDAO;
    }

    public void initAwsDataSource(SessionDAO sessionDAO) {
        if(sessionDAODataSource == null){
            this.sessionDAO = sessionDAO;
            this.sessionDAODataSource = sessionDAO;
        }
    }

    /**
     * 保存消息
     */
    public void saveMessage(MessageBuf.IMMessage message) {

        Document document = new Document();
        String from = message.getFrom();
        String to = message.getTo();
        String msgId = String.valueOf(message.getMsgId());
        document.put("msgId", msgId);
        document.put("sequence", message.getSequence());
        document.put("from", from);
        document.put("to", to);
        document.put("appId", message.getAppId());
        document.put("type", message.getType());
        document.put("createDateTime", Calendar.getInstance().getTime().getTime());
        // 通过 from 和 to 分别存一份
        this.sessionDAO.saveChatMsg(document);
        // 通过 msgId 存一份
        this.baseDAO.add(msgId, document, getCollectionNameByMsgId(msgId));
    }

    private String getCollectionNameByMsgId(String msgId) {
        int numcode = msgId.hashCode();
        if (numcode == Integer.MIN_VALUE) {
            numcode = Integer.MAX_VALUE;
        }
        int num = Math.abs(numcode);
        int i = num % CHAT_MSG_COLLECTION_MAP.size();
        // 获取表名
        return CHAT_MSG_COLLECTION_MAP.get(String.valueOf(i));
    }
}

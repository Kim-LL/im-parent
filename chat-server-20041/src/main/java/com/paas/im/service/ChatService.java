package com.paas.im.service;

import com.paas.im.model.proto.MessageBuf;

import java.util.Set;

public interface ChatService {

    Set<String> getServers(String cluster);

    boolean saveMessage(MessageBuf.IMMessage message);
}

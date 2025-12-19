package com.paas.im.service.impl;

import com.paas.im.handler.*;
import com.paas.im.model.proto.MessageBuf;
import com.paas.im.service.HolderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class HolderServiceImpl implements HolderService {

    @Resource
    private AckHandler ackHandler;

    @Resource
    private ChatHandler chatHandler;

    @Resource
    private GroupChatHandler groupChatHandler;

    @Resource
    private GroupSysHandler groupSysHandler;

    @Resource
    private KeepaliveHandler keepaliveHandler;

    @Resource
    private LoginHandler loginHandler;

    @Resource
    private RoomHandler roomHandler;

    @Override
    public IDataHandler getHandler(int cmd) {
        return switch (cmd) {
            case MessageBuf.TypeEnum.LOGIN_VALUE -> loginHandler;
            case MessageBuf.TypeEnum.KEEPALIVE_VALUE -> keepaliveHandler;
            case MessageBuf.TypeEnum.ACK_VALUE -> ackHandler;
            case MessageBuf.TypeEnum.ROOM_VALUE -> roomHandler;
            case MessageBuf.TypeEnum.CHAT_VALUE -> chatHandler;
            case MessageBuf.TypeEnum.CHAT_GROUP_VALUE -> groupChatHandler;
            case MessageBuf.TypeEnum.SYSTEM_VALUE -> groupSysHandler;
            default -> null;
        };
    }
}

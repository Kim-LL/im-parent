package com.paas.im.service.impl;

import com.paas.im.handler.*;
import com.paas.im.model.proto.MessageBuf;
import com.paas.im.service.HolderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HolderServiceImpl implements HolderService {

    @Resource
    private AckHandler ackHandler;

    @Resource
    private ChatHandler chatHandler;

    @Resource
    private ChatUnreadMessageHandler chatUnreadMessageHandler;

    @Resource
    private KeepaliveHandler keepaliveHandler;

    @Resource
    private LoginHandler loginHandler;

    @Override
    public IBaseHandler getHandler(int cmd) {
        return switch (cmd) {
            case MessageBuf.TypeEnum.ACK_VALUE -> ackHandler;
            case MessageBuf.TypeEnum.CHAT_VALUE -> chatHandler;
            case MessageBuf.SubTypeEnum.CHAT_UNREAD_MESSAGE_VALUE -> chatUnreadMessageHandler;
            case MessageBuf.TypeEnum.KEEPALIVE_VALUE -> keepaliveHandler;
            case MessageBuf.TypeEnum.LOGIN_VALUE -> loginHandler;
            default -> null;
        };
    }
}

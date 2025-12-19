package com.paas.im.handler;

import com.paas.im.model.proto.Packet;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KeepaliveHandler implements IBaseHandler {

    @Override
    public void execute(ChannelHandlerContext ctx, Packet packet) {

    }
}

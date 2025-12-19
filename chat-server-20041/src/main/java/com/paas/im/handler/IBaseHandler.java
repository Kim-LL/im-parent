package com.paas.im.handler;

import com.paas.im.model.proto.Packet;
import io.netty.channel.ChannelHandlerContext;

public interface IBaseHandler {

    /**
     * 业务操作
     */
    void execute(ChannelHandlerContext ctx, Packet packet);
}

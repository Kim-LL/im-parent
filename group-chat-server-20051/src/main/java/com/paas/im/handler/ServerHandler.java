package com.paas.im.handler;

import com.paas.im.model.proto.Packet;
import com.paas.im.service.HolderService;
import com.paas.im.tool.channel.AttributeKeys;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Resource
    private HolderService holderService;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            Packet packet = (Packet) msg;
            byte cmd = packet.getCmd();

            IDataHandler handler = holderService.getHandler(cmd);
            if(handler != null){
                handler.execute(ctx, packet);
            }else {
                log.error("handler error! Unknown cmd: {}, packet: {}", cmd, packet);
            }
        }catch (Exception e){
            log.error("IDataHandler error:{}", e.getMessage(), e);
        }
    }

    /**
     * 超过心跳时间，server为从该Channel收到数据，触发该事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 相隔指定的时间没有从该channel读到数据，认为该连接失效，进行主动注销操作
        log.info("心跳过期:  channel={} ", ctx.channel());
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.READER_IDLE) {
                /*读超时*/
                log.warn("READER_IDLE 读超时");
                ctx.disconnect();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                /*写超时*/
                log.warn("WRITER_IDLE 写超时");
            } else if (event.state() == IdleState.ALL_IDLE) {
                /*总超时*/
                log.warn("ALL_IDLE 总超时");
            }
        }
        // 关闭当前Channel
        ctx.close().sync();
    }

    /**
     * exceptionCaught 会比 channelInactive 先触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String userId = ctx.channel().attr(AttributeKeys.USER_ID).get();
        try {
            if(userId != null){
                log.info("连接异常 userId: {}, channel={} ", userId, ctx.channel());
            }
        }catch (Exception e){
            log.error("exceptionCaught -- Exception:{}", e.getMessage(), e);
        }catch (Throwable e){
            log.error("exceptionCaught -- Throwable:{}", e.getMessage(), e);
        } finally{
            ctx.channel().close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String userId = ctx.channel().attr(AttributeKeys.USER_ID).get();
        try {
            if(userId != null){
                log.info("连接非活动状态 userId: {}  channel:{}  ", userId, ctx.channel());
            }
            ctx.close().sync();
        }catch (Exception e){
            // 关闭当前Channel
            ctx.close().sync();
            log.error("channelInactive  user: {}, channel: {}, error: {}", userId, ctx.channel(), e);
        }
    }
}

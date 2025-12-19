package com.paas.im.handler;

import com.paas.im.model.proto.MessageBuf;
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

import java.util.concurrent.TimeUnit;

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
            byte subType = packet.getSubType();

            // 校验业务类型 是否为拉取历史消息
            if(cmd == MessageBuf.TypeEnum.CHAT_VALUE){
                MessageBuf.IMMessage receiveMessage = MessageBuf.IMMessage.parseFrom(packet.getBody());
                if(receiveMessage.getSubType() == MessageBuf.SubTypeEnum.PULL_HISTORY_MSG_VALUE){
                    // cmd
                    cmd = MessageBuf.SubTypeEnum.PULL_HISTORY_MSG_VALUE;
                    log.info("当前为拉取历史消息..... pac:{}", packet);
                }
            }

            IBaseHandler handler;
            if(cmd == MessageBuf.TypeEnum.CHAT_VALUE && subType == MessageBuf.SubTypeEnum.CHAT_UNREAD_MESSAGE_VALUE){
                handler = holderService.getHandler(subType);
            }else {
                handler = holderService.getHandler(cmd);
            }
            if(handler != null){
                handler.execute(ctx, packet);
            } else {
                log.info("handler error! Unknown cmd: {}", cmd);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {}
    }

    /**
     * 超过心跳时间，server为从该Channel收到数据，触发该事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("心跳过期:  channel={}", ctx.channel());

        if(evt instanceof IdleStateEvent event){
            if(event.state() == IdleState.READER_IDLE){
                /*读超时*/
                log.warn("READER_IDLE 读超时");
                ctx.disconnect();
            }else if(event.state() == IdleState.WRITER_IDLE){
                /*写超时*/
                log.warn("WRITER_IDLE 写超时");
            }else if(event.state() == IdleState.ALL_IDLE){
                /*总超时*/
                log.warn("ALL_IDLE 总超时");
            }

            ctx.close().sync();
        }
    }

    /**
     * 连接发生异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String userId = ctx.channel().attr(AttributeKeys.USER_ID).get();
        try {
            if(userId != null){

            }
        } catch (Exception e) {
            log.error("异常信息: {}", e.getMessage(), e);
        } catch (Throwable t) {
            //捕获throwable,防止某个连接异常导致整个链路不可用
            log.error("遇到问题! t: {}", t.getMessage(), t);
            TimeUnit.SECONDS.sleep(1);
        } finally {
            ctx.close().sync();
        }
    }

    /**
     *  从活跃状态变为非活跃状态时被触发，也就是通道的连接彻底关闭时
     *  服务端在 channelRead 里调用 ctx.channel().close()，客户端主动关闭，客户端因为网络被动关闭 等都会触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String userId = ctx.channel().attr(AttributeKeys.USER_ID).get();
        try {
            if(userId != null){
                log.info("channelInactive:  userId: {}, channel:{}", userId, ctx.channel());
            }
        }catch (Exception e){
            log.error("channelInactive error   channel={}  e:{}", ctx.channel(), e);
        }finally {
            ctx.close().sync();
        }
    }
}

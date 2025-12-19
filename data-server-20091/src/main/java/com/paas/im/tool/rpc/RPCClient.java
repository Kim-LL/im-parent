package com.paas.im.tool.rpc;

import com.paas.im.codec.MessageDecoder;
import com.paas.im.codec.MessageEncoder;
import com.paas.im.constant.Constants;
import com.paas.im.model.proto.MessageBuf;
import com.paas.im.model.proto.Packet;
import com.paas.im.model.proto.RPCBuf;
import com.paas.im.tool.channel.AttributeKeys;
import com.paas.im.tool.zookeeper.ZKConfigManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Rpc 连接登录
 */
@Slf4j
public class RPCClient {

    private final String rpcServer;
    private final int rpcPort;
    private final int index;
    private Bootstrap bootstrap;
    private static EventLoopGroup group = new NioEventLoopGroup(5);;
    private Channel channel;

    public RPCClient(String rpcServer, int rpcPort, int index) {
        this.rpcServer = rpcServer;
        this.rpcPort = rpcPort;
        this.index = index;
        connection();
    }

    private void connection() {
        int rpcKeepaliveTime = ZKConfigManager.getInstance().getImConfig().getRpcKeepaliveTime();
        try {
            // 用客户端链接
            bootstrap = new Bootstrap();
            bootstrap.group(group);

            bootstrap.channel(NioSocketChannel.class);

            //开启 TCP 的保活机制，作用是检测长时间空闲的连接是否仍然有效（比如对方是否宕机、网络是否中断）
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            //允许复用本地的 IP 地址和端口
            bootstrap.option(ChannelOption.SO_REUSEADDR, true);

            //缓冲区大小 256KB
            bootstrap.option(ChannelOption.SO_RCVBUF, 256 * 1024);
            bootstrap.option(ChannelOption.SO_SNDBUF, 256 * 1024);

            //是否禁用 TCP 的 Nagle 算法, 是就是禁用，决定 TCP 是否会 “延迟发送小数据包”
            bootstrap.option(ChannelOption.TCP_NODELAY, true);

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    ChannelPipeline pipeline = socketChannel.pipeline();
                    pipeline.addLast("MessageDecoder", MessageDecoder.INSTANCE);
                    pipeline.addLast("MessageEncoder", MessageEncoder.INSTANCE);
                    pipeline.addLast("Keepalive", new IdleStateHandler(0, rpcKeepaliveTime, 0));
                    pipeline.addLast("Handler", null);
                }
            });

            final ChannelFuture channelFuture = bootstrap.connect(rpcServer, rpcPort).sync();
            channel = channelFuture.channel();
            channelFuture.addListener((ChannelFutureListener) future -> {
               if (future.isSuccess()) {
                   loginRPCServer(channel.id().toString(), rpcServer, rpcPort, index);
               }
            });
        }catch (Exception e){
            log.error("connection: {}", e.getMessage());
        }
    }

    private void loginRPCServer(String channelId, String rpcServer, int rpcPort,  int index) {
        RPCBuf.RPC.Builder rpcBuilder = RPCBuf.RPC.newBuilder();
        rpcBuilder.setRPCServer(rpcServer);
        rpcBuilder.setRPCPort(rpcPort);
        rpcBuilder.setIndex(index);
        rpcBuilder.setChannelId(channelId);
        String pattern = "yyyy-MM-dd HH:mm:ss";
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        rpcBuilder.setTime(DateTimeFormatter.ofPattern(pattern).format(localDateTime));

        Packet packet = new Packet(
                (byte)MessageBuf.TypeEnum.LOGIN_VALUE,
                (byte) MessageBuf.SubTypeEnum.TEXT_VALUE,
                0,
                0,
                rpcBuilder.build().toByteArray()
        );
        this.sendMessage(packet);

        log.info("开始发送RPC客户端登录消息...");

        //channel 上绑定 RPC 数据
        channel.attr(AttributeKeys.RPC_SERVER).set(rpcServer);
        channel.attr(AttributeKeys.RPC_PORT).set(rpcPort);
        channel.attr(AttributeKeys.RPC_INDEX).set(index);
        channel.attr(AttributeKeys.RPC_POOL_KEY).set(rpcServer + Constants.SEQ + rpcPort + Constants.SEQ + index);

        log.info("loginRPCServer 登录后的 channel 信息: rpcServer: {}, channel: {}", channel.attr(AttributeKeys.RPC_SERVER).get(), channel);
    }

    /**
     * 发送成功事件监听
     */
    public void sendMessage(Packet packet, ChannelFutureListener listener) {
        if(listener != null){
            channel.writeAndFlush(packet).addListener(listener);
        }else {
            channel.writeAndFlush(packet);
        }
    }

    public void sendMessage(Packet packet) {
        channel.writeAndFlush(packet);
    }
}

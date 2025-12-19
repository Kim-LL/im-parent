package com.paas.im.start;

import com.paas.im.codec.MessageDecoder;
import com.paas.im.codec.MessageEncoder;
import com.paas.im.constant.Constants;
import com.paas.im.handler.ServerHandler;
import com.paas.im.tool.data.DataManager;
import com.paas.im.tool.rpc.RPCClientManager;
import com.paas.im.tool.zookeeper.ZKRegister;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ChatStart implements SmartLifecycle {

    @Value(value = "${server.chat.reactor.main:1}")
    private int threadMainReactor;

    @Value(value = "${server.chat.reactor.sub:2}")
    private int threadSubReactor;

    @Value(value = "${server.chat.reader-idle-time:200}")
    private int readerIdleTime;

    @Resource
    private ServerHandler serverHandler;

    private ChannelFuture channelFuture;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if(running.get()) {
            // 避免重复启动
            return;
        }
        bossGroup = new NioEventLoopGroup(threadMainReactor);
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * threadSubReactor);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);

            // 采用NioServerSocketChannel类来实例化，实现基于选择器接受新连接
            bootstrap.channel(NioServerSocketChannel.class);

            // 指定的的Channel设置参数
            bootstrap.option(ChannelOption.SO_KEEPALIVE, false);
            bootstrap.option(ChannelOption.TCP_NODELAY, true); // 设置封包 使用一次大数据写操作，而不是多次小数据的写操作
            bootstrap.option(ChannelOption.SO_REUSEADDR, true); //挂起的连接


            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    ChannelPipeline pipeline = socketChannel.pipeline();
                    pipeline.addLast("MessageDecoder", MessageDecoder.INSTANCE);
                    pipeline.addLast("MessageEncoder", MessageEncoder.INSTANCE);
                    // 若在 200 秒内，当前 Channel 没有「读取到任何数据」（客户端未发消息），则触发 IdleStateEvent.READER_IDLE 事件，换句话客户端 200 秒内未发送任何数据（包括心跳包、消息），说明客户端可能断网 / 崩溃 / 失联
                    // 写空闲超时时间：设为 0 表示「禁用写空闲检测」
                    // 全空闲超时时间：设为 0 表示「禁用全空闲检测」
                    pipeline.addLast("Keepalive", new IdleStateHandler(readerIdleTime, 0, 0));
                    pipeline.addLast("Handler", serverHandler);
                }
            });

            channelFuture = bootstrap.bind(Constants.CHAT_RPC_PORT).sync();

            channelFuture.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    // bind 端口成功后，执行 initTask 方法
                    initTask();
                    running.set(true);
                }
            });
            log.info("chat server started. port：{}", Constants.CHAT_RPC_PORT);
        } catch (Exception e){
            log.error("chat server start error: {}", e.getMessage(), e);
        } finally {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        }
    }

    /**
     * Netty服务停止逻辑（由Spring触发，此时Redis资源还未被销毁）
     */
    @Override
    public void stop() {
        if(!running.get()) {
            return;
        }
        // spring 管理生命周期
        log.info("chat server stop.");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 退出
     */
    private void shutdown() {
        if (channelFuture != null) {
            channelFuture.channel().close().syncUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 加载任务
     */
    private void initTask() {
        // 将当前服务注册到 zookeeper 里，后期可以获取服务
        ZKRegister.getInstance().registerIp(Constants.CHAT_CLUSTER);

        // WebIM RPC链接池
        RPCClientManager.getInstance().syncRPCServer(Constants.WEB_IM_CLUSTER, Constants.CHAT_RPC_PORT);

        // data RPC链接池
        RPCClientManager.getInstance().syncRPCServer(Constants.DATA_CLUSTER, Constants.DATA_RPC_PORT);

        // data RPC 初始化
        DataManager.getInstance().initDataServerRPC();
    }

}

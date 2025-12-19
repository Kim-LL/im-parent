package com.paas.im.start;

import com.paas.im.codec.MessageDecoder;
import com.paas.im.codec.MessageEncoder;
import com.paas.im.constant.Constants;
import com.paas.im.handler.ServerHandler;
import com.paas.im.tool.data.DataSourceManager;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class DataStart implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    private ChannelFuture channelFuture;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Value(value = "${server.task.reactor.main:1}")
    private int threadMainReactor;

    @Value(value = "${server.task.reactor.sub:2}")
    private int threadSubReactor;

    @Value(value = "${server.task.reader-idle-time:200}")
    private int readerIdleTime;

    @Resource
    private ServerHandler serverHandler;

    @Override
    public void start() {
        if(running.get()) {
            // 避免重复启动
            return;
        }

        // 加载任务
        initTask();

        bossGroup = new NioEventLoopGroup(threadMainReactor);
        // JVM 可用的处理器核心数 * 2
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * threadSubReactor);

        try {
            //设置服务器的辅助类
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);

            //采用NioServerSocketChannel类来实例化，实现基于选择器接受新连接
            bootstrap.channel(NioServerSocketChannel.class);

            //指定的 Channel 设置参数
            bootstrap.option(ChannelOption.SO_KEEPALIVE, false);
            //设置封包 使用一次大数据写操作，而不是多次小数据的写操作，参数作用于入站和出站
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.option(ChannelOption.SO_BACKLOG, 128); //挂起的连接

            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    ChannelPipeline pipeline = socketChannel.pipeline();
                    pipeline.addLast("MessageDecoder", MessageDecoder.INSTANCE);
                    pipeline.addLast("MessageEncoder", MessageEncoder.INSTANCE);
                    pipeline.addLast("Keepalive", new IdleStateHandler(readerIdleTime, 0, 0));
                    pipeline.addLast("Handler", serverHandler);
                }
            });

            channelFuture = bootstrap.bind(Constants.DATA_RPC_PORT).sync();

        }catch (Exception e){
            log.error("Task startup error!!!!!!", e);
        } finally {
            Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().start(this::shutdown));
        }

    }

    @Override
    public void stop() {
        if(!running.get()) {
            return;
        }
        // spring 管理生命周期
        log.info("data server stop.");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 退出
     */
    public void shutdown() {
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
    public void initTask() {
        //data注册RPC长连接 IP
        ZKRegister.getInstance().registerIp(Constants.DATA_CLUSTER);

        //初始化数据库连接池
        DataSourceManager.getInstance().initDataSource();
    }
}

package com.github.netty.core;

import com.github.netty.core.util.*;
import io.netty.bootstrap.ChannelFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.PlatformDependent;

import java.net.InetSocketAddress;


/**
 * 一个抽象的netty服务端
 * @author 84215
 */
public abstract class AbstractNettyServer implements Runnable{
    protected LoggerX logger = LoggerFactoryX.getLogger(getClass());
    private String name;

    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private InetSocketAddress serverAddress;
    private boolean enableEpoll;
    private int ioThreadCount = 0;
    private int ioRatio = 100;
    private boolean running = false;

    public AbstractNettyServer(int port) {
        this(new InetSocketAddress(port));
    }

    public AbstractNettyServer(InetSocketAddress address) {
        this("", address);
    }

    public AbstractNettyServer(String preName,InetSocketAddress address) {
        super();
        this.enableEpoll = Epoll.isAvailable();
        this.serverAddress = address;
        this.name = NamespaceUtil.newIdName(preName,getClass());
    }

    public void setIoRatio(int ioRatio) {
        if(worker instanceof NioEventLoopGroup){
            ((NioEventLoopGroup) worker).setIoRatio(ioRatio);
            this.ioRatio = ioRatio;
        }else if(worker instanceof EpollEventLoopGroup){
            ((EpollEventLoopGroup) worker).setIoRatio(ioRatio);
            this.ioRatio = ioRatio;
        }
    }

    public void setIoThreadCount(int ioThreadCount) {
        this.ioThreadCount = ioThreadCount;
    }

    protected abstract ChannelHandler newInitializerChannelHandler();

    protected ServerBootstrap newServerBootstrap(){
        return new ServerBootstrap();
    }

    protected EventLoopGroup newWorkerEventLoopGroup() {
        EventLoopGroup worker;
        if(enableEpoll){
            worker = new EpollEventLoopGroup(ioThreadCount,new ThreadFactoryX("Epoll","Server-Worker"));
        }else {
            worker = new NioEventLoopGroup(ioThreadCount,new ThreadFactoryX("NIO","Server-Worker"));
        }
        return worker;
    }

    protected EventLoopGroup newBossEventLoopGroup() {
        EventLoopGroup boss;
        if(enableEpoll){
            EpollEventLoopGroup epollBoss = new EpollEventLoopGroup(1,new ThreadFactoryX("Epoll","Server-Boss"));
            epollBoss.setIoRatio(ioRatio);
            boss = epollBoss;
        }else {
            NioEventLoopGroup jdkBoss = new NioEventLoopGroup(1,new ThreadFactoryX("NIO","Server-Boss"));
            jdkBoss.setIoRatio(ioRatio);
            boss = jdkBoss;
        }
        return boss;
    }

    protected ChannelFactory<? extends ServerChannel> newServerChannelFactory() {
        ChannelFactory<? extends ServerChannel> channelFactory;
        if(enableEpoll){
            channelFactory = EpollServerSocketChannel::new;
        }else {
            channelFactory = NioServerSocketChannel::new;
        }
        return channelFactory;
    }

    @Override
    public final void run() {
        try {
            if(running){
                return;
            }

            ServerBootstrap bootstrap = newServerBootstrap();
            this.boss = newBossEventLoopGroup();
            this.worker = newWorkerEventLoopGroup();
            ChannelFactory<? extends ServerChannel> channelFactory = newServerChannelFactory();
            ChannelHandler initializerChannelHandler = newInitializerChannelHandler();

            bootstrap
                    .group(boss, worker)
                    .channelFactory(channelFactory)
                    .handler(initializerChannelHandler)
                    .childHandler(initializerChannelHandler)
                    //允许在同一端口上启动同一服务器的多个实例，只要每个实例捆绑一个不同的本地IP地址即可
                    .option(ChannelOption.SO_REUSEADDR, true)
                    //用于构造服务端套接字ServerSocket对象，标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度
//                    .option(ChannelOption.SO_BACKLOG, 1024) // determining the number of connections queued

                    //禁用Nagle算法，即数据包立即发送出去 (在TCP_NODELAY模式下，假设有3个小包要发送，第一个小包发出后，接下来的小包需要等待之前的小包被ack，在这期间小包会合并，直到接收到之前包的ack后才会发生)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    //开启TCP/IP协议实现的心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //netty的默认内存分配器
                    .childOption(ChannelOption.ALLOCATOR, ByteBufAllocatorX.INSTANCE);
//                    .childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);

            ChannelFuture channelFuture = bootstrap.bind(serverAddress);

            channelFuture.addListener(this::startAfter);
            running = true;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void stop() {
        Throwable cause = null;
        try {
            if (boss != null) {
                boss.shutdownGracefully();
            }
            if (worker != null) {
                worker.shutdownGracefully();
            }
        }catch (Throwable e){
            cause = e;
        }
        stopAfter(cause);
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        if(serverAddress == null){
            return 0;
        }
        return serverAddress.getPort();
    }

    protected void stopAfter(Throwable cause){
        //有异常抛出
        if(cause != null){
            cause.printStackTrace();
        }
        logger.info(name + " stop [port = "+getPort()+" , cause = "+cause+"]...");
    }

    protected void startAfter(Future<? super Void> future){
        //有异常抛出
        Throwable cause = future.cause();
        if(cause != null){
            PlatformDependent.throwException(cause);
        }
        logger.info("{} start (port = {}, pid = {}, os = {}) ...",
                getName(),
                getPort()+"",
                HostUtil.getPid()+"",
                HostUtil.getOsName());
    }

    @Override
    public String toString() {
        return name+"{" +
                "port=" + getPort() +
                '}';
    }

}

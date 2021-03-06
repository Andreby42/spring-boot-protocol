package com.github.netty.springboot;

import com.github.netty.core.util.ApplicationX;
import com.github.netty.protocol.DynamicProtocolChannelHandler;
import com.github.netty.protocol.mysql.client.MysqlFrontendBusinessHandler;
import com.github.netty.protocol.mysql.server.MysqlBackendBusinessHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.util.ResourceLeakDetector;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * You can configure it here
 * @author wangzihao
 * 2018/8/25/025
 */
@ConfigurationProperties(prefix = "server.netty", ignoreUnknownFields = true)
public class NettyProperties implements Serializable{
    private static final long serialVersionUID = 1L;

    /**
     * 服务端 - TCP级别最大同时在线的连接数
     */
    private int maxConnections = 10000;
    /**
     * 服务端 - 是否tcp数据包日志
     */
    private boolean enableTcpPackageLog = false;
    /**
     * 服务端 - 第一个客户端包的超时时间 (毫秒)
     */
    private long firstClientPacketReadTimeoutMs = 800;
    /**
     * 服务端 - tcp数据包日志等级(需要先开启tcp数据包日志)
     */
    private io.netty.handler.logging.LogLevel tcpPackageLogLevel = io.netty.handler.logging.LogLevel.DEBUG;

    /**
     * 服务端-IO线程数  注: (0 = cpu核数 * 2 )
     */
    private int serverIoThreads = 0;
    /**
     * 服务端-io线程执行调度与执行io事件的百分比. 注:(100=每次只执行一次调度工作, 其他都执行io事件), 并发高的时候可以设置最大
     */
    private int serverIoRatio = 100;

    /**
     * 是否禁用Nagle算法，true=禁用Nagle算法. 即数据包立即发送出去 (在TCP_NODELAY模式下，假设有3个小包要发送，第一个小包发出后，接下来的小包需要等待之前的小包被ack，在这期间小包会合并，直到接收到之前包的ack后才会发生)
     */
    private boolean tcpNodelay = false;

    /**
     * netty的内存泄漏检测级别(调试程序的时候用). 默认禁用, 不然极其耗费性能
     */
    private ResourceLeakDetector.Level resourceLeakDetectorLevel = ResourceLeakDetector.Level.DISABLED;

    /**
     * 动态协议处理器,是在进入所有协议之前的入口- 使用者可以继承它加入自己的逻辑 比如:(处理超出最大tcp连接数时的逻辑, 处理遇到不支持的协议时的逻辑等..)
     */
    private Class<?extends DynamicProtocolChannelHandler> channelHandler = DynamicProtocolChannelHandler.class;

    /**
     * HTTP协议(Servlet实现)
     */
    @NestedConfigurationProperty
    private final HttpServlet httpServlet = new HttpServlet();
    /**
     * NRPC协议
     */
    @NestedConfigurationProperty
    private final Nrpc nrpc = new Nrpc();
    /**
     * MQTT协议
     */
    @NestedConfigurationProperty
    private final Mqtt mqtt = new Mqtt();
    /**
     * RTSP协议
     */
    @NestedConfigurationProperty
    private final Rtsp rtsp = new Rtsp();

    /**
     * MYSQL代理协议
     */
    @NestedConfigurationProperty
    private final Mysql mysql = new Mysql();

    /**
     * 全局对象(类似spring容器)
     */
    private transient final ApplicationX application = new ApplicationX();

    public NettyProperties() {}

    public ApplicationX getApplication() {
        return application;
    }

    public boolean isTcpNodelay() {
        return tcpNodelay;
    }

    public void setTcpNodelay(boolean tcpNodelay) {
        this.tcpNodelay = tcpNodelay;
    }

    public ResourceLeakDetector.Level getResourceLeakDetectorLevel() {
        return resourceLeakDetectorLevel;
    }

    public void setResourceLeakDetectorLevel(ResourceLeakDetector.Level resourceLeakDetectorLevel) {
        this.resourceLeakDetectorLevel = resourceLeakDetectorLevel;
    }

    public long getFirstClientPacketReadTimeoutMs() {
        return firstClientPacketReadTimeoutMs;
    }

    public void setFirstClientPacketReadTimeoutMs(long firstClientPacketReadTimeoutMs) {
        this.firstClientPacketReadTimeoutMs = firstClientPacketReadTimeoutMs;
    }

    public Class<?extends DynamicProtocolChannelHandler> getChannelHandler() {
        return channelHandler;
    }

    public void setChannelHandler(Class<?extends DynamicProtocolChannelHandler> channelHandler) {
        this.channelHandler = channelHandler;
    }

    public LogLevel getTcpPackageLogLevel() {
        return tcpPackageLogLevel;
    }

    public void setTcpPackageLogLevel(LogLevel tcpPackageLogLevel) {
        this.tcpPackageLogLevel = tcpPackageLogLevel;
    }

    public int getServerIoThreads() {
        return serverIoThreads;
    }

    public void setServerIoThreads(int serverIoThreads) {
        this.serverIoThreads = serverIoThreads;
    }

    public int getServerIoRatio() {
        return serverIoRatio;
    }

    public void setServerIoRatio(int serverIoRatio) {
        this.serverIoRatio = serverIoRatio;
    }

    public boolean isEnableTcpPackageLog() {
        return enableTcpPackageLog;
    }

    public void setEnableTcpPackageLog(boolean enableTcpPackageLog) {
        this.enableTcpPackageLog = enableTcpPackageLog;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Nrpc getNrpc() {
        return nrpc;
    }

    public Mqtt getMqtt() {
        return mqtt;
    }

    public Rtsp getRtsp() {
        return rtsp;
    }

    public HttpServlet getHttpServlet() {
        return httpServlet;
    }

    public Mysql getMysql() {
        return mysql;
    }

    public static class HttpServlet{
        /**
         * 请求体最大字节
         */
        private int maxContentSize = 20 * 1024 * 1024;
        /**
         * 请求头每行最大字节
         */
        private int maxHeaderLineSize = 40960;
        /**
         * 请求头最大字节
         */
        private int maxHeaderSize = 81920;
        /**
         * 大于这个字节则进行分段传输,每段大小
         */
        private int maxChunkSize = 5 * 1024 * 1024;
        /**
         * 服务端 - servlet线程执行器
         */
        private Class<?extends Executor> serverHandlerExecutor = null;
        /**
         * 服务端 - servlet3异步特性。 异步dispatch的线程执行器 (默认用的是netty的IO线程) {@link #serverIoThreads}
         */
        private Class<?extends ExecutorService> asyncExecutorService = null;
        /**
         * 服务端 - servlet3的异步特性。 异步回调是否切换至新的线程执行任务, 如果没有异步嵌套异步的情况,建议开启.因为只有给前端写数据的IO损耗.
         * (设置false会减少一次线程切换, 用回调方的线程执行. 提示:tomcat是true，用新线程执行)
         */
        private boolean asyncSwitchThread = true;
        /**
         * session存储 - 是否开启本地文件存储
         */
        private boolean enablesLocalFileSession = false;

        /**
         * session存储 - session远程存储的url地址, 注: 如果不设置就不会开启
         */
        private String sessionRemoteServerAddress;

        /**
         * 每次调用servlet的 OutputStream.Writer()方法写入的最大堆字节,超出后用堆外内存
         */
        private int responseWriterChunkMaxHeapByteLength = 4096 * 10;

        /**
         * servlet文件存储的根目录。(servlet文件上传下载) 如果未指定，则使用临时目录。
         */
        private File basedir;

        /**
         * 是否开启DNS地址查询. true=开启 {@link javax.servlet.ServletRequest#getRemoteHost}
         */
        private boolean enableLookup = false;

        public boolean isAsyncSwitchThread() {
            return asyncSwitchThread;
        }

        public void setAsyncSwitchThread(boolean asyncSwitchThread) {
            this.asyncSwitchThread = asyncSwitchThread;
        }

        public Class<? extends ExecutorService> getAsyncExecutorService() {
            return asyncExecutorService;
        }

        public void setAsyncExecutorService(Class<? extends ExecutorService> asyncExecutorService) {
            this.asyncExecutorService = asyncExecutorService;
        }

        public boolean isEnableLookup() {
            return enableLookup;
        }

        public void setEnableLookup(boolean enableLookup) {
            this.enableLookup = enableLookup;
        }

        public int getMaxContentSize() {
            return maxContentSize;
        }

        public void setMaxContentSize(int maxContentSize) {
            this.maxContentSize = maxContentSize;
        }

        public int getMaxHeaderLineSize() {
            return maxHeaderLineSize;
        }

        public void setMaxHeaderLineSize(int maxHeaderLineSize) {
            this.maxHeaderLineSize = maxHeaderLineSize;
        }

        public int getMaxHeaderSize() {
            return maxHeaderSize;
        }

        public void setMaxHeaderSize(int maxHeaderSize) {
            this.maxHeaderSize = maxHeaderSize;
        }

        public int getMaxChunkSize() {
            return maxChunkSize;
        }

        public void setMaxChunkSize(int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
        }

        public Class<?extends Executor> getServerHandlerExecutor() {
            return serverHandlerExecutor;
        }

        public void setServerHandlerExecutor(Class<?extends Executor> serverHandlerExecutor) {
            this.serverHandlerExecutor = serverHandlerExecutor;
        }

        public boolean isEnablesLocalFileSession() {
            return enablesLocalFileSession;
        }

        public void setEnablesLocalFileSession(boolean enablesLocalFileSession) {
            this.enablesLocalFileSession = enablesLocalFileSession;
        }

        public String getSessionRemoteServerAddress() {
            return sessionRemoteServerAddress;
        }

        public void setSessionRemoteServerAddress(String sessionRemoteServerAddress) {
            this.sessionRemoteServerAddress = sessionRemoteServerAddress;
        }

        public int getResponseWriterChunkMaxHeapByteLength() {
            return responseWriterChunkMaxHeapByteLength;
        }

        public void setResponseWriterChunkMaxHeapByteLength(int responseWriterChunkMaxHeapByteLength) {
            this.responseWriterChunkMaxHeapByteLength = responseWriterChunkMaxHeapByteLength;
        }

        public File getBasedir() {
            return basedir;
        }

        public void setBasedir(File basedir) {
            this.basedir = basedir;
        }
    }

    public static class Nrpc {

        /**
         * RPC客户端-工作线程数   注: (0 = cpu核数 * 2 )
         */
        private int clientIoThreads = 0;

        /**
         * RPC客户端-IO线程执行调度与执行io事件的百分比. 注:(100=每次只执行一次调度工作, 其他都执行io事件), 并发高的时候可以设置最大
         */
        private int clientIoRatio = 100;

        /**
         * RPC客户端-建立链接超时（毫秒）. 首次建立通道最大等待时间，建立后就是长连接
         */
        private int clientConnectTimeout = 1000;

        /**
         * RPC客户端-服务端响应超时（毫秒）.一次业务请求的最大等待时间
         */
        private int clientServerResponseTimeout = 1000;

        /**
         * RPC客户端- 心跳间隔（毫秒）. 用于监测健康的心跳包. 小于等于0则为不启用心跳
         */
        private int clientHeartIntervalTimeMs = -1;

        /**
         * RPC客户端-是否开启断线重连的定时任务（true=开启,false=关闭）. 默认关闭,因为即使断线不重连,服务端可用的情况下,下次请求也是可以正常使用. 断线重连只是提前预热了三次握手的环节.
         */
        private boolean clientReconnectScheduledTaskEnable = false;

        /**
         * RPC客户端-断线重连的定时任务的检测间隔（毫秒）.
         */
        private int clientReconnectScheduledIntervalMs = 5000;

        /**
         * RPC客户端-是否开启心跳日志
         */
        private boolean clientEnableHeartLog = false;

        /**
         * RPC客户端 - 同名方法检查（因为泛化调用的参数允许不一致， 所以保证每个类的方法名称都是唯一的）
         */
        private boolean clientMethodOverwriteCheck = false;

        /**
         * RPC服务端 - 同名方法检查（因为泛化调用的参数允许不一致， 所以保证每个类的方法名称都是唯一的）
         */
        private boolean serverMethodOverwriteCheck = true;

        /**
         * RPC服务端 - 每次消息最大长度 (默认10M)
         */
        private int serverMessageMaxLength = 10 * 1024 * 1024;

        /**
         * RPC客户端 - 用户接口的全局默认版本，可以用主动覆盖 {@link com.github.netty.annotation.Protocol.RpcService#version() }
         */
        private String clientDefaultVersion = "";

        /**
         * RPC服务端 - 用户接口的全局默认版本，可以用主动覆盖 {@link com.github.netty.annotation.Protocol.RpcService#version() }
         */
        private String serverDefaultVersion = "";

        public boolean isClientReconnectScheduledTaskEnable() {
            return clientReconnectScheduledTaskEnable;
        }

        public void setClientReconnectScheduledTaskEnable(boolean clientReconnectScheduledTaskEnable) {
            this.clientReconnectScheduledTaskEnable = clientReconnectScheduledTaskEnable;
        }

        public String getServerDefaultVersion() {
            return serverDefaultVersion;
        }

        public void setClientReconnectScheduledIntervalMs(int clientReconnectScheduledIntervalMs) {
            this.clientReconnectScheduledIntervalMs = clientReconnectScheduledIntervalMs;
        }

        public int getClientReconnectScheduledIntervalMs() {
            return clientReconnectScheduledIntervalMs;
        }

        public void setServerDefaultVersion(String serverDefaultVersion) {
            this.serverDefaultVersion = serverDefaultVersion;
        }

        public int getClientServerResponseTimeout() {
            return clientServerResponseTimeout;
        }

        public void setClientServerResponseTimeout(int clientServerResponseTimeout) {
            this.clientServerResponseTimeout = clientServerResponseTimeout;
        }

        public boolean isServerMethodOverwriteCheck() {
            return serverMethodOverwriteCheck;
        }

        public void setServerMethodOverwriteCheck(boolean serverMethodOverwriteCheck) {
            this.serverMethodOverwriteCheck = serverMethodOverwriteCheck;
        }

        public boolean isClientMethodOverwriteCheck() {
            return clientMethodOverwriteCheck;
        }

        public void setClientMethodOverwriteCheck(boolean clientMethodOverwriteCheck) {
            this.clientMethodOverwriteCheck = clientMethodOverwriteCheck;
        }

        public int getClientHeartIntervalTimeMs() {
            return clientHeartIntervalTimeMs;
        }

        public void setClientHeartIntervalTimeMs(int clientHeartIntervalTimeMs) {
            this.clientHeartIntervalTimeMs = clientHeartIntervalTimeMs;
        }

        public int getClientConnectTimeout() {
            return clientConnectTimeout;
        }

        public void setClientConnectTimeout(int clientConnectTimeout) {
            this.clientConnectTimeout = clientConnectTimeout;
        }

        public String getClientDefaultVersion() {
            return clientDefaultVersion;
        }

        public void setClientDefaultVersion(String clientDefaultVersion) {
            this.clientDefaultVersion = clientDefaultVersion;
        }

        public int getClientIoThreads() {
            return clientIoThreads;
        }

        public void setClientIoThreads(int clientIoThreads) {
            this.clientIoThreads = clientIoThreads;
        }

        public int getClientIoRatio() {
            return clientIoRatio;
        }

        public void setClientIoRatio(int clientIoRatio) {
            this.clientIoRatio = clientIoRatio;
        }

        public boolean isClientEnableHeartLog() {
            return clientEnableHeartLog;
        }

        public void setClientEnableHeartLog(boolean clientEnableHeartLog) {
            this.clientEnableHeartLog = clientEnableHeartLog;
        }

        public int getServerMessageMaxLength() {
            return serverMessageMaxLength;
        }

        public void setServerMessageMaxLength(int serverMessageMaxLength) {
            this.serverMessageMaxLength = serverMessageMaxLength;
        }
    }

    public static class Mqtt{
        /**
         * 是否开启MQTT协议
         */
        private boolean enabled = false;
        /**
         * 消息最大长度(字节)
         */
        private int messageMaxLength = 8192;
        /**
         * netty读事件空闲时间(秒)
         */
        private int nettyReaderIdleTimeSeconds = 10;
        /**
         * 刷新缓冲区数据间隔(秒) (如果大于0秒则定时发送缓冲区数据, 小于等于0秒则实时发送数据)
         */
        private int autoFlushIdleTime = 0;

        public int getMessageMaxLength() {
            return messageMaxLength;
        }

        public void setMessageMaxLength(int messageMaxLength) {
            this.messageMaxLength = messageMaxLength;
        }

        public int getNettyReaderIdleTimeSeconds() {
            return nettyReaderIdleTimeSeconds;
        }

        public void setNettyReaderIdleTimeSeconds(int nettyReaderIdleTimeSeconds) {
            this.nettyReaderIdleTimeSeconds = nettyReaderIdleTimeSeconds;
        }

        public int getAutoFlushIdleTime() {
            return autoFlushIdleTime;
        }

        public void setAutoFlushIdleTime(int autoFlushIdleTime) {
            this.autoFlushIdleTime = autoFlushIdleTime;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public static class Rtsp{

    }

    public static class Mysql{
        /**
         * 是否开启MYSQL代理协议
         */
        private boolean enabled = false;
        /**
         * 包最大长度(字节)
         */
        private int packetMaxLength = 16777216;
        private String mysqlHost = "localhost";
        private int mysqlPort = 3306;
        /**
         * 代理日志的配置
         */
        @NestedConfigurationProperty
        private final MysqlProxyLog proxyLog = new MysqlProxyLog();

        /**
         * 用户可以处理MYSQL后端的业务处理, 每次有链接进入时, 会从spring容器中获取实例, 不能是单例对象, 请使用原型实例
         */
        private Class<?extends MysqlBackendBusinessHandler> backendBusinessHandler = MysqlBackendBusinessHandler.class;
        /**
         * 用户可以处理MYSQL前端的业务逻辑, 每次有链接进入时, 会从spring容器中获取实例, 不能是单例对象, 请使用原型实例
         */
        private Class<?extends MysqlFrontendBusinessHandler> frontendBusinessHandler = MysqlFrontendBusinessHandler.class;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public MysqlProxyLog getProxyLog() {
            return proxyLog;
        }

        public int getPacketMaxLength() {
            return packetMaxLength;
        }

        public void setPacketMaxLength(int packetMaxLength) {
            this.packetMaxLength = packetMaxLength;
        }

        public String getMysqlHost() {
            return mysqlHost;
        }

        public void setMysqlHost(String mysqlHost) {
            this.mysqlHost = mysqlHost;
        }

        public int getMysqlPort() {
            return mysqlPort;
        }

        public void setMysqlPort(int mysqlPort) {
            this.mysqlPort = mysqlPort;
        }

        public Class<? extends MysqlFrontendBusinessHandler> getFrontendBusinessHandler() {
            return frontendBusinessHandler;
        }

        public Class<? extends MysqlBackendBusinessHandler> getBackendBusinessHandler() {
            return backendBusinessHandler;
        }

        public void setFrontendBusinessHandler(Class<? extends MysqlFrontendBusinessHandler> frontendBusinessHandler) {
            this.frontendBusinessHandler = frontendBusinessHandler;
        }

        public void setBackendBusinessHandler(Class<? extends MysqlBackendBusinessHandler> backendBusinessHandler) {
            this.backendBusinessHandler = backendBusinessHandler;
        }
    }

    /**
     * mysql代理日志的配置
     */
    public static class MysqlProxyLog{
        /**
         * 是否开启代理日志
         */
        private boolean enable = false;
        /**
         * 日志刷新写入间隔 (5000毫秒)
         */
        private int logFlushInterval = 5000;
        /**
         * 日志文件名
         */
        private String logFileName = "-packet.log";
        /**
         * 日志文件夹
         */
        private String logPath = "${user.dir}/netty-mysql";

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public int getLogFlushInterval() {
            return logFlushInterval;
        }

        public void setLogFlushInterval(int logFlushInterval) {
            this.logFlushInterval = logFlushInterval;
        }

        public String getLogFileName() {
            return logFileName;
        }

        public void setLogFileName(String logFileName) {
            this.logFileName = logFileName;
        }

        public String getLogPath() {
            return logPath;
        }

        public void setLogPath(String logPath) {
            this.logPath = logPath;
        }

    }

}

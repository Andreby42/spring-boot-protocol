package com.github.netty.protocol;

import com.github.netty.core.AbstractProtocol;
import com.github.netty.core.util.LoggerFactoryX;
import com.github.netty.core.util.LoggerX;
import com.github.netty.protocol.mqtt.*;
import com.github.netty.protocol.mqtt.config.BrokerConfiguration;
import com.github.netty.protocol.mqtt.config.FileResourceLoader;
import com.github.netty.protocol.mqtt.config.IResourceLoader;
import com.github.netty.protocol.mqtt.interception.BrokerInterceptor;
import com.github.netty.protocol.mqtt.interception.InterceptHandler;
import com.github.netty.protocol.mqtt.security.*;
import com.github.netty.protocol.mqtt.subscriptions.CTrieSubscriptionDirectory;
import com.github.netty.protocol.mqtt.subscriptions.ISubscriptionsDirectory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.timeout.IdleStateHandler;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

/**
 * Internet of things messaging protocol
 * @author wangzihao
 *  2018/12/5/005
 */
public class MqttProtocol extends AbstractProtocol {
    private LoggerX logger = LoggerFactoryX.getLogger(MqttProtocol.class);
    public static final int ORDER = NRpcProtocol.ORDER + 100;

    private int messageMaxLength;
    private int nettyReaderIdleTimeSeconds;
    private int autoFlushIdleTime;
    private boolean enableMetrics = false;
    private String metricsLibratoEmail;
    private String metricsLibratoToken;
    private String metricsLibratoSource;

    private MqttIdleTimeoutChannelHandler timeoutHandler = new MqttIdleTimeoutChannelHandler();
    private MqttLoggerChannelHandler mqttMessageLoggerChannelHandler = new MqttLoggerChannelHandler();

    private BrokerInterceptor interceptor = new BrokerInterceptor(1);
    private MqttServerChannelHandler mqttServerChannelHandler;
    private MqttDropWizardMetricsChannelHandler mqttDropWizardMetricsChannelHandler;
    private MqttPostOffice mqttPostOffice;

    public MqttProtocol() {
        this(8092,10,0);
    }

    /**
     *
     * @param messageMaxLength Maximum message length per pass (bytes)
     * @param nettyReaderIdleTimeSeconds Read idle interval (seconds)
     * @param autoFlushIdleTime Auto refresh buffer interval (s). If greater than 0, it will be on and auto refresh. If less than or equal to 0, it will refresh every time
     */
    public MqttProtocol(int messageMaxLength, int nettyReaderIdleTimeSeconds, int autoFlushIdleTime) {
        this.messageMaxLength = messageMaxLength;
        this.nettyReaderIdleTimeSeconds = nettyReaderIdleTimeSeconds;
        this.autoFlushIdleTime = autoFlushIdleTime;
    }

    @Override
    public String getProtocolName() {
        return "mqtt";
    }

    @Override
    public boolean canSupport(ByteBuf msg) {
        if(msg.readableBytes() < 9){
            return false;
        }

        if( msg.getByte(4) == 'M'
                &&  msg.getByte(5) == 'Q'
                &&  msg.getByte(6) == 'T'
                &&   msg.getByte(7) == 'T'){
            return true;
        }
        return false;
    }

    @Override
    public void onSupportPipeline(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addFirst("idleStateHandler", new IdleStateHandler(nettyReaderIdleTimeSeconds, 0, 0));
        pipeline.addAfter("idleStateHandler", "idleEventHandler", timeoutHandler);

        if(autoFlushIdleTime > 0) {
            pipeline.addLast("autoflush", new MqttAutoFlushChannelHandler(autoFlushIdleTime, TimeUnit.SECONDS));
        }
        pipeline.addLast("decoder", new MqttDecoder(messageMaxLength));
        pipeline.addLast("encoder", MqttEncoder.INSTANCE);

        pipeline.addLast("messageLogger",mqttMessageLoggerChannelHandler);

        if(isEnableMetrics()) {
            if(mqttDropWizardMetricsChannelHandler == null) {
                mqttDropWizardMetricsChannelHandler = new MqttDropWizardMetricsChannelHandler();
                mqttDropWizardMetricsChannelHandler.init(metricsLibratoEmail, metricsLibratoToken, metricsLibratoSource);
            }
            pipeline.addLast("wizardMetrics", mqttDropWizardMetricsChannelHandler);
        }

        pipeline.addLast("handler", mqttServerChannelHandler);
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void onServerStart() throws Exception {
        IAuthorizatorPolicy authorizatorPolicy = initializeAuthorizatorPolicy();

        ISubscriptionsDirectory subscriptions = new CTrieSubscriptionDirectory(new MemorySubscriptionsRepository());
        MqttSessionRegistry sessions = new MqttSessionRegistry(subscriptions, new MemoryQueueRepository());
        mqttPostOffice = new MqttPostOffice(subscriptions, authorizatorPolicy, new MemoryRetainedRepository(), sessions,interceptor);
        mqttServerChannelHandler = new MqttServerChannelHandler(new BrokerConfiguration(), new AcceptAllAuthenticator(), sessions, mqttPostOffice);
    }

    @Override
    public void onServerStop() throws Exception {
        if(interceptor != null) {
            interceptor.stop();
        }
    }

    protected IAuthorizatorPolicy initializeAuthorizatorPolicy() {
        IAuthorizatorPolicy authorizatorPolicy;
        String aclFilePath = null;
        if (null == null || aclFilePath.isEmpty()) {
            authorizatorPolicy = new PermitAllAuthorizatorPolicy();
        } else {
            authorizatorPolicy = new DenyAllAuthorizatorPolicy();
            try {
                IResourceLoader resourceLoader = new FileResourceLoader();
                authorizatorPolicy = ACLFileParser.parse(resourceLoader.loadResource(aclFilePath));
            } catch (ParseException pex) {
                logger.error("Unable to parse ACL file. path=" + aclFilePath, pex);
            }
        }
        return authorizatorPolicy;
    }

    public void internalPublish(MqttPublishMessage msg, final String clientId) {
        final int messageID = msg.variableHeader().packetId();
        logger.trace("Internal publishing message CId: {}, messageId: {}", clientId, messageID);
        mqttPostOffice.internalPublish(msg);
    }

    public void addInterceptHandler(InterceptHandler interceptHandler) {
        logger.info("Adding MQTT message interceptor. InterceptorId={}", interceptHandler.getID());
        interceptor.addInterceptHandler(interceptHandler);
    }

    public void removeInterceptHandler(InterceptHandler interceptHandler) {
        logger.info("Removing MQTT message interceptor. InterceptorId={}", interceptHandler.getID());
        interceptor.removeInterceptHandler(interceptHandler);
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    public String getMetricsLibratoEmail() {
        return metricsLibratoEmail;
    }

    public void setMetricsLibratoEmail(String metricsLibratoEmail) {
        this.metricsLibratoEmail = metricsLibratoEmail;
    }

    public String getMetricsLibratoToken() {
        return metricsLibratoToken;
    }

    public void setMetricsLibratoToken(String metricsLibratoToken) {
        this.metricsLibratoToken = metricsLibratoToken;
    }

    public String getMetricsLibratoSource() {
        return metricsLibratoSource;
    }

    public void setMetricsLibratoSource(String metricsLibratoSource) {
        this.metricsLibratoSource = metricsLibratoSource;
    }

    public int getAutoFlushIdleTime() {
        return autoFlushIdleTime;
    }

    public void setAutoFlushIdleTime(int autoFlushIdleTime) {
        this.autoFlushIdleTime = autoFlushIdleTime;
    }
}
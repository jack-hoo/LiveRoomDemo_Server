package com.liveroom.config;

import com.liveroom.service.LiveHandshakeInterceptor;
import com.liveroom.service.MyChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

/**
 * STOMP over WebSocket 配置。
 *
 * <p>消息代理使用外部 RabbitMQ（需要启用 {@code rabbitmq_stomp} 插件，默认端口 61613）。
 * 如果只想本地跑通、不想装 RabbitMQ，把 {@code liveroom.broker.relay-enabled} 设为 false，
 * 会退化成 Spring 内置的简单内存代理。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    private final String[] allowedOrigins;
    private final boolean relayEnabled;
    private final String relayHost;
    private final int relayPort;
    private final String brokerUsername;
    private final String brokerPassword;

    public WebSocketConfig(
            @Value("${liveroom.cors.allowed-origins:http://localhost:8080}") String[] allowedOrigins,
            @Value("${liveroom.broker.relay-enabled:true}") boolean relayEnabled,
            @Value("${liveroom.broker.relay-host:127.0.0.1}") String relayHost,
            @Value("${liveroom.broker.relay-port:61613}") int relayPort,
            @Value("${liveroom.broker.username:guest}") String brokerUsername,
            @Value("${liveroom.broker.password:guest}") String brokerPassword) {
        this.allowedOrigins = allowedOrigins;
        this.relayEnabled = relayEnabled;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.brokerUsername = brokerUsername;
        this.brokerPassword = brokerPassword;
    }

    /**
     * 以 @Bean 方式声明，拦截器里的 @Autowired 字段才会被注入（直接 new 出来的对象不受 Spring 管理）。
     */
    @Bean
    public MyChannelInterceptor myChannelInterceptor() {
        return new MyChannelInterceptor();
    }

    @Bean
    public LiveHandshakeInterceptor liveHandshakeInterceptor() {
        return new LiveHandshakeInterceptor();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 原来是 setAllowedOrigins("*")，任意站点都能建立 WebSocket 连接并借用用户的 session。
        // 改为与 CORS 共用同一份来源白名单。
        registry.addEndpoint("/live")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(liveHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/demo");
        if (relayEnabled) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(brokerUsername)
                    .setClientPasscode(brokerPassword)
                    .setSystemLogin(brokerUsername)
                    .setSystemPasscode(brokerPassword)
                    .setSystemHeartbeatSendInterval(5000)
                    .setSystemHeartbeatReceiveInterval(4000);
        } else {
            registry.enableSimpleBroker("/topic", "/queue");
        }
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 原实现把 setInterceptors 的返回值赋给了一个没人用的局部变量，看起来像是没生效。
        // 实际生效的是 registration 上的副作用，这里去掉误导性的赋值。
        registration.setInterceptors(myChannelInterceptor());
        super.configureClientInboundChannel(registration);
    }
}

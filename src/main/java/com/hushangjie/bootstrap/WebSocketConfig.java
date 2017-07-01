package com.hushangjie.bootstrap;

import com.hushangjie.dao.StatDao;
import com.hushangjie.service.HandShkeInceptor;
import com.hushangjie.service.MyChannelInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * Created by hushangjie on 2017/6/12.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {
    //拦截器注入service失败解决办法
    @Bean
    public MyChannelInterceptor myChannelInterceptor(){
        return new MyChannelInterceptor();
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //添加访问域名限制可以防止跨域soket连接
        //setAllowedOrigins("http://localhost:8085")
        registry.addEndpoint("/live").setAllowedOrigins("*").addInterceptors(new HandShkeInceptor()).withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        /*.enableSimpleBroker("/topic","/queue");*/
        //假如需要第三方消息队列，比如rabitMQ,activeMq，在这里配置
        registry.setApplicationDestinationPrefixes("/demo")
                .enableStompBrokerRelay("/topic","/queue")
                .setRelayHost("127.0.0.1")
                .setRelayPort(61613)
                .setClientLogin("guest")
                .setClientPasscode("guest")
                .setSystemLogin("guest")
                .setSystemPasscode("guest")
                .setSystemHeartbeatSendInterval(5000)
                .setSystemHeartbeatReceiveInterval(4000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        ChannelRegistration channelRegistration = registration.setInterceptors(myChannelInterceptor());
        super.configureClientInboundChannel(registration);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        super.configureClientOutboundChannel(registration);
    }

}

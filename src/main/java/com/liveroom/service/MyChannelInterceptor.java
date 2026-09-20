package com.liveroom.service;

import com.liveroom.dao.StatDao;
import com.liveroom.entity.Guest;
import com.liveroom.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * STOMP 入站通道拦截器，负责两件事：
 * <ul>
 *   <li>校验订阅目的地，防止客户端订阅未授权的频道；</li>
 *   <li>在 CONNECT / DISCONNECT 时维护在线用户集合与访客历史，并实时推送在线人数。</li>
 * </ul>
 *
 * <p>本类由 {@code WebSocketConfig} 以 @Bean 方式创建，因此 @Autowired 字段能正常注入。
 */
public class MyChannelInterceptor extends ChannelInterceptorAdapter {

    private static final Logger log = LoggerFactory.getLogger(MyChannelInterceptor.class);

    private static final String ONLINE_USER_TOPIC = "/topic/online_user";

    /** 允许订阅的频道白名单。真实项目里应该按房间/权限从数据库查，这里是 demo，写死即可。 */
    private static final Set<String> ALLOWED_DESTINATIONS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("/topic/group", ONLINE_USER_TOPIC)));

    @Autowired
    private StatDao statDao;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && !ALLOWED_DESTINATIONS.contains(accessor.getDestination())) {
            // 返回 null 表示丢弃这条 SUBSCRIBE 帧，客户端也就收不到该频道的任何消息。
            log.warn("拒绝非法订阅：{}", accessor.getDestination());
            return null;
        }
        return super.preSend(message, channel);
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        UserEntity user = currentUser(accessor);

        if (user != null && StompCommand.CONNECT.equals(command)) {
            statDao.pushOnlineUser(user);

            Guest guest = new Guest();
            guest.setUserEntity(user);
            guest.setAccessTime(Calendar.getInstance().getTimeInMillis());
            statDao.pushGuestHistory(guest);

            simpMessagingTemplate.convertAndSend(ONLINE_USER_TOPIC, statDao.getAllUserOnline());
        } else if (user != null && StompCommand.DISCONNECT.equals(command)) {
            statDao.popOnlineUser(user);
            simpMessagingTemplate.convertAndSend(ONLINE_USER_TOPIC, statDao.getAllUserOnline());
        }
        super.afterSendCompletion(message, channel, sent, ex);
    }

    /**
     * 原实现把 session attributes 直接强转成 {@code Map<String, UserEntity>}：这个 map 里
     * 其实什么类型都可能有，泛型只是骗过了编译器，取值时才会在别处炸出 ClassCastException。
     */
    private UserEntity currentUser(StompHeaderAccessor accessor) {
        Object attributes = accessor.getHeader("simpSessionAttributes");
        if (!(attributes instanceof Map)) {
            return null;
        }
        Object user = ((Map<?, ?>) attributes).get("user");
        return user instanceof UserEntity ? (UserEntity) user : null;
    }
}

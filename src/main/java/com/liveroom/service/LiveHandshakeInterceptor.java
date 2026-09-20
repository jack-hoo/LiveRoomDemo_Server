package com.liveroom.service;

import com.liveroom.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * WebSocket 握手拦截器：只有先访问过 {@code /live_room}（从而在 session 里拿到 user）
 * 的客户端才允许升级成 WebSocket 连接。
 */
public class LiveHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LiveHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        if (!(request instanceof ServletServerHttpRequest)) {
            log.warn("非 Servlet 请求，拒绝握手：{}", request.getURI());
            return false;
        }
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        // 这里必须用 getSession(false)：传 true 会在握手阶段新建 session，
        // 部分移动端浏览器因此拿不到原来的 session，导致握手失败。
        HttpSession session = servletRequest.getServletRequest().getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof UserEntity)) {
            log.debug("session 中没有访客信息，拒绝握手：{}", request.getURI());
            return false;
        }
        return super.beforeHandshake(request, response, wsHandler, attributes);
    }
}

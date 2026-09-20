package com.liveroom.controller;

import com.liveroom.dao.StatDao;
import com.liveroom.dao.UserDao;
import com.liveroom.entity.MsgEntity;
import com.liveroom.entity.UserEntity;
import com.liveroom.service.IpUtil;
import com.liveroom.service.NameGenerator;
import com.liveroom.service.UserAgentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 直播间页面与聊天消息入口。
 */
@Controller
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final UserDao userDao;
    private final StatDao statDao;
    private final boolean trustProxyHeaders;
    private final int maxMessageLength;
    private final String rtmpUrl;
    private final String publishUrl;

    @Autowired
    public DemoController(UserDao userDao,
                          StatDao statDao,
                          @Value("${liveroom.trust-proxy-headers:false}") boolean trustProxyHeaders,
                          @Value("${liveroom.chat.max-message-length:200}") int maxMessageLength,
                          @Value("${liveroom.stream.rtmp-url:}") String rtmpUrl,
                          @Value("${liveroom.stream.publish-url:}") String publishUrl) {
        this.userDao = userDao;
        this.statDao = statDao;
        this.trustProxyHeaders = trustProxyHeaders;
        this.maxMessageLength = maxMessageLength;
        this.rtmpUrl = rtmpUrl;
        this.publishUrl = publishUrl;
    }

    /**
     * 直播间首页。手机端返回打包好的 Vue 单页，电脑端返回 thymeleaf 渲染的页面。
     */
    @RequestMapping(value = "/live_room", method = RequestMethod.GET)
    public String liveRoom(HttpServletRequest request, Model model) {
        String ip = IpUtil.getIp(request, trustProxyHeaders);
        HttpSession session = request.getSession();

        // 原实现对同一个 ip 查了两次库，这里只查一次。
        UserEntity user = userDao.findOne(ip);
        if (user == null) {
            user = new UserEntity();
            user.setIp(ip);
            user.setRandomName(NameGenerator.generate());
            user = userDao.save(user);
            log.debug("新访客 ip={} 分配昵称={}", ip, user.getRandomName());
        } else {
            log.debug("回访访客 ip={} 昵称={}", ip, user.getRandomName());
        }
        session.setAttribute("user", user);

        if (UserAgentUtil.isMobile(request)) {
            return "live_m";
        }
        model.addAttribute("online_guests", getOnlineUser());
        model.addAttribute("history_guests", getHistoryGuests());
        // 流地址原本写死在页面里（连服务器公网 IP 都硬编码了），改成从配置注入
        model.addAttribute("rtmp_url", rtmpUrl);
        model.addAttribute("publish_url", publishUrl);
        return "live";
    }

    @RequestMapping(value = "/online_guests", method = RequestMethod.GET)
    @ResponseBody
    public Set<Object> getOnlineUser() {
        return statDao.getAllUserOnline();
    }

    @RequestMapping(value = "/history_guests", method = RequestMethod.GET)
    @ResponseBody
    public List<Object> getHistoryGuests() {
        return statDao.getGuestHistory();
    }

    /**
     * 接收聊天消息并广播给整个直播间。
     *
     * <p>消息体来自客户端，这里做三件事：去首尾空白、拒绝空消息、限制长度（默认 200 字），
     * 避免单条超长消息刷屏或撑爆弹幕队列。返回 null 时 Spring 不会向 {@code @SendTo}
     * 指定的目的地推送，相当于静默丢弃这条消息。
     *
     * <p>消息内容不在服务端做 HTML 转义，由前端负责按文本渲染，避免双重转义。
     */
    @MessageMapping(value = "/chat")
    @SendTo("/topic/group")
    public MsgEntity onChatMessage(String message,
                                   @Header(value = "simpSessionAttributes") Map<String, Object> session) {
        Object attribute = session == null ? null : session.get("user");
        if (!(attribute instanceof UserEntity)) {
            // 握手拦截器会拦掉没有 session 用户的连接，正常流程不会走到这里。
            // 原实现直接强转并调用 getRandomName()，一旦为 null 就是 NPE。
            log.warn("收到没有绑定用户的聊天消息，已丢弃");
            return null;
        }
        if (message == null) {
            return null;
        }
        String body = message.trim();
        if (body.isEmpty()) {
            return null;
        }
        if (body.length() > maxMessageLength) {
            body = body.substring(0, maxMessageLength);
        }

        MsgEntity msg = new MsgEntity();
        msg.setCreator(((UserEntity) attribute).getRandomName());
        msg.setsTime(Calendar.getInstance());
        msg.setMsgBody(body);
        return msg;
    }
}

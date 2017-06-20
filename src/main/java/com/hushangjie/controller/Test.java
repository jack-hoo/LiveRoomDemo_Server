package com.hushangjie.controller;

import com.hushangjie.dao.StatDao;
import com.hushangjie.dao.UserDao;
import com.hushangjie.entity.Guest;
import com.hushangjie.entity.UserEntity;
import com.hushangjie.service.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 2017/6/12.
 */
@Controller
@RequestMapping("/")
public class Test {
    @Autowired
    private UserDao userDao;
    @Autowired
    private StatDao statDao;
    @Autowired
    @Lazy
    private SimpMessagingTemplate simpMessagingTemplate;
    /*@RequestMapping(value = "/live",method = RequestMethod.GET)
    public String hello(){
        return "live";
    }*/
    @RequestMapping(value = "/live2",method = RequestMethod.GET)
    public String hello2(HttpServletRequest request){
        System.out.println(request.getSession().getId());
        return "live2";
    }
    @RequestMapping(value = "/video" , method = RequestMethod.GET)
    public String video(){
        return "video";
    }
    @RequestMapping(value = "/ip",method = RequestMethod.GET)
    @ResponseBody
    public String getIp(HttpServletRequest request){
        return IpUtil.getIp(request);
    }
    @RequestMapping(value = "/chatroom",method = RequestMethod.GET)
    public String testChatRoom(){
        return "websocket";
    }
    @RequestMapping(value = "/jpa",method = RequestMethod.GET)
    @ResponseBody
    public UserEntity testJpa(@RequestParam(value = "name",required = true) String username ,
                              HttpServletRequest request){
        UserEntity entity = new UserEntity();
        entity.setRandomName(username);
        entity.setIp(IpUtil.getIp(request));
        return userDao.save(entity);
    }
    /*Principal principal ,存放用户的登录验证信息
    Message message，最基础的消息体，里面方有header和payload等信息
    @Payload 消息体内容
    @Header(“..”) 某个头部key的值
    @Headers, 所有头部key的map集合
    MessageHeaders , SimpMessageHeaderAccessor, MessageHeaderAccessor ,StompHeaderAccessor 消息头信息
    @DestinationVariable 类似springmvc中的@PathVariable*/
    @MessageMapping(value = "/live")
    @SendTo("/topic/group")
    public String testWst(String message){

        return message;
    }
    /*@MessageMapping(value = "/live2")
    @SendTo("/topic/group2")
    public String testWst2(String message, @Header(value = "simpSessionAttributes") Map<String,Object> session){
        *//*Iterator iterator = session.entrySet().iterator();
        while (iterator.hasNext()){
            System.out.println(iterator.next());
        }*//*
        *//*UserEntity user = (UserEntity) session.get("user");
        System.out.println(user.getRandomName()+"ip"+user.getIp());*//*
        return "group2";
    }*/
   /* @MessageMapping(value = "/live3")
    @SendTo("/queue/users")
    public String getOnlineGuest(){
        return "当前人数40人";
        //this.simpMessagingTemplate.convertAndSend("/topic/online_user","老师发布了一个通知");
    }*/
    @RequestMapping(value = "/online_counts",method = RequestMethod.GET)
    @ResponseBody
    public String  pushCounts(@RequestParam(value = "ip",required = true) String ip){
        UserEntity userEntity = new UserEntity();
        userEntity.setIp(ip);
        userEntity.setRandomName("test");
        statDao.pushOnlineUser(userEntity);
        return "ok";
    }
    @RequestMapping(value = "/online_counts",method = RequestMethod.DELETE)
    @ResponseBody
    public String  popCounts(@RequestParam(value = "ip",required = true) String ip){
        UserEntity userEntity = new UserEntity();
        userEntity.setIp(ip);
        userEntity.setRandomName("test");
        statDao.popOnlineUser(userEntity);
        return "ok";
    }
    @RequestMapping(value = "/guest",method = RequestMethod.GET)
    @ResponseBody
    public void pushGuest(@RequestParam(value = "ip",required = true) String ip){
        UserEntity userEntity = new UserEntity();
        userEntity.setIp(ip);
        userEntity.setRandomName("test");
        Guest guest = new Guest();
        guest.setUserEntity(userEntity);
        guest.setAccessTime(Calendar.getInstance().getTimeInMillis());
        statDao.pushGuestHistory(guest);
    }
    @RequestMapping(value = "/all_guest",method = RequestMethod.GET)
    @ResponseBody
    public List AllGuest(){
        return statDao.getGuestHistory();
    }
    @RequestMapping(value= "/send", method = RequestMethod.GET)
    @ResponseBody
    public String send() {

        this.simpMessagingTemplate.convertAndSend("/topic/online_user","老师发布了一个通知");
        //this.template.convertAndSend("/discuss/replymesg/666666", "有人回复了你");
        return "服务器发布消息";
    }
}

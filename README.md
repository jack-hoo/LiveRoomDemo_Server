
# LiveRoomDemo(服务端)
> 这是一个用java实现的一个直播间Demo,主要实现了以下功能
* 拉取服务器上的直播流(移动端拉取hls流、电脑端拉取rtmp流)
* 基于websocket的直播聊天室
* 直播间弹幕
* 直播间的实时数据统计    
* [演示地址(电脑端与移动端效果不同哦)](http://139.199.82.213:8080/LiveDemo/live_room)
* [博客地址](https://segmentfault.com/a/1190000009892006)

## 技术栈    
- IDE: IntelliJ IDEA 
- 项目架构: SpringBoot1.5.4 +Maven3.0
- 主数据库: Mysql5.7
- 辅数据库: redis3.2
- 数据库访问层: spring-boot-starter-data-jpa + spring-boot-starter-data-redis
- websocket: spring-boot-starter-websocket
- 消息中间件: RabbitMQ/3.6.10
- 前端(电脑端)汇总:
    * 项目架构: Jquery + BootStrap
    * 视频播放器: video.js
    * websocket客户端: stomp.js + sockjs.js
    * 弹幕插件: Jquery.danmu.js
    * 模版引擎: thymeleaf       
- 移动客户端项目在[这里](https://github.com/jack-hoo/LiveRoomDemo_Client)
    
## 运行截图   
![户外直播](https://github.com/jack-hoo/LiveRoomDemo_Client/blob/master/static/screenshot/mzdemo.jpg)

![全局](https://github.com/jack-hoo/LiveRoomDemo_Client/blob/master/static/screenshot/screenshot1.png)
> 弹幕效果
![弹幕](https://github.com/jack-hoo/LiveRoomDemo_Client/blob/master/static/screenshot/danmu.png)

## 部署说明

详细部署说明文档在[这里](https://segmentfault.com/a/1190000009892006)

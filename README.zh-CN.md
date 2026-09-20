# LiveRoomDemo（服务端）

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-1.5.4-brightgreen.svg)](https://spring.io/projects/spring-boot)

一个直播间 Demo 的服务端，实现了：

- 拉取媒体服务器上的直播流（移动端拉 HLS，电脑端拉 RTMP）
- 基于 WebSocket(STOMP + SockJS) 的直播聊天室
- 与聊天内容联动的直播间弹幕
- 实时在线人数统计和访客记录（存 Redis）
- 同时提供电脑端页面（Thymeleaf + jQuery）和移动端单页（来自
  [LiveRoomDemo_Client](https://github.com/jack-hoo/LiveRoomDemo_Client) 的构建产物）

English: [README.md](README.md)

---

## 目录

- [运行截图](#运行截图)
- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [部署](#部署)
- [项目结构](#项目结构)
- [已知限制](#已知限制)
- [参与贡献](#参与贡献)
- [开源协议](#开源协议)

## 运行截图

![全局](https://raw.githubusercontent.com/jack-hoo/LiveRoomDemo_Client/master/static/screenshot/screenshot1.png)

弹幕效果：

![弹幕](https://raw.githubusercontent.com/jack-hoo/LiveRoomDemo_Client/master/static/screenshot/danmu.png)

## 技术栈

| 分层 | 选型 |
| --- | --- |
| 框架 | Spring Boot 1.5.4 + Maven |
| 主数据库 | MySQL 5.7（spring-boot-starter-data-jpa） |
| 实时数据 | Redis 3.2（spring-boot-starter-data-redis） |
| 消息 | STOMP over WebSocket，可选接 RabbitMQ 做消息代理 |
| 电脑端前端 | Thymeleaf + jQuery + Bootstrap + video.js + jquery.danmu.js |
| 移动端前端 | Vue 2 单页，单独构建后作为静态资源托管 |

## 环境要求

| 组件 | 版本 | 是否必需 |
| --- | --- | --- |
| JDK | **8**（Spring Boot 1.5.x 不支持更高版本的 JDK 编译） | 必需 |
| Maven | 3.x | 必需 |
| MySQL | 5.7+ | 必需 |
| Redis | 3.2+ | 必需 |
| RabbitMQ | 3.6+，需开启 `rabbitmq_stomp` 插件 | 可选，见下 |
| nginx-rtmp | — | 可选，只有要自己推流时才需要 |

> **关于 JDK 版本**：如果 `java -version` 显示的不是 8，用 `JAVA_HOME` 临时指定即可：
>
> ```bash
> JAVA_HOME=/path/to/jdk8 mvn spring-boot:run
> ```

## 快速开始

### 1. 建库建表

```bash
mysql -u root -p < docs/sql/schema.sql
```

脚本会创建 `livedemo` 库和 `user` 表。应用配置里 `spring.jpa.hibernate.ddl-auto=none`，
表结构不会自动生成，这一步不能跳过。

### 2. 启动 Redis

```bash
redis-server
```

### 3. 配置并启动应用

所有敏感配置都走环境变量，不需要改动 `application.properties`：

```bash
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=yourpassword

# 不想装 RabbitMQ 的话，关掉 relay，用 Spring 内置的内存代理，单机 demo 完全够用
export BROKER_RELAY_ENABLED=false

mvn spring-boot:run
```

### 4. 访问

浏览器打开 <http://localhost:8085/LiveDemo/live_room>。

服务端会按 User-Agent 判断终端：手机 UA 返回移动端单页，其他返回电脑端页面。
想在电脑上看移动端效果，用浏览器开发者工具切换成手机设备模拟即可。

> **路径里的 `/LiveDemo` 是什么？**
> 前端把接口前缀写死成了 `/LiveDemo`，所以服务端的 `server.context-path` 默认也设成
> `/LiveDemo`。打成 war 部署到外部 Tomcat 时，这个路径由 war 包名决定，
> 因此 `pom.xml` 里把 `finalName` 固定成了 `LiveDemo`。

### 5. 跑测试

```bash
mvn test
```

单元测试不依赖 MySQL / Redis / RabbitMQ，可以直接跑。

## 配置说明

所有配置项及其环境变量名见 **[docs/CONFIGURATION.md](docs/CONFIGURATION.md)**。
最常用的几个：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | `root` / 空 | 数据库账号 |
| `BROKER_RELAY_ENABLED` | `true` | 设为 `false` 可不依赖 RabbitMQ |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8080` | 跨域与 WebSocket 来源白名单 |
| `STREAM_HLS_URL` | 空 | 移动端拉流地址（m3u8） |
| `STREAM_PUBLISH_URL` | 空 | 推流地址，用于页面提示 |
| `TRUST_PROXY_HEADERS` | `false` | 是否信任 `X-Forwarded-For` |

## 部署

生产部署（nginx-rtmp 推拉流、RabbitMQ STOMP 插件、war 包部署到外部 Tomcat、
Nginx 反向代理 WebSocket）请看 **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)**。

## 项目结构

```
src/main/java/com/liveroom/
├── LiveRoomApplication.java        # 启动类（内嵌容器）
├── ServletInitializer.java         # war 包部署入口
├── config/
│   ├── CorsConfig.java             # 跨域白名单
│   ├── RedisConfig.java            # Redis 序列化
│   └── WebSocketConfig.java        # STOMP 端点与消息代理
├── controller/
│   └── DemoController.java         # 直播间页面 + 聊天消息入口
├── dao/
│   ├── StatDao.java                # Redis 在线/访客统计
│   └── UserDao.java                # 访客表
├── entity/                         # UserEntity / Guest / MsgEntity
└── service/
    ├── IpUtil.java                 # 访客 IP 解析
    ├── LiveHandshakeInterceptor.java # WebSocket 握手鉴权
    ├── MyChannelInterceptor.java   # 订阅校验 + 在线统计
    ├── NameGenerator.java          # 随机中文昵称
    └── UserAgentUtil.java          # 手机/电脑端判断

src/main/resources/
├── application.properties          # 配置（含详细注释）
├── templates/
│   ├── live.html                   # 电脑端页面
│   └── live_m.html                 # 移动端单页入口
└── static/                         # 移动端构建产物 + 第三方静态资源

docs/
├── CONFIGURATION.md                # 配置项说明
├── DEPLOYMENT.md                   # 部署文档
└── sql/schema.sql                  # 建表脚本
```

### 移动端资源是怎么来的

`src/main/resources/static/{js,css,vonic}/` 下的 `app.*.js`、`vendor.*.js`、
`manifest.*.js`、`app.*.css` 是客户端仓库 `npm run build` 的产物，
`templates/live_m.html` 则对应客户端的 `index.tpl.html`。

改了客户端代码之后，需要重新构建并把产物同步过来，具体步骤见
[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) 的「同步移动端构建产物」一节。

## 已知限制

- **Spring Boot 1.5.4 已于 2019 年 8 月停止维护**，存在已知 CVE。本次整理刻意没有升级
  框架版本（保持项目原貌），因此**不建议直接部署到公网**。详见 [SECURITY.md](SECURITY.md)。
- **电脑端 RTMP 拉流已经播不出来了**：RTMP 播放依赖 Flash，主流浏览器 2021 年起
  全面移除了 Flash 支持。要在浏览器里看直播请配置 HLS(m3u8) 源。
- 访客身份用 IP 做主键，同一个局域网出口下的用户会被认成同一个人。这是 demo 的简化设计，
  真实项目应该接入正经的登录体系。
- 聊天消息不做持久化，服务重启后历史消息即丢失。

## 参与贡献

欢迎提 Issue 和 PR，请先阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 开源协议

[MIT](LICENSE) © jack-hoo

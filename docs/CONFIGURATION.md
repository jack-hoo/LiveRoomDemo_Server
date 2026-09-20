# 配置说明

所有配置都在 `src/main/resources/application.properties` 里，写成
`属性名=${环境变量:默认值}` 的形式。这意味着：

- **默认值是本地开发用的占位值**，直接 `mvn spring-boot:run` 就能跑起来（前提是
  MySQL / Redis 用默认账号在本机监听）。
- **真实密码通过环境变量注入**，不要写回这个文件，更不要提交到仓库。

三种覆盖方式，优先级从高到低：

```bash
# 1. 命令行参数
java -jar target/LiveDemo.war --spring.datasource.password=xxx

# 2. 环境变量（推荐）
export MYSQL_PASSWORD=xxx

# 3. 外置配置文件（不会被打进包里，且已在 .gitignore 中）
java -jar target/LiveDemo.war --spring.profiles.active=local
# 对应 application-local.properties
```

---

## 应用

| 属性 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `server.port` | `SERVER_PORT` | `8085` | 内嵌容器监听端口 |
| `server.context-path` | `SERVER_CONTEXT_PATH` | `/LiveDemo` | 应用上下文路径 |

> `context-path` 必须和前端的接口前缀一致。前端默认写的是 `/LiveDemo`
> （客户端仓库 `config/prod.env.js` 的 `API_BASE`）。两边要改一起改。
>
> 打成 war 部署到外部 Tomcat 时，上下文路径由 war 包名决定，此项不生效；
> `pom.xml` 里已把 `finalName` 固定成 `LiveDemo`。

## MySQL

| 属性 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| — | `MYSQL_HOST` | `127.0.0.1` | 数据库主机 |
| — | `MYSQL_PORT` | `3306` | 数据库端口 |
| — | `MYSQL_DATABASE` | `livedemo` | 库名 |
| `spring.datasource.username` | `MYSQL_USERNAME` | `root` | 账号 |
| `spring.datasource.password` | `MYSQL_PASSWORD` | 空 | 密码 |
| `spring.jpa.show-sql` | `JPA_SHOW_SQL` | `false` | 是否打印 SQL |

`spring.jpa.hibernate.ddl-auto` 固定为 `none`：表结构由
[`docs/sql/schema.sql`](sql/schema.sql) 创建，不允许 Hibernate 自动改表。

## Redis

| 属性 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `spring.redis.host` | `REDIS_HOST` | `127.0.0.1` | 主机 |
| `spring.redis.port` | `REDIS_PORT` | `6379` | 端口 |
| `spring.redis.password` | `REDIS_PASSWORD` | 空 | 密码 |
| `spring.redis.database` | `REDIS_DATABASE` | `0` | 库序号 |
| `spring.redis.timeout` | `REDIS_TIMEOUT` | `2000` | 超时（毫秒） |

Redis 里用到两个 key：

- `OnlineUser`（Set）：当前在线的访客
- `Guest`（List）：访客历史，长度上限由 `MAX_GUEST_HISTORY` 控制

## STOMP 消息代理

| 属性 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `liveroom.broker.relay-enabled` | `BROKER_RELAY_ENABLED` | `true` | 是否使用外部消息代理 |
| `liveroom.broker.relay-host` | `BROKER_HOST` | `127.0.0.1` | 代理主机 |
| `liveroom.broker.relay-port` | `BROKER_PORT` | `61613` | STOMP 端口 |
| `liveroom.broker.username` | `BROKER_USERNAME` | `guest` | 账号 |
| `liveroom.broker.password` | `BROKER_PASSWORD` | `guest` | 密码 |

`relay-enabled=false` 时退化成 Spring 内置的内存代理，不需要装 RabbitMQ，
单机跑 demo 推荐这样。多实例部署时必须用外部代理，否则各实例之间的消息不互通。

> RabbitMQ 的默认账号 `guest/guest` 只允许从 localhost 连接，且**绝不能**用于生产环境。

## 跨域与 WebSocket 来源

| 属性 | 环境变量 | 默认值 |
| --- | --- | --- |
| `liveroom.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:8080` |

逗号分隔的来源白名单，同时作用于 HTTP CORS 和 SockJS 端点。

本项目用 session 里的访客信息认身份，CORS 又开了 `allowCredentials`，
所以这里**不能**填 `*`——那等于允许任意站点带着用户 Cookie 调本站接口。
部署到线上请填你自己的域名，例如：

```bash
export CORS_ALLOWED_ORIGINS=https://live.example.com,https://www.example.com
```

## 直播流地址

| 属性 | 环境变量 | 说明 |
| --- | --- | --- |
| `liveroom.stream.rtmp-url` | `STREAM_RTMP_URL` | 电脑端拉取的 rtmp 地址 |
| `liveroom.stream.hls-url` | `STREAM_HLS_URL` | 移动端拉取的 hls(m3u8) 地址 |
| `liveroom.stream.publish-url` | `STREAM_PUBLISH_URL` | 推流地址，用于页面提示文案 |

三项默认都为空，此时页面不初始化播放器，但聊天室和弹幕照常可用。
搭建推流服务见 [DEPLOYMENT.md](DEPLOYMENT.md) 的 nginx-rtmp 一节。

> RTMP 播放依赖 Flash，现代浏览器已不再支持，`rtmp-url` 实际已无法在浏览器中播放。
> 要在浏览器里看直播请配置 `hls-url`。

## 业务参数

| 属性 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `liveroom.stat.max-guest-history` | `MAX_GUEST_HISTORY` | `2000` | Redis 中保留的访客记录条数 |
| `liveroom.chat.max-message-length` | `MAX_MESSAGE_LENGTH` | `200` | 单条聊天消息字数上限，超出截断 |
| `liveroom.trust-proxy-headers` | `TRUST_PROXY_HEADERS` | `false` | 是否信任反向代理的 IP 头部 |

### 关于 `trust-proxy-headers`

本项目拿访客 IP 当身份（数据库主键 + 昵称绑定），而 `X-Forwarded-For`
是客户端可以随手伪造的头部。

- **直接对外暴露**（没有反向代理）→ 保持 `false`。打开的话任何人都能伪造身份。
- **部署在 Nginx 等反向代理后面** → 设为 `true`，否则所有访客的 IP 都会变成代理的地址，
  统计功能失去意义。同时必须确保代理会**覆写**（而不是追加）这个头部：

  ```nginx
  proxy_set_header X-Forwarded-For $remote_addr;   # 覆写，不要用 $proxy_add_x_forwarded_for
  ```

## Actuator

默认配置：

```properties
endpoints.enabled=false
endpoints.health.enabled=true
endpoints.health.sensitive=false
endpoints.info.enabled=true
management.context-path=/manage
```

即只开放 `/LiveDemo/manage/health` 和 `/LiveDemo/manage/info`。

Spring Boot 1.5 在 classpath 没有 Spring Security 时，actuator 端点是**完全公开**的，
`/env`、`/configprops` 会把包括数据库密码在内的环境变量直接打印出来。因此默认全部关闭。
需要开启其他端点时，请务必同时加上访问控制。

## 日志

| 属性 | 环境变量 | 默认值 |
| --- | --- | --- |
| `logging.level.com.liveroom` | `LOG_LEVEL` | `INFO` |

排查握手失败、订阅被拒等问题时设为 `DEBUG`：

```bash
export LOG_LEVEL=DEBUG
```

# 安全说明

## ⚠️ 请先读这一段

**本项目是一个学习/演示项目，不建议直接部署到公网。**

它基于 **Spring Boot 1.5.4**（2017 年发布，2019 年 8 月已停止维护）。
这次整理刻意没有升级框架版本，目的是保留项目原貌。因此：

- Spring Boot 1.5.x 及其依赖链存在多个已公开的 CVE，且**不会再有安全更新**
- 项目**没有任何登录/鉴权机制**，访客身份仅靠 IP 识别，可被伪造
- 聊天内容不做审核，也没有频率限制

如果你打算把它用在真实场景，至少要先做这几件事：

1. 升级到仍在维护的 Spring Boot 版本（2.7.x 仍支持 Java 8，3.x 需要 Java 17）
2. 引入 Spring Security，用真正的账号体系替代「IP 即身份」
3. 给聊天接口加频率限制和内容过滤
4. 把服务放在反向代理之后，只暴露必要端口

## 本次整理修复的安全问题

| 问题 | 原状 | 现状 |
| --- | --- | --- |
| 无鉴权的调试接口 | `Test.java` 暴露了可任意写库、伪造在线用户、向全站广播消息的接口 | 整个类已删除 |
| CORS 配置 | `allowedOrigins("*")` + `allowCredentials(true)`，任意站点可带 Cookie 调接口 | 改为可配置的来源白名单 |
| WebSocket 来源 | SockJS 端点 `setAllowedOrigins("*")` | 与 CORS 共用白名单 |
| Actuator 未保护 | `/env`、`/configprops` 公开可访问，会泄露数据库密码 | 默认只开放 health / info，并移至 `/manage` |
| IP 伪造 | 无条件信任 `X-Forwarded-For`，而 IP 就是访客身份主键 | 改为默认不信任，由 `TRUST_PROXY_HEADERS` 显式开启 |
| Redis 反序列化 | ObjectMapper 开启 `enableDefaultTyping(NON_FINAL)`，可被诱导实例化任意类型 | 去掉多态类型信息 |
| 个人隐私数据 | 仓库内含真实姓名、手机号、邮箱、简历页面和个人照片 | 已全部移除 |
| 明文密码 | 配置文件中直接写数据库/MQ 账号 | 全部改为 `${环境变量:默认值}` |


## 部署时请注意

- `CORS_ALLOWED_ORIGINS` **不要**填 `*`
- 数据库用权限受限的专用账号，不要用 root
- Redis 设 `requirepass`，不要监听公网
- RabbitMQ 不要用默认的 `guest/guest`
- 所有密码通过环境变量注入，不要写进 `application.properties`
- `TRUST_PROXY_HEADERS` 只在确有可信反向代理时开启

完整清单见 [docs/DEPLOYMENT.md 的上线前检查清单](docs/DEPLOYMENT.md#7-上线前检查清单)。

## 报告漏洞

发现安全问题请**不要**公开提 Issue。

请通过 GitHub 的
[私密安全公告](https://github.com/jack-hoo/LiveRoomDemo_Server/security/advisories/new)
功能反馈，其中包含：

- 问题描述和影响范围
- 复现步骤
- 如果有，修复建议

由于这是一个业余维护的演示项目，响应时间无法保证，请谅解。

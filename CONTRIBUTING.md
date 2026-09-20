# 贡献指南

感谢你愿意为 LiveRoomDemo 出一份力。这是一个以学习和演示为目的的项目，
所以**可读性优先于炫技**，代码清楚好懂比少写两行更重要。

## 提 Issue

提问题前请先搜一下已有的 Issue，避免重复。

**报 Bug** 请提供：

- 复现步骤（越具体越好）
- 期望的行为 vs 实际的行为
- 环境：JDK 版本（`java -version`）、MySQL / Redis / RabbitMQ 版本、操作系统
- 相关日志。设 `LOG_LEVEL=DEBUG` 能拿到更多信息

**提需求**请说明你想解决的问题，而不只是你想要的方案——知道背景才好判断有没有更简单的做法。

## 提 Pull Request

```bash
# 1. Fork 后 clone 你的仓库
git clone https://github.com/你的用户名/LiveRoomDemo_Server.git
cd LiveRoomDemo_Server

# 2. 开一个分支
git checkout -b fix/handshake-npe

# 3. 改代码，确保测试通过
JAVA_HOME=/path/to/jdk8 mvn test

# 4. 提交并推送
git commit -m "fix: 握手拦截器在 session 为空时抛 NPE"
git push origin fix/handshake-npe
```

然后在 GitHub 上发起 PR。

### 前置条件

需要 **JDK 8**。Spring Boot 1.5.x 不支持更高版本的 JDK 编译，
`mvn` 会直接失败。系统默认不是 8 的话用 `JAVA_HOME` 临时指定。

### 代码规范

- 缩进 4 空格，不用 Tab
- 文件编码 UTF-8，换行符 LF
- 类、公开方法写 Javadoc；**注释写「为什么」，不写「是什么」**
- 不要提交注释掉的死代码，Git 会记住它
- 不要提交带真实密码的配置。所有敏感项都走 `${环境变量:默认值}`
- 新增配置项请同步更新 [docs/CONFIGURATION.md](docs/CONFIGURATION.md)

### 关于框架版本

本项目**刻意保持 Spring Boot 1.5.4 不动**，以保留项目原貌。
升级框架版本的 PR 请先开 Issue 讨论——这会牵动几乎所有文件，需要先对齐目标。

修 Bug、补测试、改文档的 PR 随时欢迎。

### 提交信息

推荐 [Conventional Commits](https://www.conventionalcommits.org/) 风格：

```
fix: 修复访客历史列表无限增长
feat: 支持通过配置关闭 RabbitMQ 依赖
docs: 补充 nginx-rtmp 部署步骤
test: 补充 IpUtil 的转发链解析用例
refactor: 提取订阅白名单常量
chore: 更新 .gitignore
```

中英文都可以，说清楚做了什么就行。

### 测试

改了 `service/` 下的工具类，请一并补上单元测试。现有测试都不依赖外部服务，
请保持这个特性——CI 上没有 MySQL / Redis / RabbitMQ。

```bash
JAVA_HOME=/path/to/jdk8 mvn test
```

## 行为准则

参与本项目即表示你同意遵守 [行为准则](CODE_OF_CONDUCT.md)。

## 安全问题

发现安全漏洞**请不要**公开提 Issue，按 [SECURITY.md](SECURITY.md) 的方式私下反馈。

## 许可

提交代码即表示你同意以 [MIT 协议](LICENSE) 授权你的贡献。

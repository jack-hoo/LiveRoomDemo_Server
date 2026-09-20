# 部署文档

本文覆盖从零搭起一套完整直播间的全部步骤。只想在本机跑起来看看效果的话，
看 [README.zh-CN.md 的「快速开始」](../README.zh-CN.md#快速开始) 就够了，
本文的第 4、5 节（推流服务、反向代理）可以跳过。

- [1. 准备依赖服务](#1-准备依赖服务)
- [2. 构建](#2-构建)
- [3. 启动方式](#3-启动方式)
- [4. 搭建推流服务（nginx-rtmp）](#4-搭建推流服务nginx-rtmp)
- [5. Nginx 反向代理](#5-nginx-反向代理)
- [6. 同步移动端构建产物](#6-同步移动端构建产物)
- [7. 上线前检查清单](#7-上线前检查清单)
- [8. 常见问题](#8-常见问题)

---

## 1. 准备依赖服务

### 1.1 JDK 8

Spring Boot 1.5.x 只能用 JDK 8 编译，更高版本会直接编译失败。

```bash
# Debian / Ubuntu
sudo apt install openjdk-8-jdk

# CentOS
sudo yum install java-1.8.0-openjdk-devel
```

如果机器上装了多个 JDK，用 `JAVA_HOME` 临时指定，不用改系统默认：

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn clean package
```

### 1.2 MySQL

```bash
mysql -u root -p < docs/sql/schema.sql
```

建议为应用单独建一个只有必要权限的账号，不要直接用 root：

```sql
CREATE USER 'livedemo'@'localhost' IDENTIFIED BY '换成你自己的强密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON livedemo.* TO 'livedemo'@'localhost';
FLUSH PRIVILEGES;
```

### 1.3 Redis

```bash
redis-server /etc/redis/redis.conf
```

生产环境务必：

- 在 `redis.conf` 里设置 `requirepass`
- 绑定内网地址（`bind 127.0.0.1`），不要监听公网
- 保持 `protected-mode yes`

### 1.4 RabbitMQ（可选）

只有多实例部署时才必须。单机跑 demo 设 `BROKER_RELAY_ENABLED=false` 即可跳过本节。

```bash
# 启用 STOMP 插件，默认监听 61613
rabbitmq-plugins enable rabbitmq_stomp
systemctl restart rabbitmq-server

# 确认端口已监听
ss -lntp | grep 61613
```

默认的 `guest/guest` 账号只能从 localhost 连接，生产环境请另建账号：

```bash
rabbitmqctl add_user liveroom 换成你自己的强密码
rabbitmqctl set_permissions -p / liveroom ".*" ".*" ".*"
```

## 2. 构建

```bash
JAVA_HOME=/path/to/jdk8 mvn clean package
```

产物是 `target/LiveDemo.war`。这个 war 有两种用法：

- 直接 `java -jar` 启动（内嵌 Tomcat）
- 丢进外部 Tomcat 的 `webapps/` 目录

> `pom.xml` 里把 `finalName` 固定成了 `LiveDemo`，因为部署到外部 Tomcat 时
> **war 包名就是访问路径**，而前端的接口前缀写死为 `/LiveDemo`。改名会导致前端调不通接口。

跳过测试：`mvn clean package -DskipTests`。

## 3. 启动方式

### 3.1 内嵌 Tomcat（推荐，最省事）

```bash
export MYSQL_USERNAME=livedemo
export MYSQL_PASSWORD=你的数据库密码
export REDIS_PASSWORD=你的redis密码
export BROKER_RELAY_ENABLED=false
export CORS_ALLOWED_ORIGINS=https://live.example.com

java -jar target/LiveDemo.war
```

访问 `http://服务器地址:8085/LiveDemo/live_room`。

### 3.2 systemd 托管

`/etc/systemd/system/liveroom.service`：

```ini
[Unit]
Description=LiveRoomDemo Server
After=network.target mysql.service redis.service

[Service]
Type=simple
User=liveroom
WorkingDirectory=/opt/liveroom

# 敏感配置放在只有该用户可读的文件里：chmod 600 /opt/liveroom/liveroom.env
EnvironmentFile=/opt/liveroom/liveroom.env
ExecStart=/usr/lib/jvm/java-8-openjdk-amd64/bin/java -jar /opt/liveroom/LiveDemo.war

Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

`/opt/liveroom/liveroom.env`：

```bash
MYSQL_USERNAME=livedemo
MYSQL_PASSWORD=你的数据库密码
REDIS_PASSWORD=你的redis密码
BROKER_RELAY_ENABLED=false
CORS_ALLOWED_ORIGINS=https://live.example.com
TRUST_PROXY_HEADERS=true
STREAM_HLS_URL=https://live.example.com/hls/demo.m3u8
STREAM_PUBLISH_URL=rtmp://live.example.com/live/demo
```

```bash
sudo chmod 600 /opt/liveroom/liveroom.env
sudo systemctl daemon-reload
sudo systemctl enable --now liveroom
sudo journalctl -u liveroom -f
```

### 3.3 外部 Tomcat

```bash
cp target/LiveDemo.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```

访问路径是 `http://服务器地址:8080/LiveDemo/live_room`。

用外部容器时 `server.port` 和 `server.context-path` 都不生效，环境变量照常生效。
给 Tomcat 传环境变量可以写在 `$CATALINA_HOME/bin/setenv.sh` 里。

## 4. 搭建推流服务（nginx-rtmp）

想让「我要直播」按钮真正可用，需要一台支持 RTMP 的媒体服务器。

### 4.1 编译带 rtmp 模块的 Nginx

```bash
# 依赖
sudo apt install build-essential libpcre3-dev libssl-dev zlib1g-dev

# 源码
wget http://nginx.org/download/nginx-1.24.0.tar.gz
tar -zxvf nginx-1.24.0.tar.gz
git clone https://github.com/arut/nginx-rtmp-module.git

cd nginx-1.24.0
./configure --prefix=/usr/local/nginx \
            --with-http_ssl_module \
            --add-module=../nginx-rtmp-module
make && sudo make install
```

### 4.2 配置 RTMP 与 HLS

`/usr/local/nginx/conf/nginx.conf`：

```nginx
rtmp {
    server {
        listen 1935;
        chunk_size 4096;

        application live {
            live on;
            record off;

            # 同时切出 HLS，供移动端和现代浏览器播放
            hls on;
            hls_path /usr/local/nginx/html/hls;
            hls_fragment 3s;
            hls_playlist_length 60s;

            # 只允许本机推流；对外提供推流能力时请改用 on_publish 鉴权
            allow publish 127.0.0.1;
            deny publish all;
            allow play all;
        }
    }
}

http {
    server {
        listen 80;

        location /hls {
            types {
                application/vnd.apple.mpegurl m3u8;
                video/mp2t ts;
            }
            root /usr/local/nginx/html;
            add_header Cache-Control no-cache;
            add_header Access-Control-Allow-Origin *;
        }
    }
}
```

重载：`/usr/local/nginx/sbin/nginx -s reload`

### 4.3 推流与配置地址

用 OBS（电脑端）或任意支持 RTMP 的手机推流软件：

- 服务器：`rtmp://你的服务器地址/live`
- 串流码：`demo`

然后把地址配进应用：

```bash
export STREAM_PUBLISH_URL=rtmp://你的服务器地址/live/demo
export STREAM_HLS_URL=http://你的服务器地址/hls/demo.m3u8
export STREAM_RTMP_URL=rtmp://你的服务器地址/live/demo
```

> **RTMP 在浏览器里已经播不了了**：RTMP 播放依赖 Flash，主流浏览器 2021 年起全面移除了
> Flash 支持。`STREAM_RTMP_URL` 保留只是为了兼容老代码，实际请用 `STREAM_HLS_URL`。

## 5. Nginx 反向代理

把应用挂到 80/443 上，并正确转发 WebSocket：

```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

server {
    listen 443 ssl;
    server_name live.example.com;

    ssl_certificate     /etc/nginx/ssl/live.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/live.example.com.key;

    location /LiveDemo/ {
        proxy_pass http://127.0.0.1:8085;
        proxy_http_version 1.1;

        # SockJS 握手后要升级成 WebSocket，这两行缺一不可
        proxy_set_header Upgrade    $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        proxy_set_header Host $host;
        # 覆写而不是追加，避免客户端伪造 IP（本项目拿 IP 当访客身份）
        proxy_set_header X-Forwarded-For   $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 长连接，别让代理提前掐断
        proxy_read_timeout  3600s;
        proxy_send_timeout  3600s;
    }
}
```

配了反向代理之后，记得同步这两项：

```bash
export TRUST_PROXY_HEADERS=true
export CORS_ALLOWED_ORIGINS=https://live.example.com
```

## 6. 同步移动端构建产物

移动端单页在
[LiveRoomDemo_Client](https://github.com/jack-hoo/LiveRoomDemo_Client) 仓库，
构建产物需要手动拷到本仓库的静态资源目录。

```bash
# 在客户端仓库（需要 Node 8，原因见客户端 README）
npm run build          # 产物在 dist/
```

对应关系：

| 客户端产物 | 服务端位置 |
| --- | --- |
| `dist/index.html` | `src/main/resources/templates/live_m.html` |
| `dist/js/*.js` | `src/main/resources/static/js/` |
| `dist/css/*.css` | `src/main/resources/static/css/` |
| `dist/vonic/*` | `src/main/resources/static/vonic/` |

```bash
CLIENT=../LiveRoomDemo_Client
RES=src/main/resources

# 先删掉上一次构建留下的带 hash 的文件，否则会越积越多
rm -f $RES/static/js/app.*.js $RES/static/js/vendor.*.js \
      $RES/static/js/manifest.*.js $RES/static/css/app.*.css

cp $CLIENT/dist/index.html   $RES/templates/live_m.html
cp $CLIENT/dist/js/*.js      $RES/static/js/
cp $CLIENT/dist/css/*.css    $RES/static/css/
cp -r $CLIENT/dist/vonic/*   $RES/static/vonic/

# 源码映射文件体积很大（vendor 的 map 有 2MB 多），没必要打进 war 包
find $RES/static -name '*.map' -delete
```

产物里的资源路径都是相对形式（`js/xxx.js`），页面在 `/LiveDemo/live_room`，
正好解析到 `/LiveDemo/js/xxx.js`，与这里的目录结构天然对齐，拷过去即可用，
**不需要手工修改 HTML 里的任何路径**。

`live_m.html` 引用的是带 hash 的文件名（`app.70ce7c9bb0612eb3ea89.js` 这种），
每次构建 hash 都会变，所以 html 和 js/css 必须整套一起替换，不能只换其中一部分。

## 7. 上线前检查清单

- [ ] `CORS_ALLOWED_ORIGINS` 改成真实域名，没有留 `*`
- [ ] MySQL 用的是权限受限的专用账号，不是 root
- [ ] Redis 设了 `requirepass` 且没有监听公网
- [ ] RabbitMQ 没有在用默认的 `guest/guest`
- [ ] 所有密码通过环境变量注入，没有写进 `application.properties`
- [ ] `TRUST_PROXY_HEADERS` 与实际部署结构一致（有反代才开）
- [ ] Actuator 只开放了 health / info
- [ ] 确认过 [SECURITY.md](../SECURITY.md) 中关于框架版本的风险提示

## 8. 常见问题

### 页面能打开，但聊天室一直停在「连接中...」

按顺序排查：

1. **消息代理**：`BROKER_RELAY_ENABLED=true` 却没装 RabbitMQ 或没开 STOMP 插件。
   先设成 `false` 试试，能连上就说明是代理的问题。
2. **来源白名单**：浏览器控制台如果报跨域错误，检查 `CORS_ALLOWED_ORIGINS`
   是否包含你当前访问的域名和端口（协议、端口都要完全一致）。
3. **反向代理**：Nginx 少了 `Upgrade` / `Connection` 两个 header，SockJS 无法升级成
   WebSocket。可以把 `LOG_LEVEL=DEBUG` 打开看握手日志。

### 握手日志显示「session 中没有访客信息，拒绝握手」

WebSocket 握手要求 session 里已经有访客信息，而这是在访问 `/live_room` 时写入的。
直接访问 WebSocket 端点、或者中途换了浏览器 Cookie 都会触发这个拒绝。
先正常访问一次 `/live_room` 再连接即可。

### 启动报 `Table 'livedemo.user' doesn't exist`

建表脚本没执行。`spring.jpa.hibernate.ddl-auto=none` 不会自动建表：

```bash
mysql -u root -p < docs/sql/schema.sql
```

### 启动报 `Unknown system variable` 或连不上数据库

MySQL 8 与项目使用的 Connector/J 5.x 驱动不完全兼容。要用 MySQL 8 的话需要把
`pom.xml` 里的 `mysql-connector-java` 升到 8.x，并把
`spring.datasource.driver-class-name` 改成 `com.mysql.cj.jdbc.Driver`。
推荐直接用 MySQL 5.7。

### 编译报 `不支持发行版本 5` 或类似错误

用了 JDK 9 及以上。Spring Boot 1.5.x 必须用 JDK 8：

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn clean package
```

### 移动端页面白屏

多半是移动端构建产物和 `live_m.html` 的 hash 对不上。参考
[第 6 节](#6-同步移动端构建产物) 整套重新替换一次。

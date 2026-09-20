# LiveRoomDemo — Server

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-1.5.4-brightgreen.svg)](https://spring.io/projects/spring-boot)

The server side of a live-streaming room demo: HLS/RTMP playback, a STOMP-over-WebSocket
chat room, a bullet-screen (danmaku) overlay, and real-time viewer statistics.

**中文文档请看 [README.zh-CN.md](README.zh-CN.md)** — it is the primary documentation and
covers setup, configuration and deployment in detail.

---

## Features

- Pulls a live stream from a media server — HLS on mobile, RTMP on desktop
- WebSocket chat room built on STOMP + SockJS
- Bullet-screen comments synced with the chat
- Live online-viewer count and visitor history, backed by Redis
- Serves both a desktop page (Thymeleaf + jQuery) and a mobile SPA
  (the prebuilt bundle from [LiveRoomDemo_Client](https://github.com/jack-hoo/LiveRoomDemo_Client))

## Tech stack

| Layer | Choice |
| --- | --- |
| Framework | Spring Boot 1.5.4 (Java 8) |
| Primary store | MySQL, via spring-boot-starter-data-jpa |
| Real-time store | Redis, via spring-boot-starter-data-redis |
| Messaging | STOMP over WebSocket; RabbitMQ relay (optional) |
| Desktop frontend | Thymeleaf + jQuery + Bootstrap + video.js |
| Mobile frontend | Vue 2 SPA, built separately and served as static assets |

## Quick start

Requires **JDK 8** (Spring Boot 1.5 does not compile on newer JDKs), Maven 3.x,
MySQL 5.7+ and Redis 3.2+.

```bash
# 1. create the schema
mysql -u root -p < docs/sql/schema.sql

# 2. point the app at your services and start it
export MYSQL_USERNAME=root MYSQL_PASSWORD=yourpassword
export BROKER_RELAY_ENABLED=false          # skip RabbitMQ, use the in-memory broker
mvn spring-boot:run

# 3. open http://localhost:8085/LiveDemo/live_room
```

Full instructions — including nginx-rtmp setup, RabbitMQ STOMP, and WAR deployment
to an external Tomcat — are in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) (Chinese).
Every configuration key is documented in [docs/CONFIGURATION.md](docs/CONFIGURATION.md).

## Project status

This is a 2017 demo project that was cleaned up and re-released as a proper open-source
project. Bugs were fixed and personal data removed, but **the framework versions were
deliberately left untouched**.

That means Spring Boot 1.5.4, which reached end of life in August 2019 and has known
CVEs. Do not expose this to the public internet as-is — see [SECURITY.md](SECURITY.md).

Also note that **RTMP playback in the browser no longer works**: it depends on Flash,
which every major browser removed in 2021. Use an HLS source instead.

## Related repositories

- [LiveRoomDemo_Client](https://github.com/jack-hoo/LiveRoomDemo_Client) — the mobile Vue SPA

## Contributing

Issues and pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) first.

## License

[MIT](LICENSE) © jack-hoo

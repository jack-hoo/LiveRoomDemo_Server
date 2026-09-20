package com.liveroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 直播间 Demo 服务端启动类。
 *
 * <p>既可以用 {@code mvn spring-boot:run} / {@code java -jar} 以内嵌 Tomcat 方式启动，
 * 也可以打成 war 包部署到外部 Tomcat（见 {@link ServletInitializer}）。
 */
@SpringBootApplication
public class LiveRoomApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveRoomApplication.class, args);
    }
}

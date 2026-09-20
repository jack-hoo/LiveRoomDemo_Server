package com.liveroom;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

/**
 * 打成 war 包部署到外部 Tomcat 时的入口。
 *
 * <p>war 包名决定访问路径：{@code LiveDemo.war} 对应 {@code /LiveDemo}，这与前端写死的
 * 接口前缀一致。若要换成别的名字，需要同步修改前端的 {@code VUE_APP_API_BASE} 配置。
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(LiveRoomApplication.class);
    }
}

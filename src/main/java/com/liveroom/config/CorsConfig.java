package com.liveroom.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * 跨域配置。
 *
 * <p>原实现是 {@code allowedOrigins("*") + allowCredentials(true)}。这两个选项不能同时使用：
 * 带 Cookie 的跨域请求要求服务端回显一个具体的 Origin，而不是通配符。本项目用 session 里的
 * user 做身份识别，等于允许任意站点携带用户 Cookie 调用本站接口。
 *
 * <p>现在改为白名单，通过 {@code liveroom.cors.allowed-origins} 配置（逗号分隔），
 * 默认只放行前端开发服务器 {@code http://localhost:8080}。
 */
@Configuration
public class CorsConfig extends WebMvcConfigurerAdapter {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${liveroom.cors.allowed-origins:http://localhost:8080}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "DELETE", "PUT", "OPTIONS")
                .maxAge(3600);
    }
}

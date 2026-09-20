package com.liveroom.config;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * CORS 白名单是安全相关的配置：一旦退化成通配符，任意站点就能带着用户 Cookie 调本站接口。
 * 这里验证它确实是从配置读取并正确拆分的，而不是靠人眼检查。
 */
public class CorsConfigTest {

    /** CorsRegistry.getCorsConfigurations() 是 protected 的，开个子类把它暴露出来 */
    private static class ProbeCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }

    private AnnotationConfigApplicationContext context;

    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    private CorsConfiguration resolveConfig(Map<String, Object> properties) {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("test", properties));
        context.register(CorsConfig.class);
        context.refresh();

        ProbeCorsRegistry registry = new ProbeCorsRegistry();
        context.getBean(CorsConfig.class).addCorsMappings(registry);

        Map<String, CorsConfiguration> configurations = registry.configurations();
        assertEquals("应该只注册一条映射", 1, configurations.size());
        CorsConfiguration config = configurations.get("/**");
        assertNotNull("未注册 /** 的跨域配置", config);
        return config;
    }

    @Test
    public void bindsCommaSeparatedOriginsFromConfiguration() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("liveroom.cors.allowed-origins", "https://a.example.com,https://b.example.com");

        CorsConfiguration config = resolveConfig(properties);

        assertEquals(java.util.Arrays.asList("https://a.example.com", "https://b.example.com"),
                config.getAllowedOrigins());
        assertEquals(Boolean.TRUE, config.getAllowCredentials());
    }

    @Test
    public void fallsBackToLocalDevOriginWhenNotConfigured() {
        CorsConfiguration config = resolveConfig(Collections.<String, Object>emptyMap());

        assertEquals(Collections.singletonList("http://localhost:8080"), config.getAllowedOrigins());
    }

    /**
     * 原实现是 allowedOrigins("*") + allowCredentials(true)。这个组合本身就是矛盾的：
     * 带 Cookie 的跨域请求要求服务端回显具体的 Origin，而不是通配符。这里用测试把
     * 「默认配置不得出现通配符」这条约束钉死，防止以后有人图省事又改回去。
     */
    @Test
    public void defaultConfigurationNeverUsesWildcardOrigin() {
        CorsConfiguration config = resolveConfig(Collections.<String, Object>emptyMap());

        assertTrue("开启 allowCredentials 时不能使用通配符来源",
                Boolean.TRUE.equals(config.getAllowCredentials()));
        assertFalse("默认来源里不应出现 \"*\"", config.getAllowedOrigins().contains("*"));
    }
}

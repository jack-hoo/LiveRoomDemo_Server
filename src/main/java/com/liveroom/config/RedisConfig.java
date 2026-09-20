package com.liveroom.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置。
 *
 * <p>原实现有两个问题：
 * <ul>
 *   <li>返回类型声明成 {@code RedisTemplate<String, String>}，实际返回的却是
 *       {@link org.springframework.data.redis.core.StringRedisTemplate}，又给它装了一个写 JSON
 *       的 value 序列化器——声明的类型和真实行为对不上，调用方只能用裸类型规避编译检查。</li>
 *   <li>ObjectMapper 开了 {@code enableDefaultTyping(NON_FINAL)}，会把 Java 类名写进 Redis
 *       并在反序列化时据此实例化任意类型。一旦 Redis 里的数据可被篡改，这就是一条反序列化
 *       攻击链（Jackson 历史上多个 RCE 都走这条路）。</li>
 * </ul>
 *
 * <p>现在统一成 {@code RedisTemplate<String, Object>}：key/hashKey 用字符串序列化器保证
 * redis-cli 里可读，value 用不带多态类型信息的 JSON。读回来的是 Map 而不是原始实体类，
 * 但本项目的用法（统计在线用户、访客记录，最终都直接转成 JSON 返回前端）不需要还原具体类型。
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(factory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        Jackson2JsonRedisSerializer<Object> valueSerializer =
                new Jackson2JsonRedisSerializer<Object>(Object.class);
        valueSerializer.setObjectMapper(objectMapper());
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 有意不开启 default typing：序列化结果里不含 Java 类名，反序列化也就无法被诱导实例化任意类。
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}

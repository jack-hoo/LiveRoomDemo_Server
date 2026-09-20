package com.liveroom.dao;

import com.liveroom.entity.Guest;
import com.liveroom.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 基于 Redis 的实时统计：在线用户集合 + 访客历史列表。
 */
@Repository
public class StatDao {

    private static final String ONLINE_USER_KEY = "OnlineUser";
    private static final String GUEST_HISTORY_KEY = "Guest";

    private final RedisTemplate<String, Object> redisTemplate;
    private final long maxGuestHistory;

    @Autowired
    public StatDao(RedisTemplate<String, Object> redisTemplate,
                   @Value("${liveroom.stat.max-guest-history:2000}") long maxGuestHistory) {
        this.redisTemplate = redisTemplate;
        this.maxGuestHistory = maxGuestHistory;
    }

    public void pushOnlineUser(UserEntity userEntity) {
        redisTemplate.opsForSet().add(ONLINE_USER_KEY, userEntity);
    }

    public void popOnlineUser(UserEntity userEntity) {
        redisTemplate.opsForSet().remove(ONLINE_USER_KEY, userEntity);
    }

    public Set<Object> getAllUserOnline() {
        Set<Object> members = redisTemplate.opsForSet().members(ONLINE_USER_KEY);
        return members == null ? Collections.emptySet() : members;
    }

    /**
     * 记录一次访问，并把历史长度裁剪到上限以内。
     *
     * <p>原实现是 {@code if (size == 2000) rightPop()}：只要因为并发或历史数据让长度跨过了
     * 2000（比如直接变成 2001），这个相等判断就再也不会成立，列表会无限增长。另外
     * {@code opsForList().size()} 返回的是 {@code Long}，key 不存在时为 null，拿它和 long
     * 字面量比较会触发拆箱 NPE。这里改成循环裁剪 + null 安全。
     */
    public void pushGuestHistory(Guest guest) {
        redisTemplate.opsForList().leftPush(GUEST_HISTORY_KEY, guest);
        Long size = redisTemplate.opsForList().size(GUEST_HISTORY_KEY);
        if (size != null && size > maxGuestHistory) {
            redisTemplate.opsForList().trim(GUEST_HISTORY_KEY, 0, maxGuestHistory - 1);
        }
    }

    public List<Object> getGuestHistory() {
        List<Object> range = redisTemplate.opsForList().range(GUEST_HISTORY_KEY, 0, -1);
        return range == null ? Collections.emptyList() : range;
    }
}

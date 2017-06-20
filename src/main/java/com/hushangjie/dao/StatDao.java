package com.hushangjie.dao;

import com.hushangjie.entity.Guest;
import com.hushangjie.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Created by Administrator on 2017/6/16.
 */
@Repository
public class StatDao {
    @Autowired
    RedisTemplate redisTemplate;
    public void pushOnlineUser(UserEntity userEntity){
        redisTemplate.opsForSet().add("OnlineUser",userEntity);
    }
    public void popOnlineUser(UserEntity userEntity){
        redisTemplate.opsForSet().remove("OnlineUser" ,userEntity);
    }
    public Set getAllUserOnline(){
        return redisTemplate.opsForSet().members("OnlineUser");
    }
    public void pushGuestHistory(Guest guest){
        //最多存储指定个数的访客
        if (redisTemplate.opsForList().size("Guest") == 5l){
            redisTemplate.opsForList().rightPop("Guest");
        }
        redisTemplate.opsForList().leftPush("Guest",guest);
        /*Long guestsNow = redisTemplate.opsForZSet().zCard("Guest");
        if (guestsNow == 5l){
            HashSet<String> keys = (HashSet<String>) redisTemplate.opsForZSet().range("Guest",0 ,-1);
            String lastGuest = "";
            for(String key :keys){

            }
            redisTemplate.opsForZSet().remove("Guest",keys[0]);
        }
        redisTemplate.opsForZSet().add("Guest",userEntity,time);*/
    }
    /*public void popGuestHistory(Guest guest){
        redisTemplate.opsForList().remove("Guest",0,guest);
    }*/
    public List getGuestHistory(){
        return redisTemplate.opsForList().range("Guest",0,-1);
    }
}

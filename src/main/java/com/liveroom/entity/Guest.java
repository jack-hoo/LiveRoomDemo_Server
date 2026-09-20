package com.liveroom.entity;

import java.io.Serializable;

/**
 * 一条访客记录：谁、什么时候来的。存在 Redis 的列表里。
 */
public class Guest implements Serializable {

    private static final long serialVersionUID = 1L;

    private UserEntity userEntity;
    private long accessTime;

    public UserEntity getUserEntity() {
        return userEntity;
    }

    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }

    /** 访问时间，毫秒时间戳。 */
    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    @Override
    public String toString() {
        return "Guest{userEntity=" + userEntity + ", accessTime=" + accessTime + "}";
    }
}

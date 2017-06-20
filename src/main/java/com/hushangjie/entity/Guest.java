package com.hushangjie.entity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/6/16.
 */
public class Guest implements Serializable{
    private static final long serialVersionUID =1l;
    private UserEntity userEntity;
    private long accessTime;

    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }

    public long getAccessTime() {
        return accessTime;
    }
}

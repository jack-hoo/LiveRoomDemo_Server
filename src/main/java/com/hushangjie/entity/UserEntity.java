package com.hushangjie.entity;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Administrator on 2017/6/12.
 */
@Entity
@Table(name = "user", schema = "livedemo", catalog = "")
public class UserEntity implements Serializable{
    private static final long serialVersionUID = 1l;
    private String ip;
    private String randomName;

    @Id
    @Column(name = "ip")
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Basic
    @Column(name = "random_name")
    public String getRandomName() {
        return randomName;
    }

    public void setRandomName(String randomName) {
        this.randomName = randomName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserEntity that = (UserEntity) o;

        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (randomName != null ? !randomName.equals(that.randomName) : that.randomName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + (randomName != null ? randomName.hashCode() : 0);
        return result;
    }
}

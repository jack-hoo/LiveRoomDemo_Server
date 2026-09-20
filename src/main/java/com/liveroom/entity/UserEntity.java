package com.liveroom.entity;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * 访客。用 IP 作为主键，同一个 IP 再次访问时沿用之前分配的随机昵称。
 *
 * <p>原来写的是 {@code @Table(name = "user", schema = "livedemo", catalog = "")}，
 * 把库名写死在代码里。这样一来 JDBC 连的哪个库都没用，Hibernate 生成的 SQL 永远指向
 * {@code livedemo.user}，换个库名就报表不存在。库名应该只由数据源 URL 决定，这里去掉。
 */
@Entity
@Table(name = "user")
public class UserEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ip;
    private String randomName;

    @Id
    @Column(name = "ip", length = 64, nullable = false)
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Basic
    @Column(name = "random_name", length = 64)
    public String getRandomName() {
        return randomName;
    }

    public void setRandomName(String randomName) {
        this.randomName = randomName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserEntity that = (UserEntity) o;
        if (ip != null ? !ip.equals(that.ip) : that.ip != null) {
            return false;
        }
        return randomName != null ? randomName.equals(that.randomName) : that.randomName == null;
    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + (randomName != null ? randomName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserEntity{ip='" + ip + "', randomName='" + randomName + "'}";
    }
}

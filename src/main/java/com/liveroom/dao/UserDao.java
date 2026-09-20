package com.liveroom.dao;

import com.liveroom.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;

/**
 * 访客表。主键就是访客 IP。
 */
public interface UserDao extends CrudRepository<UserEntity, String> {
}

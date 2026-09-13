package com.example.demo.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    Page<User> selectList(Page<User> page, @Param("user") User user);

    User selectByUsername(String username);

    int insert(User user);

    int update(User user);

    int deleteByUsername(String username);
}

package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    List<User> selectList(User user);

    User selectByUsername(String username);

    int insert(User user);

    int update(User user);

    int deleteByUsername(String username);
}

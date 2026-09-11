package com.example.demo.service;

import com.example.demo.entity.User;

import java.util.List;

public interface UserService {

    List<User> queryUsers(User user);

    User getByUsername(String username);

    int create(User user);

    int update(User user);

    int delete(String username);
}

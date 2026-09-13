package com.baihailang.userservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baihailang.userservice.entity.User;

public interface UserService {

    Page<User> queryUsers(User user, int page, int pageSize);

    User getByUsername(String username);

    int create(User user);

    int update(User user);

    int delete(String username);
}

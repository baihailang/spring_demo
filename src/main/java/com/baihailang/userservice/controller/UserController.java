package com.baihailang.userservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baihailang.userservice.entity.User;
import com.baihailang.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户增删改查接口")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表", description = "page 从 1 开始，pagesize 范围为 1~100，可按用户名、手机号、昵称筛选")
    public Page<User> list(User user,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(name = "pagesize", defaultValue = "10") int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 必须大于 0，pagesize 范围为 1~100");
        }
        return userService.queryUsers(user, page, pageSize);
    }

    @GetMapping("/{username}")
    @Operation(summary = "查询单个用户", description = "根据用户名查询用户详情")
    public User get(@PathVariable String username) {
        return userService.getByUsername(username);
    }

    @PostMapping
    @Operation(summary = "新增用户", description = "创建新用户")
    public int create(@RequestBody User user) {
        return userService.create(user);
    }

    @PutMapping
    @Operation(summary = "更新用户", description = "根据用户名更新用户信息")
    public int update(@RequestBody User user) {
        return userService.update(user);
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "删除用户", description = "根据用户名删除用户")
    public int delete(@PathVariable String username) {
        return userService.delete(username);
    }
}

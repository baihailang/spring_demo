package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户增删改查接口")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    @Operation(summary = "查询用户列表", description = "按用户名、手机号、昵称条件查询用户列表")
    public List<User> list(User user) {
        return userService.queryUsers(user);
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

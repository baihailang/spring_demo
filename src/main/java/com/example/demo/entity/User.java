package com.example.demo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private String username;
    private String password;
    private String phone;
    private String email;
    private LocalDateTime created;
    private LocalDateTime updated;
    private String nickName;
    private String name;
}

package com.baihailang.userservice.service.impl;

import com.baihailang.userservice.cache.CacheNullValue;
import com.baihailang.userservice.constant.CacheConstants;
import com.baihailang.userservice.entity.User;
import com.baihailang.userservice.mapper.UserMapper;
import com.baihailang.userservice.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final Duration DETAIL_TTL = Duration.ofMinutes(30);
    private static final Duration DETAIL_NULL_TTL = Duration.ofSeconds(60);
    private static final Duration LIST_TTL = Duration.ofMinutes(5);

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(UserMapper userMapper, RedisTemplate<String, Object> redisTemplate,
                           StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Page<User> queryUsers(User user, int page, int pageSize) {
        String key = listCacheKey(user, page, pageSize);
        Page<User> cached = getPage(key);
        if (cached != null) {
            return cached;
        }
        Page<User> result = userMapper.selectList(new Page<>(page, pageSize), user);
        put(key, result, randomTtl(LIST_TTL));
        return result;
    }

    @Override
    public User getByUsername(String username) {
        String key = CacheConstants.USER_DETAIL_PREFIX + username;
        Object cached = get(key);
        if (cached instanceof CacheNullValue) {
            return null;
        }
        if (cached instanceof User user) {
            return user;
        }
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            put(key, new CacheNullValue(), randomTtl(DETAIL_NULL_TTL));
        } else {
            put(key, user, randomTtl(DETAIL_TTL));
        }
        return user;
    }

    @Override
    public int create(User user) {
        LocalDateTime now = LocalDateTime.now();
        user.setCreated(now);
        user.setUpdated(now);
        int rows = userMapper.insert(user);
        evict(user.getUsername());
        return rows;
    }

    @Override
    public int update(User user) {
        user.setUpdated(LocalDateTime.now());
        int rows = userMapper.update(user);
        evict(user.getUsername());
        return rows;
    }

    @Override
    public int delete(String username) {
        int rows = userMapper.deleteByUsername(username);
        evict(username);
        return rows;
    }

    private String listCacheKey(User user, int page, int pageSize) {
        String condition = user == null ? "all"
                : nullSafe(user.getUsername()) + '|' + nullSafe(user.getPhone()) + '|' + nullSafe(user.getNickName());
        String hash = DigestUtils.md5DigestAsHex((condition + '|' + page + '|' + pageSize).getBytes(StandardCharsets.UTF_8));
        return CacheConstants.USER_LIST_PREFIX + listVersion() + ':' + hash;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String listVersion() {
        try {
            String version = stringRedisTemplate.opsForValue().get(CacheConstants.USER_LIST_VERSION_KEY);
            return version == null ? "0" : version;
        } catch (Exception e) {
            log.warn("读取列表缓存版本失败，降级查询数据库: {}", e.getMessage());
            return "0";
        }
    }

    @SuppressWarnings("unchecked")
    private Page<User> getPage(String key) {
        Object cached = get(key);
        return cached instanceof Page<?> page ? (Page<User>) page : null;
    }

    private Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 读取失败，降级查询数据库, key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Redis 写入失败, key={}: {}", key, e.getMessage());
        }
    }

    private void evict(String username) {
        try {
            redisTemplate.delete(CacheConstants.USER_DETAIL_PREFIX + username);
            stringRedisTemplate.opsForValue().increment(CacheConstants.USER_LIST_VERSION_KEY);
        } catch (Exception e) {
            log.warn("清理用户缓存失败, username={}: {}", username, e.getMessage());
        }
    }

    private Duration randomTtl(Duration base) {
        long jitter = ThreadLocalRandom.current().nextLong(base.getSeconds() / 5 + 1);
        return base.plusSeconds(jitter);
    }
}

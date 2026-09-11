package com.example.demo;

import com.example.demo.constant.CacheConstants;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.config.RedisConfig;
import com.example.demo.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class UserCacheIntegrationTests {

    private LettuceConnectionFactory connectionFactory;
    private RedisTemplate<String, Object> redisTemplate;
    private StringRedisTemplate stringRedisTemplate;
    private FakeUserMapper userMapper;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        assumeTrue(redisAvailable(), "Redis 未启动，跳过缓存集成测试");
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new RedisConfig().redisTemplate(connectionFactory);
        stringRedisTemplate = new StringRedisTemplate(connectionFactory);
        stringRedisTemplate.afterPropertiesSet();
        userMapper = new FakeUserMapper();
        userService = new UserServiceImpl(userMapper, redisTemplate, stringRedisTemplate);
        clearCache();
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory == null) {
            return;
        }
        clearCache();
        connectionFactory.destroy();
    }

    @Test
    void cachesDetailAndServesFromCacheWithJitteredTtl() {
        User alice = user("alice");
        userMapper.stored = alice;

        assertNotNull(userService.getByUsername("alice"));
        assertNotNull(userService.getByUsername("alice"));
        assertEquals(1, userMapper.selectByUsernameCalls.get());

        Long ttl = redisTemplate.getExpire(CacheConstants.USER_DETAIL_PREFIX + "alice");
        assertNotNull(ttl);
        assertTrue(ttl > 30 * 60 && ttl <= 36 * 60, "detail TTL should be 1800-2160s but was " + ttl);
    }

    @Test
    void cachesNullValueToPreventPenetration() {
        assertNull(userService.getByUsername("ghost"));
        assertNull(userService.getByUsername("ghost"));
        assertEquals(1, userMapper.selectByUsernameCalls.get());

        Long ttl = redisTemplate.getExpire(CacheConstants.USER_DETAIL_PREFIX + "ghost");
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 72, "null cache TTL should be 60-72s but was " + ttl);
    }

    @Test
    void writeEvictsDetailAndBumpsListVersion() {
        User alice = user("alice");
        userMapper.stored = alice;
        userService.getByUsername("alice");
        String versionBefore = stringRedisTemplate.opsForValue().get(CacheConstants.USER_LIST_VERSION_KEY);

        userService.update(alice);

        assertNull(redisTemplate.opsForValue().get(CacheConstants.USER_DETAIL_PREFIX + "alice"));
        String versionAfter = stringRedisTemplate.opsForValue().get(CacheConstants.USER_LIST_VERSION_KEY);
        assertNotEquals(versionBefore, versionAfter);
    }

    @Test
    void listQueryIsCachedPerCondition() {
        userMapper.stored = user("alice");

        assertEquals(1, userService.queryUsers(null).size());
        assertEquals(1, userService.queryUsers(null).size());
        assertEquals(1, userMapper.selectListCalls.get());

        User condition = new User();
        condition.setUsername("alice");
        userService.queryUsers(condition);
        assertEquals(2, userMapper.selectListCalls.get());
    }

    private void clearCache() {
        Set<String> keys = redisTemplate.keys(CacheConstants.USER_DETAIL_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        Set<String> listKeys = redisTemplate.keys(CacheConstants.USER_LIST_PREFIX + "*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
        }
    }

    private static User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private static boolean redisAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 6379), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static class FakeUserMapper implements UserMapper {

        final AtomicInteger selectByUsernameCalls = new AtomicInteger();
        final AtomicInteger selectListCalls = new AtomicInteger();
        User stored;

        @Override
        public List<User> selectList(User user) {
            selectListCalls.incrementAndGet();
            List<User> result = new ArrayList<>();
            if (stored != null) {
                result.add(stored);
            }
            return result;
        }

        @Override
        public User selectByUsername(String username) {
            selectByUsernameCalls.incrementAndGet();
            return stored != null && stored.getUsername().equals(username) ? stored : null;
        }

        @Override
        public int insert(User user) {
            stored = user;
            return 1;
        }

        @Override
        public int update(User user) {
            stored = user;
            return 1;
        }

        @Override
        public int deleteByUsername(String username) {
            stored = null;
            return 1;
        }
    }
}

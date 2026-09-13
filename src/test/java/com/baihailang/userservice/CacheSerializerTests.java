package com.baihailang.userservice;

import com.baihailang.userservice.cache.CacheNullValue;
import com.baihailang.userservice.entity.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CacheSerializerTests {

    private final RedisSerializer<Object> serializer = GenericJacksonJsonRedisSerializer.create(builder ->
            builder.enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType("com.baihailang.userservice")
                    .allowIfSubType("com.baomidou.mybatisplus")
                    .allowIfSubType("java.util")
                    .allowIfSubType("java.time")
                    .build()));

    @Test
    void roundTripsUserWithLocalDateTime() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("pwd");
        user.setCreated(LocalDateTime.of(2026, 9, 11, 10, 30));
        user.setUpdated(LocalDateTime.of(2026, 9, 11, 11, 0));

        Object restored = serializer.deserialize(serializer.serialize(user));

        assertInstanceOf(User.class, restored);
        assertEquals(user, restored);
    }

    @Test
    void roundTripsListAndNullMarker() {
        User user = new User();
        user.setUsername("bob");
        Object restoredList = serializer.deserialize(serializer.serialize(new ArrayList<>(List.of(user))));

        assertInstanceOf(List.class, restoredList);
        User restoredUser = (User) ((List<?>) restoredList).get(0);
        assertEquals("bob", restoredUser.getUsername());

        Object restoredNull = serializer.deserialize(serializer.serialize(new CacheNullValue()));
        assertInstanceOf(CacheNullValue.class, restoredNull);
    }

    @Test
    void roundTripsPage() {
        User user = new User();
        user.setUsername("carol");
        Page<User> page = new Page<>(2, 10, 21);
        page.setRecords(new ArrayList<>(List.of(user)));

        Object restored = serializer.deserialize(serializer.serialize(page));

        assertInstanceOf(Page.class, restored);
        Page<?> restoredPage = (Page<?>) restored;
        assertEquals(2, restoredPage.getCurrent());
        assertEquals(10, restoredPage.getSize());
        assertEquals(21, restoredPage.getTotal());
        assertEquals("carol", ((User) restoredPage.getRecords().get(0)).getUsername());
    }
}

package com.baihailang.userservice.config;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class NacosConfigContent {

    private NacosConfigContent() {
    }

    public static Map<String, Object> toMap(String content, String type) throws Exception {
        if ("properties".equalsIgnoreCase(type)) {
            Properties properties = new Properties();
            properties.load(new java.io.StringReader(content));
            Map<String, Object> result = new LinkedHashMap<>();
            for (String name : properties.stringPropertyNames()) {
                result.put(name, properties.getProperty(name));
            }
            return result;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (PropertySource<?> source : loader.load("nacos-remote", new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)))) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    result.put(name, enumerable.getProperty(name));
                }
            }
        }
        return result;
    }
}

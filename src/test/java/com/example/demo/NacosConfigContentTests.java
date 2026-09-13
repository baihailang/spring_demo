package com.example.demo;

import com.example.demo.config.NacosConfigContent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NacosConfigContentTests {

    @Test
    void parsesYamlContent() throws Exception {
        Map<String, Object> properties = NacosConfigContent.toMap("server:\n  port: 9090\nfeature:\n  enabled: true\n", "yaml");

        assertEquals(9090, properties.get("server.port"));
        assertEquals(true, properties.get("feature.enabled"));
    }

    @Test
    void parsesPropertiesContent() throws Exception {
        Map<String, Object> properties = NacosConfigContent.toMap("server.port=9090\nfeature.enabled=true\n", "properties");

        assertEquals("9090", properties.get("server.port"));
        assertEquals("true", properties.get("feature.enabled"));
    }
}

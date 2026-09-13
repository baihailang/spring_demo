package com.example.demo.config;

import com.alibaba.nacos.api.PropertyKeyConst;

import java.util.Properties;

public final class NacosClientUtils {

    private NacosClientUtils() {
    }

    public static Properties clientProperties(NacosProperties properties) {
        Properties clientProperties = new Properties();
        clientProperties.put(PropertyKeyConst.SERVER_ADDR, properties.getServerAddr());
        if (properties.getNamespace() != null && !properties.getNamespace().isBlank()) {
            clientProperties.put(PropertyKeyConst.NAMESPACE, properties.getNamespace());
        }
        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            clientProperties.put(PropertyKeyConst.USERNAME, properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            clientProperties.put(PropertyKeyConst.PASSWORD, properties.getPassword());
        }
        return clientProperties;
    }
}

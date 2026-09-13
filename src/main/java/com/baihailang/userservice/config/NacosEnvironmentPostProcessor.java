package com.baihailang.userservice.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.Properties;

public class NacosEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROPERTY_SOURCE_NAME = "nacosRemoteConfig";
    private static final Logger log = LoggerFactory.getLogger(NacosEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        NacosProperties properties = bindProperties(environment);
        if (!properties.getConfig().isEnabled()) {
            return;
        }

        String dataId = properties.getConfig().getDataId();
        if (dataId == null || dataId.isBlank()) {
            dataId = environment.getProperty("spring.application.name", "application") + ".yaml";
        }

        if (properties.getConfig().isHttpOnly()) {
            loadFromHttp(environment, properties, dataId, null);
            return;
        }

        try {
            Properties clientProperties = NacosClientUtils.clientProperties(properties);
            ConfigService configService = NacosFactory.createConfigService(clientProperties);
            String content = configService.getConfig(dataId, properties.getConfig().getGroup(), properties.getConfig().getTimeout());
            loadRemoteConfig(environment, properties, dataId, content);
        } catch (Exception nacosClientException) {
            loadFromHttp(environment, properties, dataId, nacosClientException);
        }
    }

    private void loadFromHttp(ConfigurableEnvironment environment, NacosProperties properties, String dataId,
                              Exception nacosClientException) {
        try {
            String content = NacosHttpConfigClient.getConfig(properties, dataId, properties.getConfig().getGroup());
            loadRemoteConfig(environment, properties, dataId, content);
            log.info("已通过 Nacos HTTP API 加载配置: dataId={}, group={}", dataId, properties.getConfig().getGroup());
        } catch (Exception httpException) {
            if (nacosClientException == null) {
                log.warn("Nacos HTTP 配置加载失败，使用本地配置: dataId={}, group={}, error={}", dataId,
                        properties.getConfig().getGroup(), httpException.getMessage());
                return;
            }
            log.warn("Nacos 配置加载失败，使用本地配置: dataId={}, group={}, nativeError={}, httpError={}", dataId,
                    properties.getConfig().getGroup(), nacosClientException.getMessage(), httpException.getMessage());
        }
    }

    private void loadRemoteConfig(ConfigurableEnvironment environment, NacosProperties properties, String dataId,
                                  String content) throws Exception {
            if (content == null || content.isBlank()) {
                log.info("Nacos 配置为空，使用本地配置: dataId={}, group={}", dataId, properties.getConfig().getGroup());
                return;
            }
            Map<String, Object> source = NacosConfigContent.toMap(content, properties.getConfig().getType());
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, source));
            log.info("已加载 Nacos 配置: dataId={}, group={}", dataId, properties.getConfig().getGroup());
    }

    private NacosProperties bindProperties(ConfigurableEnvironment environment) {
        NacosProperties properties = new NacosProperties();
        properties.setServerAddr(environment.getProperty("nacos.server-addr", properties.getServerAddr()));
        properties.setNamespace(environment.getProperty("nacos.namespace", properties.getNamespace()));
        properties.setContextPath(environment.getProperty("nacos.context-path", properties.getContextPath()));
        properties.setUsername(environment.getProperty("nacos.username", properties.getUsername()));
        properties.setPassword(environment.getProperty("nacos.password", properties.getPassword()));
        properties.getConfig().setEnabled(environment.getProperty("nacos.config.enabled", Boolean.class, true));
        properties.getConfig().setDataId(environment.getProperty("nacos.config.data-id"));
        properties.getConfig().setGroup(environment.getProperty("nacos.config.group", "DEFAULT_GROUP"));
        properties.getConfig().setType(environment.getProperty("nacos.config.type", "yaml"));
        properties.getConfig().setTimeout(environment.getProperty("nacos.config.timeout", Long.class, 3000L));
        properties.getConfig().setHttpRefreshInterval(
                environment.getProperty("nacos.config.http-refresh-interval", Long.class, 5000L));
        properties.getConfig().setHttpOnly(environment.getProperty("nacos.config.http-only", Boolean.class, true));
        return properties;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}

package com.example.demo.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.Executor;

@Component
public class NacosConfigManager {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigManager.class);

    private final NacosProperties properties;
    private final ConfigurableEnvironment environment;
    private final ApplicationEventPublisher eventPublisher;
    private ConfigService configService;
    private String lastConfigSignature = "";

    public NacosConfigManager(NacosProperties properties, ConfigurableEnvironment environment,
                              ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.environment = environment;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void listen() {
        if (!properties.getConfig().isEnabled() || properties.getConfig().isHttpOnly()) {
            return;
        }
        String dataId = properties.getConfig().getDataId();
        if (dataId == null || dataId.isBlank()) {
            dataId = environment.getProperty("spring.application.name", "application") + ".yaml";
        }
        String finalDataId = dataId;
        try {
            configService = NacosFactory.createConfigService(NacosClientUtils.clientProperties(properties));
            configService.addListener(finalDataId, properties.getConfig().getGroup(), new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String content) {
                    refresh(finalDataId, content);
                }
            });
            log.info("已监听 Nacos 配置变更: dataId={}, group={}", finalDataId, properties.getConfig().getGroup());
        } catch (Exception e) {
            log.warn("Nacos 配置监听失败，应用将继续使用当前配置: dataId={}, server={}",
                    finalDataId, properties.getServerAddr(), e);
        }
    }

    private void refresh(String dataId, String content) {
        try {
            Map<String, Object> source = NacosConfigContent.toMap(content, properties.getConfig().getType());
            if (environment.getPropertySources().get(NacosEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)
                    instanceof MapPropertySource propertySource) {
                propertySource.getSource().clear();
                propertySource.getSource().putAll(source);
            } else {
                environment.getPropertySources().addFirst(
                        new MapPropertySource(NacosEnvironmentPostProcessor.PROPERTY_SOURCE_NAME, source));
            }
            lastConfigSignature = signature(content);
            eventPublisher.publishEvent(new NacosConfigRefreshEvent(dataId, properties.getConfig().getGroup()));
            log.info("Nacos 配置已刷新: dataId={}, group={}", dataId, properties.getConfig().getGroup());
        } catch (Exception e) {
            log.warn("Nacos 配置刷新失败: dataId={}, error={}", dataId, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${nacos.config.http-refresh-interval:5000}")
    public void refreshFromHttp() {
        if (!properties.getConfig().isEnabled()) {
            return;
        }
        String dataId = properties.getConfig().getDataId();
        if (dataId == null || dataId.isBlank()) {
            dataId = environment.getProperty("spring.application.name", "application") + ".yml";
        }
        try {
            String content = NacosHttpConfigClient.getConfig(properties, dataId, properties.getConfig().getGroup());
            if (content == null || content.isBlank() || signature(content).equals(lastConfigSignature)) {
                return;
            }
            refresh(dataId, content);
        } catch (Exception e) {
            log.debug("Nacos HTTP 配置轮询失败: dataId={}, error={}", dataId, e.getMessage());
        }
    }

    private String signature(String content) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    @PreDestroy
    public void close() {
        if (configService != null) {
            try {
                configService.shutDown();
            } catch (Exception e) {
                log.warn("关闭 Nacos 配置客户端失败: {}", e.getMessage());
            }
        }
    }
}

package com.example.demo.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Collections;

@Component
public class NacosServiceRegistrar {

    private static final Logger log = LoggerFactory.getLogger(NacosServiceRegistrar.class);

    private final NacosProperties properties;
    private final Environment environment;
    private NamingService namingService;
    private String ip;
    private int port;
    private String serviceName;
    private Instance instance;

    public NacosServiceRegistrar(NacosProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        if (!properties.getDiscovery().isEnabled()) {
            return;
        }
        serviceName = environment.getProperty("spring.application.name", "application");
        port = environment.getProperty("local.server.port", Integer.class,
                environment.getProperty("server.port", Integer.class, 8080));
        ip = properties.getDiscovery().getInstanceIp();
        if (ip == null || ip.isBlank()) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                log.warn("无法获取本机 IP，Nacos 注册失败: {}", e.getMessage());
                return;
            }
        }

        try {
            namingService = NacosFactory.createNamingService(NacosClientUtils.clientProperties(properties));
            instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setClusterName(properties.getDiscovery().getClusterName());
            instance.setWeight(properties.getDiscovery().getWeight());
            instance.setEphemeral(properties.getDiscovery().isEphemeral());
            instance.setMetadata(Collections.emptyMap());
            for (int attempt = 1; attempt <= 10; attempt++) {
                try {
                    namingService.registerInstance(serviceName, properties.getDiscovery().getGroup(), instance);
                    log.info("已注册 Nacos 服务: service={}, group={}, address={}:{}", serviceName,
                            properties.getDiscovery().getGroup(), ip, port);
                    return;
                } catch (Exception e) {
                    if (attempt == 10) {
                        throw e;
                    }
                    log.warn("Nacos 服务注册重试 {}/10: service={}, error={}", attempt, serviceName, e.getMessage());
                    Thread.sleep(1000L);
                }
            }
        } catch (Exception e) {
            log.warn("Nacos 服务注册失败，应用将继续启动: service={}, server={}, 请检查 9848/9849 gRPC 端口和防火墙",
                    serviceName, properties.getServerAddr(), e);
        }
    }

    @EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void deregister() {
        if (namingService == null || serviceName == null || instance == null) {
            return;
        }
        try {
            namingService.deregisterInstance(serviceName, properties.getDiscovery().getGroup(), instance);
            namingService.shutDown();
            log.info("已注销 Nacos 服务: service={}, address={}:{}", serviceName, ip, port);
        } catch (Exception e) {
            log.warn("Nacos 服务注销失败: service={}, error={}", serviceName, e.getMessage());
        }
    }
}

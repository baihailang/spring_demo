package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nacos")
public class NacosProperties {

    private String serverAddr = "127.0.0.1:8848";
    private String namespace = "";
    private String contextPath = "/nacos";
    private String username = "nacos";
    private String password = "nacos";
    private Config config = new Config();
    private Discovery discovery = new Discovery();

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public Discovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    public static class Config {
        private boolean enabled = true;
        private String dataId;
        private String group = "DEFAULT_GROUP";
        private String type = "yaml";
        private long timeout = 3000L;
        private long httpRefreshInterval = 5000L;
        private boolean httpOnly = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDataId() {
            return dataId;
        }

        public void setDataId(String dataId) {
            this.dataId = dataId;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public long getHttpRefreshInterval() {
            return httpRefreshInterval;
        }

        public void setHttpRefreshInterval(long httpRefreshInterval) {
            this.httpRefreshInterval = httpRefreshInterval;
        }

        public boolean isHttpOnly() {
            return httpOnly;
        }

        public void setHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
        }
    }

    public static class Discovery {
        private boolean enabled = true;
        private String group = "DEFAULT_GROUP";
        private String clusterName = "DEFAULT";
        private boolean ephemeral = true;
        private double weight = 1.0D;
        private String instanceIp;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String clusterName) {
            this.clusterName = clusterName;
        }

        public boolean isEphemeral() {
            return ephemeral;
        }

        public void setEphemeral(boolean ephemeral) {
            this.ephemeral = ephemeral;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public String getInstanceIp() {
            return instanceIp;
        }

        public void setInstanceIp(String instanceIp) {
            this.instanceIp = instanceIp;
        }
    }
}

package com.baihailang.userservice.config;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class NacosHttpConfigClient {

    private NacosHttpConfigClient() {
    }

    public static String getConfig(NacosProperties properties, String dataId, String group) throws Exception {
        String serverAddr = properties.getServerAddr().split(",")[0].trim();
        String baseUrl = serverAddr.startsWith("http://") || serverAddr.startsWith("https://")
                ? serverAddr : "http://" + serverAddr;
        String contextPath = properties.getContextPath();
        if (contextPath == null || contextPath.isBlank()) {
            contextPath = "/nacos";
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        String query = "dataId=" + encode(dataId) + "&group=" + encode(group)
                + "&tenant=" + encode(properties.getNamespace());
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + contextPath + "/v1/cs/configs?" + query))
                .timeout(Duration.ofMillis(properties.getConfig().getTimeout()))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConfig().getTimeout()))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Nacos HTTP 响应状态=" + response.statusCode());
        }
        return response.body();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}

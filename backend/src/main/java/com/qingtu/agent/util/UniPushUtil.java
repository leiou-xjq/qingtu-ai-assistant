package com.qingtu.agent.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * uni-push 推送工具类
 */
@Slf4j
@Component
public class UniPushUtil {

    @Value("${unipush.appid:}")
    private String appId;

    @Value("${unipush.appkey:}")
    private String appKey;

    @Value("${unipush.appsecret:}")
    private String appSecret;

    private static final String PUSH_API_URL = "https://rest-api.getui.com/v2/push/list";

    /**
     * 发送推送消息给指定用户
     *
     * @param clientId 用户客户端ID
     * @param title  推送标题
     * @param content 推送内容
     * @return 是否成功
     */
    public boolean pushToUser(String clientId, String title, String content) {
        if (clientId == null || clientId.isEmpty()) {
            log.warn("clientId 为空，无法推送");
            return false;
        }

        try {
            // 获取 access token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("获取 access_token 失败");
                return false;
            }

            // 构建推送请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("request_id", System.currentTimeMillis() + "");

            Map<String, Object> audience = new HashMap<>();
            audience.put("cid", new String[]{clientId});
            requestBody.put("audience", audience);

            Map<String, Object> pushMessage = new HashMap<>();
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("body", content);
            notification.put("click_type", "intent");
            notification.put("intent", "{\"action\":\"main\",\"params\":{\"url\":\"\"}}");
            pushMessage.put("notification", notification);

            Map<String, Object> settings = new HashMap<>();
            settings.put("ttl", 3600000);
            requestBody.put("push_message", pushMessage);
            requestBody.put("settings", settings);

            // 发送请求
            String url = PUSH_API_URL.replace("{appid}", appId);
            String response = sendPost(url, requestBody, accessToken);

            log.info("uni-push 推送结果: {}", response);
            return response != null && response.contains("success");

        } catch (Exception e) {
            log.error("uni-push 推送失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Access Token
     */
    private String getAccessToken() {
        try {
            String url = "https://rest-api.getui.com/v2/auth";

            Map<String, Object> body = new HashMap<>();
            body.put("grant_type", "client_credential");
            body.put("appkey", appKey);
            body.put("appsecret", appSecret);

            String response = sendPost(url, body, null);

            if (response != null && response.contains("\"code\":0")) {
                // 简单解析 token
                int tokenIndex = response.indexOf("\"token\":\"");
                if (tokenIndex > 0) {
                    int start = tokenIndex + 9;
                    int end = response.indexOf("\"", start);
                    return response.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.error("获取 access_token 异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 发送 HTTP POST 请求
     */
    private String sendPost(String url, Map<String, Object> body, String accessToken) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json");

            if (accessToken != null) {
                conn.setRequestProperty("token", accessToken);
            }

            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes("UTF-8"));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            }
            log.error("uni-push API 请求失败: {}", responseCode);
        } catch (Exception e) {
            log.error("uni-push 请求异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 检查配置是否完整
     */
    public boolean isConfigured() {
        return appId != null && !appId.isEmpty()
                && appKey != null && !appKey.isEmpty()
                && appSecret != null && !appSecret.isEmpty();
    }
}

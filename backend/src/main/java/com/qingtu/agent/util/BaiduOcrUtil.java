package com.qingtu.agent.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class BaiduOcrUtil {

    private static final String API_KEY = "VF3IunUMuVCNOYM6yApNST68";
    private static final String SECRET_KEY = "V8UI6arPVVupuEpqaYLrp9GMkLm34Kfk";

    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    private String cachedToken = null;
    private long tokenExpireTime = 0;

    private String getAccessToken() {
        // 检查缓存的token是否有效
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedToken;
        }

        try {
            String authUrl = TOKEN_URL + "?grant_type=client_credentials&client_id=" + API_KEY + "&client_secret=" + SECRET_KEY;

            URL url = new URL(authUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String resp = response.toString();
            log.info("百度Token响应: {}", resp);

            // 提取access_token
            Pattern pattern = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(resp);
            if (matcher.find()) {
                cachedToken = matcher.group(1);
                tokenExpireTime = System.currentTimeMillis() + 25 * 60 * 1000; // 25分钟过期
                return cachedToken;
            }
        } catch (Exception e) {
            log.error("获取百度AccessToken失败", e);
        }
        return null;
    }

    public String recognizeText(String imageBase64) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            log.error("无法获取AccessToken");
            return null;
        }

        try {
            String url = OCR_URL + "?access_token=" + accessToken;

            URL postUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) postUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // 构建请求体
            String params = "image=" + URLEncoder.encode(imageBase64, "UTF-8") +
                    "&language_type=CHN_ENG&detect_direction=true";

            conn.getOutputStream().write(params.getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().flush();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String resp = response.toString();
            log.info("百度OCR响应长度: {}", resp.length());

            return parseOcrResponse(resp);

        } catch (Exception e) {
            log.error("百度OCR识别失败", e);
            return null;
        }
    }

    private String parseOcrResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        try {
            // 提取words字段
            Pattern pattern = Pattern.compile("\"words\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(jsonResponse);
            while (matcher.find()) {
                if (text.length() > 0) {
                    text.append("\n");
                }
                text.append(matcher.group(1));
            }
        } catch (Exception e) {
            log.error("解析OCR响应失败", e);
        }

        return text.toString();
    }
}

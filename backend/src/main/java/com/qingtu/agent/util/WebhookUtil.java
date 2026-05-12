package com.qingtu.agent.util;

import okhttp3.*;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Webhook推送工具类
 * 
 * 功能说明：
 * - 支持钉钉机器人webhook推送
 * - 支持企业微信webhook推送
 * - 发送文本、Markdown格式消息
 * 
 * @author 青途智伴技术团队
 */
@Slf4j
@Component
public class WebhookUtil {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    /**
     * 发送钉钉消息
     * 
     * @param webhookUrl 钉钉webhook地址
     * @param content 消息内容
     * @return 是否发送成功
     */
    public boolean sendDingTalkMessage(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("钉钉webhook地址为空，跳过推送");
            return false;
        }

        JSONObject body = new JSONObject();
        body.put("msgtype", "text");
        JSONObject text = new JSONObject();
        text.put("content", content);
        body.put("text", text);

        return sendWebhook(webhookUrl, body.toJSONString());
    }

    /**
     * 发送钉钉Markdown消息
     * 
     * @param webhookUrl 钉钉webhook地址
     * @param title 标题
     * @param content Markdown内容
     * @return 是否发送成功
     */
    public boolean sendDingTalkMarkdown(String webhookUrl, String title, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("钉钉webhook地址为空，跳过推送");
            return false;
        }

        JSONObject body = new JSONObject();
        body.put("msgtype", "markdown");
        JSONObject markdown = new JSONObject();
        markdown.put("title", title);
        markdown.put("text", content);
        body.put("markdown", markdown);

        return sendWebhook(webhookUrl, body.toJSONString());
    }

    /**
     * 发送企业微信消息
     * 
     * @param webhookUrl 企业微信webhook地址
     * @param content 消息内容
     * @return 是否发送成功
     */
    public boolean sendWorkWeixinMessage(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("企业微信webhook地址为空，跳过推送");
            return false;
        }

        JSONObject body = new JSONObject();
        body.put("msgtype", "text");
        JSONObject text = new JSONObject();
        text.put("content", content);
        body.put("text", text);

        return sendWebhook(webhookUrl, body.toJSONString());
    }

    /**
     * 发送通用Webhook请求
     */
    private boolean sendWebhook(String url, String jsonBody) {
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("Webhook推送成功，响应：{}", responseBody);
                return true;
            } else {
                log.error("Webhook推送失败，状态码：{}", response.code());
                return false;
            }
        } catch (IOException e) {
            log.error("Webhook推送异常", e);
            return false;
        }
    }

    /**
     * 发送青途智伴早安推送消息
     * 
     * @param dingtalkUrl 钉钉webhook
     * @param workweixinUrl 企微webhook
     * @param title 标题
     * @param content 内容
     * @return 是否至少有一个发送成功
     */
    public boolean sendMorningPush(String dingtalkUrl, String workweixinUrl, String title, String content) {
        boolean dingtalkSuccess = sendDingTalkMarkdown(dingtalkUrl, title, content);
        boolean workweixinSuccess = sendWorkWeixinMessage(workweixinUrl, content);
        return dingtalkSuccess || workweixinSuccess;
    }
}
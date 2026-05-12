package com.qingtu.agent.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();

        RestTemplate restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory(okHttpClient));
        log.info("RestTemplate配置成功，使用OkHttpClient(连接5秒,读取10秒)");
        return restTemplate;
    }
}
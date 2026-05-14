package com.qingtu.agent.embedding.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeEmbeddingModel implements EmbeddingModel {

    private static final String URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final String MODEL = "text-embedding-v1";
    private static final int DIMENSIONS = 768;

    private final RestTemplate restTemplate;

    @Value("${ai.dashscope.api-key:}")
    private String apiKey;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank() || apiKey == null || apiKey.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("input", Map.of("texts", List.of(text)));

            var headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            var entity = new org.springframework.http.HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(URL, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode embeddings = root.path("output").path("embeddings");

            if (embeddings.isArray() && embeddings.size() > 0) {
                JsonNode vec = embeddings.get(0).path("embedding");
                if (vec.isArray()) {
                    float[] result = new float[vec.size()];
                    for (int i = 0; i < vec.size(); i++) {
                        result[i] = (float) vec.get(i).asDouble();
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Embedding generation failed: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }
}
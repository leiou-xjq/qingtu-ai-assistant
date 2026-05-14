package com.qingtu.agent.rag;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.rag.dto.KnowledgeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchRagStore {

    private static final String INDEX_NAME = "rag_knowledge";
    private static final int EMBEDDING_DIMS = 768;

    private final ElasticsearchClient esClient;
    private final RestTemplate restTemplate;

    @Value("${ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    public void initIndex() {
        try {
            boolean exists = esClient.indices().exists(
                ExistsRequest.of(e -> e.index(INDEX_NAME))
            ).value();

            if (exists) {
                log.info("ES索引已存在: {}", INDEX_NAME);
                return;
            }

            Map<String, Property> properties = new HashMap<>();
            properties.put("school", Property.of(p -> p.keyword(k -> k)));
            properties.put("schoolName", Property.of(p -> p.text(t -> t.analyzer("ik_smart"))));
            properties.put("category", Property.of(p -> p.keyword(k -> k)));
            properties.put("title", Property.of(p -> p.text(t -> t.analyzer("ik_smart"))));
            properties.put("question", Property.of(p -> p.text(t -> t.analyzer("ik_smart"))));
            properties.put("answer", Property.of(p -> p.text(t -> t.analyzer("ik_smart"))));
            properties.put("content", Property.of(p -> p.text(t -> t.analyzer("ik_smart"))));
            properties.put("source", Property.of(p -> p.keyword(k -> k)));
            properties.put("tags", Property.of(p -> p.keyword(k -> k)));
            properties.put("crawlTime", Property.of(p -> p.date(d -> d)));
            properties.put("createdAt", Property.of(p -> p.date(d -> d)));
            properties.put("embedding", Property.of(p -> p.denseVector(v -> v
                .dims(EMBEDDING_DIMS)
                .index(true)
                .similarity("cosine")
            )));

            esClient.indices().create(CreateIndexRequest.of(c -> c
                .index(INDEX_NAME)
                .mappings(m -> m.properties(properties))
            ));

            log.info("ES向量索引创建成功: {} (dims={})", INDEX_NAME, EMBEDDING_DIMS);

        } catch (Exception e) {
            log.error("ES索引初始化失败: {}", e.getMessage(), e);
        }
    }

    public void addDocument(KnowledgeDTO dto) {
        try {
            String content = dto.getContent() != null ? dto.getContent() : "";
            addDocument(dto.getCategory(), dto.getTitle(), content, dto.getSchool(), dto.getSchoolName(), dto.getSource(), dto.getTags());
        } catch (Exception e) {
            log.error("ES文档添加失败: {}", e.getMessage(), e);
        }
    }

    public void addDocument(String category, String title, String content) {
        addDocument(category, title, content, null, null, null, null);
    }

    public void addDocument(String category, String title, String content, String school, String schoolName, String source, String tags) {
        try {
            float[] embedding = embedText(content);

            Map<String, Object> doc = new HashMap<>();
            doc.put("school", school != null ? school : "");
            doc.put("schoolName", schoolName != null ? schoolName : "");
            doc.put("category", category != null ? category : "");
            doc.put("title", title != null ? title : "");
            doc.put("question", title != null ? title : "");
            doc.put("answer", content);
            doc.put("content", content);
            doc.put("source", source != null ? source : "");
            doc.put("tags", tags != null ? tags : "");
            doc.put("crawlTime", new Date());
            doc.put("createdAt", new Date());

            if (embedding != null) {
                doc.put("embedding", embedding);
            }

            esClient.index(IndexRequest.of(i -> i
                .index(INDEX_NAME)
                .document(doc)
            ));

            log.debug("ES文档添加成功: {} - {}", schoolName, title);

        } catch (Exception e) {
            log.error("ES文档添加失败: {}", e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> searchByKeyword(String keyword, int topK) {
        return vectorSearch(keyword, null, topK);
    }

    public List<Map<String, Object>> searchBySchoolAndKeyword(String school, String keyword, int topK) {
        return vectorSearch(keyword, school, topK);
    }

    public List<Map<String, Object>> searchCommon(String keyword, int topK) {
        return vectorSearch(keyword, "common", topK);
    }

    private List<Map<String, Object>> vectorSearch(String query, String school, int topK) {
        try {
            return knnSearch(query, school, topK);
        } catch (Exception e) {
            log.warn("向量检索失败，降级到文本检索: {}", e.getMessage());
            try {
                return textSearch(query, school, topK);
            } catch (Exception ex) {
                log.error("ES检索失败: {}", ex.getMessage());
                return new ArrayList<>();
            }
        }
    }

    private List<Map<String, Object>> knnSearch(String query, String school, int topK) {
        float[] embedding = embedText(query);
        if (embedding == null) {
            throw new RuntimeException("Embedding生成失败，无法执行向量检索");
        }

        List<Float> queryVector = new ArrayList<>(embedding.length);
        for (float v : embedding) {
            queryVector.add(v);
        }

        try {
            SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .size(topK)
                .knn(k -> k
                    .field("embedding")
                    .queryVector(queryVector)
                    .k(topK)
                    .numCandidates(topK * 2)
                );

            if (school != null && !school.isEmpty() && !"common".equals(school)) {
                builder.postFilter(f -> f.term(t -> t.field("school").value(school)));
            } else if ("common".equals(school)) {
                builder.query(q -> q.bool(b -> b
                    .should(s1 -> s1.term(t -> t.field("school").value("")))
                    .should(s2 -> s2.term(t -> t.field("school").value("common")))
                    .minimumShouldMatch("1")
                ));
            }

            SearchResponse<Map> response = esClient.search(builder.build(), Map.class);

            List<Map<String, Object>> results = new ArrayList<>();
            int filteredCount = 0;
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    Double score = hit.score();
                    if (score != null && score >= 0.5) {
                        Map<String, Object> source = hit.source();
                        source.put("_score", score);
                        results.add(source);
                    } else if (score != null) {
                        filteredCount++;
                    }
                }
            }

            if (filteredCount > 0) {
                log.debug("向量检索过滤: query={}, school={}, minScore=0.5, 过滤低分结果数={}, 保留结果数={}",
                    query, school, filteredCount, results.size());
            } else {
                log.debug("向量检索完成: query={}, school={}, topK={}, results={}",
                    query, school, topK, results.size());
            }
            return results;

        } catch (Exception e) {
            throw new RuntimeException("ES向量检索失败: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> textSearch(String keyword, String school, int topK) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .size(topK);

            if (school != null && !school.isEmpty() && !"common".equals(school)) {
                builder.query(q -> q.bool(b -> b
                    .must(m -> m.match(mc -> mc.field("question").query(keyword)))
                    .filter(f -> f.term(t -> t.field("school").value(school)))
                ));
            } else if ("common".equals(school)) {
                builder.query(q -> q.bool(b -> b
                    .should(s1 -> s1.term(t -> t.field("school").value("")))
                    .should(s2 -> s2.term(t -> t.field("school").value("common")))
                    .minimumShouldMatch("1")
                    .must(m -> m.match(mc -> mc.field("question").query(keyword)))
                ));
            } else {
                builder.query(q -> q.match(m -> m.field("question").query(keyword)));
            }

            SearchResponse<Map> response = esClient.search(builder.build(), Map.class);

            List<Map<String, Object>> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    results.add(hit.source());
                }
            }
            return results;

        } catch (Exception e) {
            log.error("ES文本搜索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public long countBySchool(String school) {
        try {
            SearchResponse<Map> response = esClient.search(SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .size(0)
                .query(q -> q.term(t -> t.field("school").value(school)))
            ), Map.class);
            return response.hits().total() != null ? response.hits().total().value() : 0;
        } catch (Exception e) {
            log.error("ES统计失败: {}", e.getMessage());
            return 0;
        }
    }

    public long countCommon() {
        try {
            SearchResponse<Map> response = esClient.search(SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .size(0)
                .query(q -> q.bool(b -> b
                    .should(s1 -> s1.term(t -> t.field("school").value("")))
                    .should(s2 -> s2.term(t -> t.field("school").value("common")))
                    .minimumShouldMatch("1")
                ))
            ), Map.class);
            return response.hits().total() != null ? response.hits().total().value() : 0;
        } catch (Exception e) {
            log.error("ES统计失败: {}", e.getMessage());
            return 0;
        }
    }

    public float[] embedText(String text) {
        if (text == null || text.isBlank() || dashscopeApiKey == null || dashscopeApiKey.isBlank()) {
            return null;
        }

        try {
            String url = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

            Map<String, Object> body = new HashMap<>();
            body.put("model", "text-embedding-v1");
            body.put("input", Map.of("texts", List.of(text)));

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + dashscopeApiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            var entity = new org.springframework.http.HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, entity, String.class);

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
            log.error("Embedding生成失败: {}", e.getMessage());
        }
        return null;
    }

    public void clearAll() {
        try {
            esClient.deleteByQuery(d -> d
                .index(INDEX_NAME)
                .query(q -> q.matchAll(m -> m))
            );
            log.info("ES索引已清空: {}", INDEX_NAME);
        } catch (Exception e) {
            log.error("ES清空失败: {}", e.getMessage(), e);
        }
    }

    public void recreateIndex() {
        try {
            if (esClient.indices().exists(e -> e.index(INDEX_NAME)).value()) {
                esClient.indices().delete(d -> d.index(INDEX_NAME));
                log.info("ES索引已删除: {}", INDEX_NAME);
            }
            initIndex();
            log.info("ES索引已重建: {}", INDEX_NAME);
        } catch (Exception e) {
            log.error("ES索引重建失败: {}", e.getMessage(), e);
        }
    }
}
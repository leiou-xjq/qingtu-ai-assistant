package com.qingtu.agent.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RAG 检索评测测试
 * 验证检索系统的 Hit Rate、MRR 和延迟是否达标
 * <p>
 * 执行方式: mvn test -Dtest=RagEvaluationTest
 */
@Slf4j
@SpringBootTest
class RagEvaluationTest {

    @Autowired
    private RagServiceCore ragServiceCore;

    private List<RagSearchEvaluator.TestQuestion> testQuestions;

    @BeforeEach
    void loadTestData() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-data/rag-test-questions.json");

        if (is == null) {
            log.warn("测试数据集未找到: test-data/rag-test-questions.json");
            return;
        }

        List<Map<String, Object>> rawData = mapper.readValue(is,
            new TypeReference<List<Map<String, Object>>>() {});

        testQuestions = rawData.stream()
            .map(item -> {
                RagSearchEvaluator.TestQuestion q = new RagSearchEvaluator.TestQuestion(
                    (String) item.get("id"),
                    (String) item.get("category"),
                    (String) item.get("question"),
                    (List<String>) item.get("keywords"),
                    ((Number) item.get("minRelevantDocs")).intValue()
                );
                return q;
            })
            .toList();

        log.info("加载测试数据: {} 条问题", testQuestions.size());
    }

    @Test
    @DisplayName("Hit Rate@5 应达到 60% 以上")
    void testHitRate() {
        RagSearchEvaluator evaluator = new RagSearchEvaluator(ragServiceCore);
        RagSearchEvaluator.EvalResult result = evaluator.evaluate(testQuestions, 5);

        log.info(result.toString());

        assertTrue(result.hitRateAtK() > 0.6,
            String.format("Hit Rate@5 = %.2f%%, 未达到 60%% 阈值", result.hitRateAtK() * 100));
    }

    @Test
    @DisplayName("MRR 应达到 0.3 以上")
    void testMRR() {
        RagSearchEvaluator evaluator = new RagSearchEvaluator(ragServiceCore);
        RagSearchEvaluator.EvalResult result = evaluator.evaluate(testQuestions, 5);

        assertTrue(result.mrr() > 0.3,
            String.format("MRR = %.4f, 未达到 0.3 阈值", result.mrr()));
    }

    @Test
    @DisplayName("P95 检索延迟应小于 3000ms")
    void testLatency() {
        RagSearchEvaluator evaluator = new RagSearchEvaluator(ragServiceCore);
        RagSearchEvaluator.EvalResult result = evaluator.evaluate(testQuestions, 5);

        assertTrue(result.p95LatencyMs() < 3000,
            String.format("P95 延迟 = %.0fms, 超过 3000ms 阈值", result.p95LatencyMs()));
    }
}

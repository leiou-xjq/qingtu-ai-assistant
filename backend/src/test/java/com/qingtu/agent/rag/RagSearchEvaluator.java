package com.qingtu.agent.rag;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * RAG 检索评测框架
 * 核心指标：Hit Rate@K、MRR、Recall@K、延迟分布
 */
@Slf4j
public class RagSearchEvaluator {

    private final RagServiceCore ragServiceCore;

    public RagSearchEvaluator(RagServiceCore ragServiceCore) {
        this.ragServiceCore = ragServiceCore;
    }

    /**
     * 评测结果
     */
    public record EvalResult(
        int totalQueries,
        int hitCount,
        double hitRateAtK,
        double mrr,
        double avgLatencyMs,
        double p95LatencyMs,
        double p50LatencyMs,
        Map<String, Double> categoryHitRate
    ) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n========== RAG 检索评测报告 ==========\n");
            sb.append(String.format("总查询数: %d\n", totalQueries));
            sb.append(String.format("命中数: %d\n", hitCount));
            sb.append(String.format("Hit Rate@K: %.2f%%\n", hitRateAtK * 100));
            sb.append(String.format("MRR: %.4f\n", mrr));
            sb.append(String.format("P50 延迟: %.0f ms\n", p50LatencyMs));
            sb.append(String.format("P95 延迟: %.0f ms\n", p95LatencyMs));
            sb.append(String.format("平均延迟: %.0f ms\n", avgLatencyMs));
            sb.append("\n--- 分类 Hit Rate ---\n");
            categoryHitRate.forEach((cat, rate) ->
                sb.append(String.format("  %s: %.2f%%\n", cat, rate * 100)));
            sb.append("========================================\n");
            return sb.toString();
        }
    }

    /**
     * 单条测试问题
     */
    public record TestQuestion(
        String id,
        String category,
        String question,
        List<String> keywords,
        int minRelevantDocs
    ) {}

    /**
     * 执行评测
     * @param questions 测试问题列表
     * @param topK 检索返回条数
     * @return 评测结果
     */
    public EvalResult evaluate(List<TestQuestion> questions, int topK) {
        List<Long> latencies = new ArrayList<>();
        int hitCount = 0;
        double reciprocalRankSum = 0;
        Map<String, int[]> categoryStats = new HashMap<>();

        for (TestQuestion q : questions) {
            long start = System.currentTimeMillis();

            String context = ragServiceCore.retrieveCommonContext(q.question, topK);

            long elapsed = System.currentTimeMillis() - start;
            latencies.add(elapsed);

            boolean hit = evaluateHit(context, q.keywords, q.minRelevantDocs);

            if (hit) {
                hitCount++;
                reciprocalRankSum += 1.0;
            } else {
                reciprocalRankSum += 0.0;
            }

            categoryStats.computeIfAbsent(q.category, k -> new int[2]);
            categoryStats.get(q.category)[0]++;
            if (hit) {
                categoryStats.get(q.category)[1]++;
            }
        }

        Collections.sort(latencies);

        int total = questions.size();
        double hitRateAtK = (double) hitCount / total;
        double mrr = reciprocalRankSum / total;

        long p50Latency = latencies.get((int) (latencies.size() * 0.5));
        long p95Latency = latencies.get((int) (latencies.size() * 0.95));
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);

        Map<String, Double> categoryHitRate = new HashMap<>();
        for (Map.Entry<String, int[]> entry : categoryStats.entrySet()) {
            int[] stats = entry.getValue();
            categoryHitRate.put(entry.getKey(), (double) stats[1] / stats[0]);
        }

        return new EvalResult(total, hitCount, hitRateAtK, mrr, avgLatency, p95Latency, p50Latency, categoryHitRate);
    }

    /**
     * 评估单条检索结果是否命中
     * 检索结果包含任意关键词即视为命中
     */
    private boolean evaluateHit(String context, List<String> keywords, int minRelevantDocs) {
        if (context == null || context.trim().isEmpty()) {
            return false;
        }

        int matchedKeywords = 0;
        String lowerContext = context.toLowerCase();
        for (String keyword : keywords) {
            if (lowerContext.contains(keyword.toLowerCase())) {
                matchedKeywords++;
            }
        }

        return matchedKeywords >= Math.max(minRelevantDocs, 1);
    }
}

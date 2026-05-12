package com.qingtu.agent.agent.orchestrator;

import com.qingtu.agent.agent.context.UserContext;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 意图分析结果
 */
@Data
public class IntentResult {

    private String intent;
    private double confidence;
    private Map<String, Object> parameters;
    private boolean requiresFiles;
    private List<String> subIntents;

    public static IntentResult of(String intent, double confidence, Map<String, Object> parameters) {
        IntentResult result = new IntentResult();
        result.setIntent(intent);
        result.setConfidence(confidence);
        result.setParameters(parameters);
        result.setRequiresFiles(false);
        return result;
    }

    public static IntentResult withFiles(String intent, double confidence, Map<String, Object> parameters) {
        IntentResult result = of(intent, confidence, parameters);
        result.setRequiresFiles(true);
        return result;
    }

    public static IntentResult multi(List<String> subIntents) {
        IntentResult result = new IntentResult();
        result.setSubIntents(subIntents);
        result.setConfidence(1.0);
        return result;
    }
}
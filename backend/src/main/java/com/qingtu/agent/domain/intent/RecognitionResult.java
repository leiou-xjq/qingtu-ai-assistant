package com.qingtu.agent.domain.intent;

import lombok.Data;
import java.util.Map;

@Data
public class RecognitionResult {
    private String intent;
    private double confidence;
    private String action;
    private Map<String, Object> parameters;

    public static RecognitionResult of(String intent, double confidence, String action, Map<String, Object> parameters) {
        RecognitionResult result = new RecognitionResult();
        result.setIntent(intent);
        result.setConfidence(confidence);
        result.setAction(action);
        result.setParameters(parameters);
        return result;
    }
}
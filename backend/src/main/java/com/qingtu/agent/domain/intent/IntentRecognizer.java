package com.qingtu.agent.domain.intent;

import com.qingtu.agent.agent.context.UserContext;
import java.util.List;

public interface IntentRecognizer {
    double MIN_CONFIDENCE = 0.6;

    RecognitionResult recognize(String message, UserContext context);
    List<RecognitionResult> recognizeWithCandidates(String message, UserContext context);
}
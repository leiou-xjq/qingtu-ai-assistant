package com.qingtu.agent.embedding;

public interface EmbeddingModel {
    float[] embed(String text);
    int dimensions();
}
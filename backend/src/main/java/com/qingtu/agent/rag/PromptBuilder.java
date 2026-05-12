package com.qingtu.agent.rag;

public class PromptBuilder {

    private final StringBuilder builder = new StringBuilder();

    public static PromptBuilder create() {
        return new PromptBuilder();
    }

    public PromptBuilder system(String role) {
        builder.append("你是青途智伴AI助手").append(role).append("\n\n");
        return this;
    }

    public PromptBuilder context(String title, String content) {
        if (content != null && !content.isEmpty()) {
            builder.append("【").append(title).append("】\n").append(content).append("\n\n");
        }
        return this;
    }

    public PromptBuilder question(String question) {
        builder.append("【问题】\n").append(question).append("\n");
        return this;
    }

    public PromptBuilder instruction(String... instructions) {
        builder.append("【要求】\n");
        for (String inst : instructions) {
            builder.append("- ").append(inst).append("\n");
        }
        builder.append("\n");
        return this;
    }

    public PromptBuilder append(String text) {
        builder.append(text);
        return this;
    }

    public String build() {
        return builder.toString();
    }
}
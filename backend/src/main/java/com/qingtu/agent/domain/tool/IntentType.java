package com.qingtu.agent.domain.tool;

public enum IntentType {
    WEATHER("天气查询"),
    COURSE("课程管理"),
    COST("消费记账"),
    DIET("饮食推荐"),
    RAG("知识问答"),
    CHAT("闲聊对话"),
    USER("用户信息"),
    OUTFIT("穿搭建议"),
    NOTE("笔记管理"),
    NOTIFICATION("通知查询");

    private final String description;

    IntentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
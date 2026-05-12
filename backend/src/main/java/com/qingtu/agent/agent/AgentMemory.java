package com.qingtu.agent.agent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * AI对话记忆管理
 * 
 * 功能说明：
 * - 维护用户与AI的对话历史
 * - 支持上下文窗口限制
 * - 自动清理过期记忆
 * 
 * @author 青途智伴技术团队
 */
@Slf4j
@Data
public class AgentMemory {

    /**
     * 最大记忆条数
     */
    private static final int MAX_MEMORY_SIZE = 20;

    /**
     * 对话历史
     */
    private final LinkedList<MemoryItem> history = new LinkedList<>();

    /**
     * 保存对话
     */
    public void save(String userMessage, String aiResponse) {
        history.add(new MemoryItem(userMessage, aiResponse));
        
        if (history.size() > MAX_MEMORY_SIZE) {
            history.removeFirst();
        }
    }

    /**
     * 获取完整对话历史
     */
    public List<MemoryItem> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * 获取格式化对话历史
     */
    public String getFormattedHistory() {
        StringBuilder sb = new StringBuilder();
        for (MemoryItem item : history) {
            sb.append("用户: ").append(item.userMessage).append("\n");
            sb.append("AI: ").append(item.aiResponse).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 获取最近N条对话
     */
    public String getRecentHistory(int count) {
        List<MemoryItem> recent = history.stream()
                .skip(Math.max(0, history.size() - count))
                .toList();
        
        StringBuilder sb = new StringBuilder();
        for (MemoryItem item : recent) {
            sb.append("用户: ").append(item.userMessage).append("\n");
            sb.append("AI: ").append(item.aiResponse).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 清空记忆
     */
    public void clear() {
        history.clear();
        log.info("对话记忆已清空");
    }

    /**
     * 记忆项
     */
    public record MemoryItem(String userMessage, String aiResponse, long timestamp) {
        public MemoryItem(String userMessage, String aiResponse) {
            this(userMessage, aiResponse, System.currentTimeMillis());
        }
    }
}
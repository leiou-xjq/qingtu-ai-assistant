package com.qingtu.agent.mcp.server.tools;

import com.qingtu.agent.mcp.server.auth.RequireToolPermission;
import com.qingtu.agent.mcp.server.auth.ToolPermission;
import com.qingtu.agent.service.DocumentParserService;
import com.qingtu.agent.util.BaiduOcrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档工具集
 * 提供文档解析、OCR识别、课表抽取等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocTools {

    private final DocumentParserService documentParserService;
    private final BaiduOcrUtil baiduOcrUtil;

    /**
     * 解析文档
     */
    @RequireToolPermission(ToolPermission.USER)
    public Map<String, Object> parseDocument(String fileUrl, String fileType) {
        long startTime = System.currentTimeMillis();
        try {
            List<String> chunks = documentParserService.parseDocument(fileUrl, fileType);
            String fullText = String.join("\n", chunks);

            return Map.of(
                    "success", true,
                    "text", fullText,
                    "fileType", fileType,
                    "chunksCount", chunks.size(),
                    "charCount", fullText.length(),
                    "executionTimeMs", System.currentTimeMillis() - startTime
            );
        } catch (Exception e) {
            log.error("文档解析失败", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * OCR 图片识别
     */
    @RequireToolPermission(ToolPermission.USER)
    public Map<String, Object> ocrImage(String imageUrl) {
        long startTime = System.currentTimeMillis();
        try {
            // TODO: 下载图片并转为 Base64
            String text = baiduOcrUtil.recognizeText("");
            double confidence = 0.85;

            return Map.of(
                    "success", true,
                    "text", text != null ? text : "",
                    "confidence", confidence,
                    "tableDetected", false,
                    "tableStructure", List.of(),
                    "executionTimeMs", System.currentTimeMillis() - startTime
            );
        } catch (Exception e) {
            log.error("OCR识别失败", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * 提取课表（直接调用 LLM）
     */
    @RequireToolPermission(ToolPermission.USER)
    public Map<String, Object> extractSchedule(String rawText, String schoolName, String semesterStart) {
        long startTime = System.currentTimeMillis();
        // TODO: 调用 LLM 提取课表
        return Map.of(
                "success", true,
                "schedules", List.of(),
                "clarifyingQuestions", List.of(),
                "executionTimeMs", System.currentTimeMillis() - startTime
        );
    }
}

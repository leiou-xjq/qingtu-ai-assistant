package com.qingtu.agent.tool.doc;

import com.qingtu.agent.service.DocumentParserService;
import com.qingtu.agent.tool.ToolDefinition;
import com.qingtu.agent.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * 文档解析工具 - 统一入口
 * 支持 PDF/DOCX/TXT，返回文本内容和表格结构
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseTool implements ToolExecutor {

    private final DocumentParserService documentParserService;

    @Override
    public String getName() {
        return "parse_document";
    }

    @Override
    public String getDescription() {
        return "解析上传的文档（PDF/Word/Excel/TXT），提取文本内容和表格结构，返回统一的解析结果";
    }

    @Override
    public String getCategory() {
        return "doc";
    }

    @Override
    public ToolDefinition.ExecuteResult execute(Map<String, Object> arguments) {
        long startTime = System.currentTimeMillis();
        try {
            String fileUrl = (String) arguments.get("file_url");
            String fileType = (String) arguments.getOrDefault("file_type", "pdf");
            Map<String, Object> parseOptions = (Map<String, Object>) arguments.getOrDefault("parse_options", new HashMap<>());

            if (fileUrl == null || fileUrl.isBlank()) {
                return ToolDefinition.ExecuteResult.error("file_url 参数不能为空");
            }

            // 解析文档（实际场景需要先下载文件）
            List<String> chunks = documentParserService.parseDocument(fileUrl, fileType);
            String fullText = String.join("\n", chunks);

            // TODO: 表格结构提取（需要增强 DocumentParserService）
            List<Map<String, Object>> tables = new ArrayList<>();

            Map<String, Object> result = new HashMap<>();
            result.put("text", fullText);
            result.put("tables", tables);
            result.put("file_type", fileType);
            result.put("chunks_count", chunks.size());
            result.put("char_count", fullText.length());

            log.info("文档解析完成: file_url={}, chars={}", fileUrl, fullText.length());

            return ToolDefinition.ExecuteResult.success(result);

        } catch (Exception e) {
            log.error("文档解析失败", e);
            return ToolDefinition.ExecuteResult.error("文档解析失败: " + e.getMessage());
        }
    }

    @Override
    public long getTimeoutMs() {
        return 60000;
    }
}

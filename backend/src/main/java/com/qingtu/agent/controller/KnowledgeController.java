package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.rag.ElasticsearchRagStore;
import com.qingtu.agent.service.DocumentParserService;
import com.qingtu.agent.service.PublicKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final PublicKnowledgeService publicKnowledgeService;
    private final DocumentParserService documentParserService;
    private final ElasticsearchRagStore esRagStore;

    @PostMapping("/import/wikipedia")
    public CommonResult<?> importWikipedia(@RequestParam String keyword) {
        int count = publicKnowledgeService.importWikipedia(keyword);
        return CommonResult.success(Map.of("imported", count));
    }

    @PostMapping("/import/wikipedia/batch")
    public CommonResult<?> importWikipediaBatch(@RequestBody List<String> keywords) {
        int count = publicKnowledgeService.importWikipediaBatch(keywords);
        return CommonResult.success(Map.of("imported", count));
    }

    @PostMapping("/import/hot")
    public CommonResult<?> importHotSearch() {
        int count = publicKnowledgeService.importHotSearch();
        return CommonResult.success(Map.of("imported", count));
    }

    @PostMapping("/import/daily")
    public CommonResult<?> importDailyKnowledge() {
        int count = publicKnowledgeService.importDailyKnowledge();
        return CommonResult.success(Map.of("imported", count));
    }

    @PostMapping("/import/campus")
    public CommonResult<?> importCampusKnowledge() {
        int count = publicKnowledgeService.importCampusKnowledge();
        return CommonResult.success(Map.of("imported", count));
    }

    @PostMapping("/import/all")
    public CommonResult<?> importAllKnowledge() {
        int[] counts = new int[4];

        // 导入日常知识
        counts[0] = publicKnowledgeService.importDailyKnowledge();

        // 导入校园知识
        counts[1] = publicKnowledgeService.importCampusKnowledge();

        // 导入热点资讯
        counts[2] = publicKnowledgeService.importHotSearch();

        // 默认关键词导入
        List<String> defaultKeywords = List.of(
            "大学", "专业", "课程", "考试", "学习", "图书馆",
            "食堂", "宿舍", "社团", "就业", "实习", "考研",
            "健康", "运动", "饮食", "天气", "生活", "科技"
        );
        counts[3] = publicKnowledgeService.importWikipediaBatch(defaultKeywords);

        int total = counts[0] + counts[1] + counts[2] + counts[3];
        return CommonResult.success(Map.of(
            "total", total,
            "daily", counts[0],
            "campus", counts[1],
            "hot", counts[2],
            "wikipedia", counts[3]
        ));
    }

    @GetMapping("/stats")
    public CommonResult<?> getStats() {
        return CommonResult.success(publicKnowledgeService.getKnowledgeStats());
    }

    @PostMapping("/upload")
    public CommonResult<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "document") String category,
            @RequestParam(value = "title", required = false) String title) {

        if (file.isEmpty()) {
            return CommonResult.fail("文件不能为空");
        }

        try {
            List<String> chunks = documentParserService.parseDocument(file);

            if (title == null || title.isEmpty()) {
                title = documentParserService.extractTitle(chunks.isEmpty() ? "" : chunks.get(0));
            }

            int imported = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunkTitle = chunks.size() > 1 ? title + " (第" + (i + 1) + "部分)" : title;
                esRagStore.addDocument(category, chunkTitle, chunks.get(i));
                imported++;
            }

            log.info("文档导入成功: {}, {}个知识块", title, imported);
            return CommonResult.success(Map.of(
                "title", title,
                "chunks", imported,
                "category", category
            ));

        } catch (Exception e) {
            log.error("文档解析失败: {}", e.getMessage());
            return CommonResult.fail("文档解析失败: " + e.getMessage());
        }
    }
}
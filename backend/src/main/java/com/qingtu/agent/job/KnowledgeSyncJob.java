package com.qingtu.agent.job;

import com.qingtu.agent.service.PublicKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSyncJob {

    private final PublicKnowledgeService publicKnowledgeService;

    @Scheduled(cron = "0 30 6 * * ?")
    public void dailyKnowledgeSync() {
        log.info("========== 每日知识同步任务开始 ==========");

        try {
            // 1. 导入日常知识
            int dailyCount = publicKnowledgeService.importDailyKnowledge();
            log.info("日常知识导入: {}条", dailyCount);

            // 2. 导入校园知识
            int campusCount = publicKnowledgeService.importCampusKnowledge();
            log.info("校园知识导入: {}条", campusCount);

            // 3. 导入热点资讯
            int hotCount = publicKnowledgeService.importHotSearch();
            log.info("热点资讯导入: {}条", hotCount);

            // 4. 维基百科关键词导入
            List<String> keywords = List.of(
                "大学生活", "学习方法", "考试技巧", "校园活动",
                "健康饮食", "运动锻炼", "时间管理", "职业规划",
                "科技创新", "人工智能", "环境保护", "社会实践"
            );
            int wikiCount = publicKnowledgeService.importWikipediaBatch(keywords);
            log.info("维基百科导入: {}条", wikiCount);

            log.info("========== 每日知识同步完成 ==========");

        } catch (Exception e) {
            log.error("每日知识同步任务异常: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyHotSearchSync() {
        try {
            int count = publicKnowledgeService.importHotSearch();
            if (count > 0) {
                log.info("整点热点资讯同步: {}条", count);
            }
        } catch (Exception e) {
            log.warn("整点热点同步失败: {}", e.getMessage());
        }
    }
}
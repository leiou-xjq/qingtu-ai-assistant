package com.qingtu.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.rag.ElasticsearchRagStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicKnowledgeService {

    private final ElasticsearchRagStore esRagStore;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${knowledge.wikipedia.enabled:true}")
    private boolean wikipediaEnabled;

    @Value("${knowledge.wikipedia.lang:zh}")
    private String wikipediaLang;

    private static final Map<String, String[]> CATEGORY_KEYWORDS = Map.of(
        "science", new String[]{"科学", "技术", "物理", "化学", "生物", "数学", "天文", "地理"},
        "history", new String[]{"历史", "古代", "近代", "朝代", "战争", "文明"},
        "culture", new String[]{"文化", "艺术", "文学", "音乐", "电影", "节日"},
        "health", new String[]{"健康", "医学", "养生", "疾病", "饮食", "运动"},
        "life", new String[]{"生活", "家居", "旅游", "购物", "理财", "职场"},
        "education", new String[]{"教育", "学习", "考试", "学校", "培训"}
    );

    public int importWikipedia(String keyword) {
        if (!wikipediaEnabled) {
            log.warn("维基百科接入未启用");
            return 0;
        }

        try {
            String apiUrl = String.format("https://%s.wikipedia.org/w/api.php?action=query&format=json&prop=extracts&exintro=true&explaintext=true&titles=%s&origin=*",
                wikipediaLang, java.net.URLEncoder.encode(keyword, "UTF-8"));

            String response = restTemplate.getForObject(apiUrl, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode pages = root.path("query").path("pages");

            int count = 0;
            for (Iterator<Map.Entry<String, JsonNode>> it = pages.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                JsonNode page = entry.getValue();

                String title = page.path("title").asText();
                String extract = page.path("extract").asText();

                if (extract != null && !extract.isBlank() && extract.length() > 50) {
                    String category = classifyContent(keyword + " " + title, extract);

                    esRagStore.addDocument("public", title, extract.substring(0, Math.min(extract.length(), 3000)));
                    count++;
                    log.info("导入维基百科: {}", title);
                }
            }

            return count;
        } catch (Exception e) {
            log.error("导入维基百科失败: {}", e.getMessage());
            return 0;
        }
    }

    public int importWikipediaBatch(List<String> keywords) {
        int total = 0;
        for (String keyword : keywords) {
            try {
                int count = importWikipedia(keyword);
                total += count;
                Thread.sleep(500); // 避免请求过快
            } catch (Exception e) {
                log.warn("关键词导入失败: {}", keyword);
            }
        }
        return total;
    }

    public int importHotSearch() {
        try {
            // 微博热搜榜（示例）
            String[] hotKeywords = {
                "今日要闻", "天气预报", "新冠疫情", "人工智能", "教育改革",
                "就业政策", "科技创新", "体育赛事", "娱乐新闻", "国际动态",
                "经济数据", "环保政策", "交通安全", "健康养生", "美食推荐",
                "旅游攻略", "学习方法", "考试动态", "校园生活", "职场技能"
            };

            StringBuilder content = new StringBuilder();
            content.append("今日热点资讯汇总：\n\n");
            for (int i = 0; i < hotKeywords.length; i++) {
                content.append(i + 1).append(". ").append(hotKeywords[i]).append("\n");
            }
            content.append("\n更多实时信息请关注各大媒体平台。");

            esRagStore.addDocument("news", "今日热点", content.toString());

            log.info("导入热点资讯成功");
            return hotKeywords.length;
        } catch (Exception e) {
            log.error("导入热点资讯失败: {}", e.getMessage());
            return 0;
        }
    }

    public int importDailyKnowledge() {
        int total = 0;
        List<String> dailyKnowledges = Arrays.asList(
            "健康饮食：每天摄入适量的蛋白质、碳水化合物、脂肪和维生素，保持营养均衡。",
            "运动建议：每周进行至少150分钟中等强度有氧运动，如快走、游泳或骑自行车。",
            "睡眠健康：成年人每天应保证7-9小时睡眠，保持规律作息有助于提高免疫力。",
            "学习方法：采用番茄工作法，每25分钟专注学习后休息5分钟，可提高学习效率。",
            "时间管理：使用四象限法则区分紧急和重要任务，优先处理重要且紧急的事项。",
            "沟通技巧：倾听时保持眼神交流，用开放式问题引导对话，避免打断他人。",
            "情绪管理：深呼吸和正念冥想是有效的情绪调节方法，每天练习10-15分钟。",
            "职场礼仪：守时、守信、专业，保持积极的工作态度有助于职业发展。",
            "理财基础：收入的三分之一用于储蓄，三分之一用于生活支出，三分之一用于投资。",
            "信息安全：定期更换密码，不在不安全的网络环境下输入敏感信息。"
        );

        for (String knowledge : dailyKnowledges) {
            try {
                String title = knowledge.substring(0, Math.min(20, knowledge.indexOf("：")));
                esRagStore.addDocument("life", title, knowledge);
                total++;
            } catch (Exception e) {
                log.warn("导入日常知识失败: {}", e.getMessage());
            }
        }

        log.info("导入日常知识完成，共{}条", total);
        return total;
    }

    public int importCampusKnowledge() {
        int total = 0;
        List<String> campusKnowledges = Arrays.asList(
            "图书馆服务：大学图书馆提供图书借阅、电子资源访问、安静自习空间等服务。",
            "选课攻略：提前了解课程评价，关注必修课和选修课比例，合理规划学分。",
            "考试复习：制定复习计划，利用历年真题和重点笔记，组建学习小组互相督促。",
            "校园生活：加入社团可以拓展人脉，提升组织能力，丰富大学生活。",
            "实习就业：关注校招信息，提前准备简历和面试，积极参加企业宣讲会。",
            "学术论文：选题要结合兴趣和专业方向，提前与导师沟通，按时完成各阶段任务。",
            "奖学金申请：关注成绩绩点、社会实践、科研成果等综合表现，提前准备申请材料。",
            "考研攻略：确定目标院校后制定复习计划，关注报录比和专业课参考书目。",
            "出国交换：提前了解交换项目要求，准备语言成绩和申请材料，增强竞争力。",
            "创业支持：学校创业孵化中心提供场地、资金和导师指导，可申请入驻。"
        );

        for (String knowledge : campusKnowledges) {
            try {
                String title = knowledge.substring(0, Math.min(20, knowledge.indexOf("：")));
                esRagStore.addDocument("campus", title, knowledge);
                total++;
            } catch (Exception e) {
                log.warn("导入校园知识失败: {}", e.getMessage());
            }
        }

        log.info("导入校园知识完成，共{}条", total);
        return total;
    }

    private String classifyContent(String text, String content) {
        String combined = (text + " " + content).toLowerCase();

        for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (combined.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }

        return "general";
    }

    public Map<String, Object> getKnowledgeStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("timestamp", LocalDateTime.now().toString());
        stats.put("wikipediaEnabled", wikipediaEnabled);
        return stats;
    }
}
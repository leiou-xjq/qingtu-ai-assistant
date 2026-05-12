package com.qingtu.agent.service;

import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.rag.ElasticsearchRagStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 学校数据自动初始化服务
 * 用户注册后异步检查ES，无该校数据则通过Qwen搜索并入库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolDataInitService {

    private final ElasticsearchRagStore esRagStore;
    private final QingTuAgent qingTuAgent;

    /** 搜索主题列表 */
    private static final String[] SEARCH_TOPICS = {
        "校园简介与历史沿革",
        "校历与学期安排",
        "宿舍条件与食堂餐饮介绍",
        "图书馆开放时间与借阅规则",
        "院系专业与课程设置",
        "校园交通与周边环境"
    };

    @Async("taskExecutor")
    public void initSchoolData(String school) {
        try {
            // 1. 去重检查
            long existingCount = esRagStore.countBySchool(school);
            if (existingCount > 0) {
                log.info("学校数据已存在，跳过初始化: school={}, count={}", school, existingCount);
                return;
            }

            log.info("开始初始化学校数据: school={}", school);

            // 2. 逐主题搜索并入库
            for (String topic : SEARCH_TOPICS) {
                try {
                    String query = school + " " + topic;
                    String prompt = "请用简洁的中文介绍" + school + "的" + topic + "，控制在200字以内。如果无法获取准确信息，请如实说明。";
                    String answer = qingTuAgent.chat(prompt);

                    if (answer != null && !answer.isBlank() && !answer.contains("无法获取")) {
                        esRagStore.addDocument("campus", topic, answer, school, null, "auto_init", null);
                        log.info("学校数据入库成功: school={}, topic={}", school, topic);
                    }
                } catch (Exception e) {
                    log.warn("学校数据搜索失败: school={}, topic={}, error={}", school, topic, e.getMessage());
                }
            }

            log.info("学校数据初始化完成: school={}", school);

        } catch (Exception e) {
            log.error("学校数据初始化失败: school={}, error={}", school, e.getMessage());
        }
    }
}
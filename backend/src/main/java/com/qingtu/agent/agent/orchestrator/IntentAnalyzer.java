package com.qingtu.agent.agent.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.TaskMessage;
import com.qingtu.agent.config.DashScopeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 意图分析器
 * 调用 LLM 分析用户消息，识别意图并提取参数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentAnalyzer {

    private final DashScopeConfig dashScopeConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INTENT_WEATHER = "weather";
    private static final String INTENT_EXPENSE = "expense";
    private static final String INTENT_COURSE = "course";
    private static final String INTENT_PROFILE = "profile";
    private static final String INTENT_NOTE = "note";
    private static final String INTENT_CALORIE = "calorie";
    private static final String INTENT_CHAT = "chat";
    private static final String INTENT_SEARCH = "search";

    public List<Task> analyze(String message, UserContext context, Map<String, String> files) {
        try {
            String prompt = buildIntentPrompt(message, context, files);
            String llmResponse = callLLM(prompt);

            return parseIntentResponse(llmResponse, context);
        } catch (Exception e) {
            log.error("意图分析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildIntentPrompt(String message, UserContext context, Map<String, String> files) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是青途AI助手的意图分析器。分析用户消息，识别用户想要执行的任务。\n\n");

        prompt.append("【可用Agent类型】\n");
        prompt.append("- weather: 天气查询（如'今天天气怎么样'、'帮我查下天气'）\n");
        prompt.append("- expense: 记账创建（如'记账花了25'、'帮我记一下消费'）\n");
        prompt.append("- course: 课程导入（如'帮我记录课程'、'上传了课表'）\n");
        prompt.append("- profile: 个人信息修改（如'帮我修改身高体重'、'改下我的资料'）\n");
        prompt.append("- note: 笔记生成（如'帮我生成笔记'、'今天学了什么'）\n");
        prompt.append("- calorie: 卡路里记录（如'中午吃了牛肉面'、'记录午餐摄入'）\n");
        prompt.append("- search: 联网搜索（如'搜索学校新闻'、'查询最新消息'）\n");
        prompt.append("- chat: 普通对话（问候、闲聊、知识问答等，以上都不匹配时）\n\n");

        prompt.append("【用户上下文】\n");
        prompt.append("- 用户ID: ").append(context.getUserId()).append("\n");
        prompt.append("- 城市: ").append(context.getCity() != null ? context.getCity() : "未知").append("\n");
        prompt.append("- 学校: ").append(context.getSchool() != null ? context.getSchool() : "未知").append("\n\n");

        prompt.append("【文件信息】\n");
        if (files != null && !files.isEmpty()) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                prompt.append("- 文件名: ").append(entry.getKey());
                prompt.append(", 类型: ").append(getFileType(entry.getKey())).append("\n");
            }
        } else {
            prompt.append("- 无文件上传\n");
        }
        prompt.append("\n");

        prompt.append("【用户消息】\n").append(message).append("\n\n");

        prompt.append("【输出要求】\n");
        prompt.append("请输出JSON格式（不要输出任何其他内容）：\n");
        prompt.append("{\n");
        prompt.append("  \"tasks\": [\n");
        prompt.append("    {\"agent\": \"agent类型\", \"action\": \"具体动作\", \"params\": {...}, \"description\": \"任务描述\"}\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("【示例】\n");
        prompt.append("用户: '帮我记账，花了25元' → {\"agent\": \"expense\", \"action\": \"create\", \"params\": {\"amount\": 25, \"category\": \"饮食\"}, \"description\": \"记账25元\"}\n");
        prompt.append("用户: '今天天气怎么样' → {\"agent\": \"weather\", \"action\": \"query\", \"params\": {\"city\": \"汉中\"}, \"description\": \"查询天气\"}\n");
        prompt.append("用户: '帮我记录课程' + 上传图片 → {\"agent\": \"course\", \"action\": \"import\", \"params\": {\"fileName\": \"课表.jpg\", \"fileType\": \"image\"}, \"description\": \"导入课程\"}\n");
        prompt.append("用户: '你好啊' → {\"agent\": \"chat\", \"action\": \"chat\", \"params\": {}, \"description\": \"普通对话\"}\n\n");

prompt.append("【金额提取规则】\n");
        prompt.append("- 数字直接提取：'13元' → amount:13, '25块' → amount:25, '50元' → amount:50\n");
        prompt.append("- 中文数字转换：'一'→1, '五'→5, '十'→10, '十三'→13, '二十五'→25\n");
        prompt.append("- 上下文推断类别：从关键词推断，如'吃饭'→'饮食', '坐车'→'交通', '买书'→'购物'\n\n");

        prompt.append("【身体数据提取规则】\n");
        prompt.append("- 体重提取：'体重71' → weight:71, '71kg' → weight:71, '体重71公斤' → weight:71\n");
        prompt.append("- 身高提取：'身高175' → height:175, '175cm' → height:175, '身高175厘米' → height:175\n");
        prompt.append("- 只保留数字部分，单位自动忽略\n\n");

        prompt.append("【卡路里记录示例】\n");
        prompt.append("用户: '中午吃了牛肉面' → {\"agent\": \"calorie\", \"action\": \"create\", \"params\": {\"food\": \"牛肉面\", \"mealType\": \"lunch\"}, \"description\": \"记录午餐\"}\n");
        prompt.append("用户: '记录一下晚餐，吃了炒饭' → {\"agent\": \"calorie\", \"action\": \"create\", \"params\": {\"food\": \"炒饭\", \"mealType\": \"dinner\"}, \"description\": \"记录晚餐\"}\n");
        prompt.append("用户: '早上吃了一个鸡蛋' → {\"agent\": \"calorie\", \"action\": \"create\", \"params\": {\"food\": \"鸡蛋\", \"mealType\": \"breakfast\"}, \"description\": \"记录早餐\"}\n");
        prompt.append("用户: '吃了油泼面' → {\"agent\": \"calorie\", \"action\": \"create\", \"params\": {\"food\": \"油泼面\"}, \"description\": \"记录饮食\"}\n");
        prompt.append("用户: '午餐吃了米饭和炒菜' → {\"agent\": \"calorie\", \"action\": \"create\", \"params\": {\"food\": \"米饭和炒菜\", \"mealType\": \"lunch\"}, \"description\": \"记录午餐\"}\n\n");

        prompt.append("【笔记生成示例】\n");
        prompt.append("用户: '帮我生成今日课程的笔记' → {\"agent\": \"note\", \"action\": \"generate\", \"params\": {\"type\": \"note\", \"includeTodayCourses\": true}, \"description\": \"生成今日课程笔记\"}\n");
        prompt.append("用户: '生成今日课程笔记' → {\"agent\": \"note\", \"action\": \"generate\", \"params\": {\"type\": \"note\", \"includeTodayCourses\": true}, \"description\": \"生成今日课程笔记\"}\n");
        prompt.append("用户: '生成学习笔记' → {\"agent\": \"note\", \"action\": \"generate\", \"params\": {\"type\": \"note\"}, \"description\": \"生成学习笔记\"}\n");
        prompt.append("用户: '帮我生成笔记' → {\"agent\": \"note\", \"action\": \"generate\", \"params\": {\"type\": \"note\"}, \"description\": \"生成笔记\"}\n\n");

        prompt.append("【资料修改示例】\n");
        prompt.append("用户: '修改我的体重为71' → {\"agent\": \"profile\", \"action\": \"update\", \"params\": {\"weight\": 71}, \"description\": \"修改体重\"}\n");
        prompt.append("用户: '体重71' → {\"agent\": \"profile\", \"action\": \"update\", \"params\": {\"weight\": 71}, \"description\": \"修改体重\"}\n");
        prompt.append("用户: '修改身高175' → {\"agent\": \"profile\", \"action\": \"update\", \"params\": {\"height\": 175}, \"description\": \"修改身高\"}\n");
        prompt.append("用户: '修改身高175，体重70' → {\"agent\": \"profile\", \"action\": \"update\", \"params\": {\"height\": 175, \"weight\": 70}, \"description\": \"修改身高体重\"}\n");
        prompt.append("用户: '帮我修改身高体重' → {\"agent\": \"profile\", \"action\": \"update\", \"params\": {}, \"description\": \"修改资料\"}\n\n");

        prompt.append("【更多示例】\n");
        prompt.append("用户: '帮我记账，花了25元' → {\"agent\": \"expense\", \"action\": \"create\", \"params\": {\"amount\": 25, \"category\": \"饮食\"}, \"description\": \"记账25元\"}\n");
        prompt.append("用户: '今天天气怎么样' → {\"agent\": \"weather\", \"action\": \"query\", \"params\": {\"city\": \"汉中\"}, \"description\": \"查询天气\"}\n");
        prompt.append("用户: '明天天气怎么样' → {\"agent\": \"weather\", \"action\": \"forecast\", \"params\": {\"dayOffset\": 1}, \"description\": \"明天天气预报\"}\n");
        prompt.append("用户: '后天要下雨吗' → {\"agent\": \"weather\", \"action\": \"forecast\", \"params\": {\"dayOffset\": 2}, \"description\": \"后天天气预报\"}\n");
        prompt.append("用户: '这周天气如何' → {\"agent\": \"weather\", \"action\": \"forecast\", \"params\": {\"dayOffset\": 7}, \"description\": \"一周天气预报\"}\n");
        prompt.append("用户: '帮我记录课程' + 上传图片 → {\"agent\": \"course\", \"action\": \"import\", \"params\": {\"fileName\": \"课表.jpg\", \"fileType\": \"image\"}, \"description\": \"导入课程\"}\n");
        prompt.append("用户: '你好啊' → {\"agent\": \"chat\", \"action\": \"chat\", \"params\": {}, \"description\": \"普通对话\"}\n");
        prompt.append("用户: '搜索学校新闻' → {\"agent\": \"search\", \"action\": \"search\", \"params\": {\"query\": \"学校新闻\"}, \"description\": \"联网搜索\"}\n");
        prompt.append("用户: '帮我查下最近有什么通知' → {\"agent\": \"search\", \"action\": \"search\", \"params\": {\"query\": \"学校通知公告\"}, \"description\": \"联网搜索通知\"}\n");

        return prompt.toString();
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif")) {
            return "image";
        } else if (lower.endsWith(".pdf")) {
            return "pdf";
        } else if (lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv")) {
            return "excel";
        } else if (lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt")) {
            return "document";
        }
        return "unknown";
    }

    private String callLLM(String prompt) {
        String url = dashScopeConfig.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", dashScopeConfig.getModel());
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("enable_search", false);
        body.put("temperature", 0.3);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + dashScopeConfig.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String response = restTemplate.postForObject(url, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
        } catch (Exception e) {
            log.error("解析LLM响应失败", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Task> parseIntentResponse(String llmResponse, UserContext context) {
        List<Task> tasks = new ArrayList<>();

        if (llmResponse == null || llmResponse.isBlank()) {
            log.warn("LLM返回为空，使用默认chat任务");
            tasks.add(Task.of(INTENT_CHAT, "chat", Map.of("message", "")));
            return tasks;
        }

        try {
            int startIdx = llmResponse.indexOf('{');
            int endIdx = llmResponse.lastIndexOf('}');
            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonStr = llmResponse.substring(startIdx, endIdx + 1);
                Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);

                List<Map<String, Object>> taskList = (List<Map<String, Object>>) parsed.getOrDefault("tasks", new ArrayList<>());
                for (Map<String, Object> taskMap : taskList) {
                    String agent = (String) taskMap.getOrDefault("agent", INTENT_CHAT);
                    String action = (String) taskMap.getOrDefault("action", "chat");
                    Map<String, Object> params = (Map<String, Object>) taskMap.getOrDefault("params", new HashMap<>());
                    String description = (String) taskMap.getOrDefault("description", "");

                    Task task = Task.of(agent, action, params, description);

                    if (context != null) {
                        params.put("_userId", context.getUserId());
                        params.put("_city", context.getCity());
                        params.put("_school", context.getSchool());
                        params.put("_semesterStart", context.getSemesterStart());
                    }

                    tasks.add(task);
                }
            }
        } catch (Exception e) {
            log.error("解析意图响应失败: {}", e.getMessage());
            tasks.add(Task.of(INTENT_CHAT, "chat", Map.of("message", llmResponse)));
        }

        if (tasks.isEmpty()) {
            tasks.add(Task.of(INTENT_CHAT, "chat", Map.of("message", llmResponse)));
        }

        return tasks;
    }
}
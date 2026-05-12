package com.qingtu.agent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RagServiceCore {

    private final ElasticsearchRagStore esRagStore;

    @Value("${ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    public record RetrieveOption(
        String school,
        boolean searchSchool,
        boolean searchCommon,
        int topK,
        int maxLength
    ) {
        public static RetrieveOption commonOnly(int topK) {
            return new RetrieveOption(null, false, true, topK, 3000);
        }

        public static RetrieveOption schoolOnly(String school, int topK) {
            return new RetrieveOption(school, true, false, topK, 3000);
        }

        public static RetrieveOption allSources(String school, int topK) {
            return new RetrieveOption(school, true, true, topK, 3000);
        }
    }

    public RagServiceCore(@Autowired(required = false) ElasticsearchRagStore esRagStore) {
        this.esRagStore = esRagStore;
    }

    public String retrieve(String query, RetrieveOption option) {
        StringBuilder results = new StringBuilder();

        if (option.searchSchool() && option.school() != null && !option.school().isBlank()) {
            String schoolResult = retrieveSchoolContext(option.school(), query, option.topK());
            if (!schoolResult.isBlank()) {
                results.append(schoolResult);
            }
        }

        if (option.searchCommon()) {
            String commonResult = retrieveCommonContext(query, option.topK());
            if (!commonResult.isBlank()) {
                if (results.length() > 0) {
                    results.append("\n\n【通用知识】\n");
                }
                results.append(commonResult);
            }
        }

        if (results.length() > option.maxLength()) {
            return results.substring(0, option.maxLength());
        }
        return results.toString();
    }

    public String retrieveCommonContext(String query, int topK) {
        log.debug("检索通用知识库: query={}, topK={}", query, topK);

        if (esRagStore != null) {
            try {
                List<Map<String, Object>> results = esRagStore.searchCommon(query, topK);
                if (results != null && !results.isEmpty()) {
                    log.debug("通用知识库检索到{}条数据", results.size());
                    return formatEsResults(results);
                }
            } catch (Exception e) {
                log.warn("通用知识库检索失败: {}", e.getMessage());
            }
        }

        return "";
    }

    public String retrieveSchoolContext(String school, String query, int topK) {
        log.debug("检索学校知识库: school={}, query={}, topK={}", school, query, topK);

        if (school == null || school.isBlank()) {
            log.warn("学校名称为空，无法检索学校知识库");
            return "";
        }

        if (esRagStore != null) {
            try {
                List<Map<String, Object>> results = esRagStore.searchBySchoolAndKeyword(school, query, topK);
                if (results != null && !results.isEmpty()) {
                    log.debug("学校知识库检索到{}条数据", results.size());
                    return formatEsResults(results);
                }
            } catch (Exception e) {
                log.warn("学校知识库检索失败: {}", e.getMessage());
            }
        }

        return "";
    }

    private String formatEsResults(List<Map<String, Object>> results) {
        StringBuilder sb = new StringBuilder();
        int maxContentLen = 800;
        int totalLimit = 3000;

        for (Map<String, Object> r : results) {
            if (sb.length() >= totalLimit) break;

            String title = (String) r.getOrDefault("question", "");
            String answer = (String) r.getOrDefault("answer", "");

            if (answer.isEmpty()) continue;

            answer = cleanHtml(answer);

            if (answer.length() > maxContentLen) {
                answer = answer.substring(0, maxContentLen) + "...";
            }

            if (sb.length() + title.length() + answer.length() > totalLimit) {
                break;
            }

            sb.append("【").append(title).append("】\n")
              .append(answer).append("\n\n");
        }
        return sb.toString();
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        String text = html;

        text = text.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "");
        text = text.replaceAll("<style[^>]*>[\\s\\S]*?</style>", "");
        text = text.replaceAll("<[^>]+>", " ");

        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&#\\d+;", " ");

        text = text.replaceAll("\\|\\s*设为", " ");
        text = text.replaceAll("\\|", " ");

        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    public void addDocument(String content, String query) {
        if (content == null || content.isEmpty()) {
            log.warn("添加文档内容为空");
            return;
        }

        if (esRagStore != null) {
            try {
                esRagStore.addDocument("web_search", query, content);
                log.info("文档已添加到ES: query={}, contentLength={}", query, content.length());
            } catch (Exception e) {
                log.error("添加文档到ES失败: {}", e.getMessage());
            }
        }
    }
}
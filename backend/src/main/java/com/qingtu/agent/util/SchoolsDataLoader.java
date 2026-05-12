package com.qingtu.agent.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SchoolsDataLoader {

    private List<Map<String, String>> localSchools = new ArrayList<>();

    @PostConstruct
    public void loadSchools() {
        try {
            ClassPathResource resource = new ClassPathResource("schools.json");
            InputStream inputStream = resource.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String content = reader.lines().collect(Collectors.joining());
            reader.close();

            JSONArray jsonArray = JSON.parseArray(content);
            for (int i = 0; i < jsonArray.size(); i++) {
                var item = jsonArray.getJSONObject(i);
                Map<String, String> school = new HashMap<>();
                school.put("name", item.getString("name"));
                school.put("province", item.getString("province"));
                school.put("city", item.getString("city"));
                localSchools.add(school);
            }

            log.info("本地学校数据加载成功，共 {} 所学校", localSchools.size());
        } catch (Exception e) {
            log.error("加载本地学校数据失败: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> searchSchools(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        String lowerKeyword = keyword.toLowerCase().trim();
        Set<String> addedNames = new HashSet<>();

        for (Map<String, String> school : localSchools) {
            String name = school.get("name");
            if (name == null) continue;

            if (name.toLowerCase().contains(lowerKeyword)) {
                if (!addedNames.contains(name)) {
                    addedNames.add(name);
                    results.add(createSchoolResult(name, school));
                }
                continue;
            }

            String shortName = getShortName(name);
            if (shortName != null && shortName.toLowerCase().contains(lowerKeyword)) {
                if (!addedNames.contains(name)) {
                    addedNames.add(name);
                    results.add(createSchoolResult(name, school));
                }
                continue;
            }

            String province = school.get("province");
            String city = school.get("city");
            if ((province != null && province.toLowerCase().contains(lowerKeyword)) ||
                (city != null && city.toLowerCase().contains(lowerKeyword))) {
                if (!addedNames.contains(name)) {
                    addedNames.add(name);
                    results.add(createSchoolResult(name, school));
                }
            }
        }

        log.info("本地搜索'{}'，找到 {} 所学校", keyword, results.size());
        return results;
    }

    private Map<String, Object> createSchoolResult(String name, Map<String, String> school) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("province", school.get("province"));
        result.put("city", school.get("city"));
        String location = school.get("city") != null ? school.get("city") : school.get("province");
        result.put("location", location != null ? location : "");
        result.put("source", "local");
        return result;
    }

    private String getShortName(String name) {
        if (name == null) return null;

        String shortName = name
                .replace("大学", "")
                .replace("学院", "")
                .replace("学校", "")
                .replace("市", "")
                .replace("省", "");

        if (shortName.length() < 2) {
            return null;
        }

        return shortName;
    }

    public int getTotalCount() {
        return localSchools.size();
    }
}
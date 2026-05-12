package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.util.SchoolsDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/school")
@RequiredArgsConstructor
@Slf4j
public class SchoolController {

    private final SchoolsDataLoader schoolsDataLoader;

    @GetMapping("/search")
    public CommonResult<?> search(@RequestParam String q) {
        if (q == null || q.trim().length() < 1) {
            return CommonResult.success(new java.util.ArrayList<>());
        }

        try {
            String keyword = q.trim();
            log.info("学校搜索关键词: {}", keyword);

            // 直接使用本地数据搜索
            var schools = schoolsDataLoader.searchSchools(keyword);
            log.info("本地数据搜索结果: {} 所学校", schools.size());

            return CommonResult.success(schools);
        } catch (Exception e) {
            log.error("学校搜索失败: {}", e.getMessage());
            return CommonResult.success(new java.util.ArrayList<>());
        }
    }
}
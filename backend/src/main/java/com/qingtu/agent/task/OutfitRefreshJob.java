package com.qingtu.agent.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.service.OutfitCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 穿搭缓存定时刷新任务
 * 每天6:00-22:00每2小时执行一次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutfitRefreshJob implements Job {

    private final UserMapper userMapper;
    private final OutfitCacheService outfitCacheService;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("【定时任务】开始刷新各城市穿搭缓存");

        try {
            Set<String> cities = getUniqueCities();
            if (cities.isEmpty()) {
                cities.add("北京");
            }

            log.info("需要更新的城市数量: {}", cities.size());

            for (String city : cities) {
                try {
                    outfitCacheService.refreshOutfitForCity(city);
                } catch (Exception e) {
                    log.error("城市{}穿搭刷新失败: {}", city, e.getMessage());
                }
            }

            log.info("【定时任务】各城市穿搭缓存刷新完成");
        } catch (Exception e) {
            log.error("【定时任务】穿搭刷新任务失败", e);
        }
    }

    private Set<String> getUniqueCities() {
        Set<String> cities = new HashSet<>();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(User::getCity);
        wrapper.ne(User::getCity, "");
        wrapper.eq(User::getDeleted, 0);
        wrapper.select(User::getCity);

        List<User> users = userMapper.selectList(wrapper);
        for (User user : users) {
            if (user.getCity() != null && !user.getCity().isEmpty()) {
                cities.add(user.getCity().trim());
            }
        }

        return cities;
    }
}

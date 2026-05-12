package com.qingtu.agent.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.entity.po.UserHealth;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.mapper.UserHealthMapper;
import com.qingtu.agent.util.RedisCacheUtil;
import com.qingtu.agent.util.WeatherUtil;
import com.qingtu.agent.agent.QingTuAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日缓存刷新任务
 * 每天凌晨04:00执行，刷新穿搭和食谱缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheRefreshJob implements Job {

    private final UserMapper userMapper;
    private final UserHealthMapper userHealthMapper;
    private final WeatherUtil weatherUtil;
    private final QingTuAgent agent;
    private final RedisCacheUtil redisCacheUtil;

    private static final String DEFAULT_OUTFIT = "上衣: T恤或薄衬衫\n下装: 牛仔裤或休闲长裤\n鞋子: 运动鞋\n配件: 双肩包\n带伞: 可携带\n提示: 今日气温适中，建议分层穿搭，早晚可添加薄外套";

    @Override
    public void execute(JobExecutionContext context) {
        log.info("【定时任务】开始刷新每日缓存");

        try {
            refreshOutfitCache();
            refreshRecipeCache();
            log.info("【定时任务】每日缓存刷新完成");
        } catch (Exception e) {
            log.error("【定时任务】缓存刷新失败", e);
        }
    }

    private void refreshOutfitCache() {
        log.info("刷新穿搭缓存...");

        String location = "北京";
        String weatherContext = "";

        try {
            WeatherUtil.WeatherInfo weather = weatherUtil.getCurrentWeather(location);
            weatherContext = String.format("当前城市：%s，气温：%s°C，天气：%s，体感：%s°C，湿度：%s%%",
                    location, weather.getTemp(), weather.getText(), weather.getFeelsLike(), weather.getHumidity());
        } catch (Exception e) {
            log.warn("获取天气信息失败: {}", e.getMessage());
            weatherContext = "天气数据获取失败，使用默认建议";
        }

        try {
            String maleInstruction = weatherContext + "。请为男性生成穿搭建议。请严格按照以下格式返回（每行一个项目，用英文冒号分隔）：\n上衣: \n下装: \n鞋子: \n配件: \n带伞: \n提示: ";
            String maleResult = agent.chat(maleInstruction, null);
            redisCacheUtil.setOutfit(location, "M", maleResult);
            log.info("男性穿搭建议已刷新缓存");
        } catch (Exception e) {
            log.error("男性穿搭生成失败: {}", e.getMessage());
            redisCacheUtil.setOutfit(location, "M", DEFAULT_OUTFIT);
        }

        try {
            String femaleInstruction = weatherContext + "。请为女性生成穿搭建议。请严格按照以下格式返回（每行一个项目，用英文冒号分隔）：\n上衣: \n下装: \n鞋子: \n配件: \n带伞: \n提示: ";
            String femaleResult = agent.chat(femaleInstruction, null);
            redisCacheUtil.setOutfit(location, "F", femaleResult);
            log.info("女性穿搭建议已刷新缓存");
        } catch (Exception e) {
            log.error("女性穿搭生成失败: {}", e.getMessage());
            redisCacheUtil.setOutfit(location, "F", DEFAULT_OUTFIT);
        }
    }

    private void refreshRecipeCache() {
        log.info("刷新食谱缓存...");

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);
        wrapper.eq(User::getDeleted, 0);
        List<User> users = userMapper.selectList(wrapper);

        int successCount = 0;
        int failCount = 0;

        for (User user : users) {
            try {
                Long userId = user.getId();
                UserHealth health = userHealthMapper.selectOne(new LambdaQueryWrapper<UserHealth>()
                        .eq(UserHealth::getUserId, userId)
                        .eq(UserHealth::getDeleted, 0));

                String recipe = generateRecipe(health);
                LocalDateTime executeTime = LocalDateTime.now();
                redisCacheUtil.setRecipeWithTime(userId, recipe, executeTime);
                successCount++;
            } catch (Exception e) {
                log.error("用户{}食谱刷新失败: {}", user.getId(), e.getMessage());
                failCount++;
            }
        }

        log.info("食谱缓存刷新完成，成功: {}，失败: {}", successCount, failCount);
    }

    private String generateRecipe(UserHealth health) {
        StringBuilder userInfo = new StringBuilder();
        userInfo.append("用户信息：\n");

        if (health != null) {
            if (health.getBmi() != null) {
                userInfo.append("- BMI：").append(health.getBmi()).append("\n");
            }
            if (health.getActivityLevel() != null) {
                userInfo.append("- 活动水平：").append(health.getActivityLevel()).append("\n");
            }
        }

        String prompt = userInfo + "请根据用户健康状况生成今日三餐饮食建议，按照以下JSON格式返回：\n" +
                "{\"breakfast\":{\"name\":\"\",\"calories\":\"\",\"foods\":[]}," +
                "\"lunch\":{\"name\":\"\",\"calories\":\"\",\"foods\":[]}," +
                "\"dinner\":{\"name\":\"\",\"calories\":\"\",\"foods\":[]}," +
                "\"tips\":[]}";

        try {
            String result = agent.chat(prompt, null);
            return result;
        } catch (Exception e) {
            log.error("AI食谱生成失败: {}", e.getMessage());
            return "{\"breakfast\":{\"name\":\"\",\"calories\":\"\",\"foods\":[]},\"lunch\":{\"name\":\"\",\"calories\":\"\",\"foods\":[]},\"dinner\":{\"name\":\"\",\"calories\":\"\",\"foods\":[]},\"tips\":[]}";
        }
    }
}
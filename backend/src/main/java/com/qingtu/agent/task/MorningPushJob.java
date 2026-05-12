package com.qingtu.agent.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.common.Constants;
import com.qingtu.agent.config.WeatherConfig;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.entity.po.SysNotification;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.mapper.SysNotificationMapper;
import com.qingtu.agent.util.UniPushUtil;
import com.qingtu.agent.util.WeatherUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

/**
 * 早安天气穿搭推送任务 — 每日07:00
 */
@Slf4j
@Component
public class MorningPushJob implements Job {

    private final UserMapper userMapper;
    private final SysNotificationMapper notificationMapper;
    private final UniPushUtil uniPushUtil;
    private final QingTuAgent qingTuAgent;

    public MorningPushJob(UserMapper userMapper, SysNotificationMapper notificationMapper,
                          UniPushUtil uniPushUtil, QingTuAgent qingTuAgent) {
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
        this.uniPushUtil = uniPushUtil;
        this.qingTuAgent = qingTuAgent;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("【定时任务】开始执行早安推送");

        var wrapper = new LambdaQueryWrapper<User>().eq(User::getStatus, 1).eq(User::getDeleted, 0);
        var users = userMapper.selectList(wrapper);
        int successCount = 0, failCount = 0;

        for (User user : users) {
            Long userId = user.getId();
            String lockKey = Constants.TASK_LOCK_PREFIX + Constants.TASK_MORNING_PUSH + "_" + userId;

            if (!com.qingtu.agent.util.RedisLockUtil.tryTaskLock(lockKey)) continue;

            try {
                String city = user.getCity() != null && !user.getCity().isBlank() ? user.getCity() : "北京";
                WeatherUtil weatherUtil = new WeatherUtil(new WeatherConfig());
                WeatherUtil.WeatherInfo weather = weatherUtil.getCurrentWeather(city);
                String weatherText = weather.getWeatherSummary();

                String outfitPrompt = weatherText + "\n请根据以上天气信息，为一位大学生推荐今日穿搭，控制在80字以内，亲切温暖。";
                String outfitContent = qingTuAgent.chat(outfitPrompt);
                if (outfitContent == null || outfitContent.isBlank()) {
                    outfitContent = "今日建议根据天气适时增减衣物，保持舒适~";
                }

                String fullContent = weatherText + "\n\n" + outfitContent;

                SysNotification notification = new SysNotification();
                notification.setUserId(userId);
                notification.setType(Constants.NOTIFY_TYPE_MORNING);
                notification.setTitle("👗 早安 · 今日天气穿搭");
                notification.setContent(fullContent);
                notification.setTargetPage("/pages/weather/index");
                notification.setCachedContent(outfitContent);
                notification.setStatus(0);
                notificationMapper.insert(notification);

                String clientId = user.getClientId();
                if (clientId != null && !clientId.isEmpty() && uniPushUtil.isConfigured()) {
                    uniPushUtil.pushToUser(clientId, "🌅 早安 · 今日穿搭", outfitContent);
                }

                successCount++;
            } catch (Exception e) {
                log.error("用户{}早安推送失败", userId, e);
                failCount++;
            } finally {
                com.qingtu.agent.util.RedisLockUtil.unlockTask(lockKey);
            }
        }

        log.info("【定时任务】早安推送完成，成功:{}，失败:{}", successCount, failCount);
    }
}
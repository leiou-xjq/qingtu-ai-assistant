package com.qingtu.agent.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.common.Constants;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.entity.po.SysNotification;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.mapper.SysNotificationMapper;
import com.qingtu.agent.service.DishService;
import com.qingtu.agent.util.UniPushUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 每日食谱预生成任务
 * 每天 06:30 执行，生成当日三餐食谱，存入消息中心
 */
@Slf4j
@Component
public class RecipePreGenJob implements Job {

    private final DishService dishService;
    private final UserMapper userMapper;
    private final SysNotificationMapper notificationMapper;
    private final UniPushUtil uniPushUtil;

    public RecipePreGenJob(DishService dishService, UserMapper userMapper,
                           SysNotificationMapper notificationMapper, UniPushUtil uniPushUtil) {
        this.dishService = dishService;
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
        this.uniPushUtil = uniPushUtil;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("【定时任务】开始生成今日食谱预缓存");

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);
        wrapper.eq(User::getDeleted, 0);
        java.util.List<User> users = userMapper.selectList(wrapper);

        int successCount = 0;
        int failCount = 0;

        for (User user : users) {
            try {
                Long userId = user.getId();
                String lockKey = Constants.TASK_LOCK_PREFIX + "recipe_pregen" + "_" + userId;

                if (!com.qingtu.agent.util.RedisLockUtil.tryTaskLock(lockKey)) {
                    log.debug("用户{}的食谱预生成任务正在执行", userId);
                    continue;
                }

                try {
                    // 调用 DishService 生成今日食谱
                    com.qingtu.agent.common.CommonResult<?> result = dishService.getTodayRecommendation(userId);

                    // 获取食谱内容
                    String recipeContent = "";
                    if (result != null && result.getData() != null) {
                        recipeContent = result.getData().toString();
                    }

                    if (recipeContent.isEmpty()) {
                        recipeContent = "今日食谱已生成，请打开查看";
                    }

                    // 保存到消息中心
                    SysNotification notification = new SysNotification();
                    notification.setUserId(userId);
                    notification.setType(Constants.NOTIFY_TYPE_DIET);
                    notification.setTitle("🍽️ 今日食谱推荐");
                    notification.setContent("点击查看今日三餐推荐");
                    notification.setTargetPage("/pages/diet/index");
                    notification.setDetailId(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
                    notification.setCachedContent(recipeContent);
                    notification.setStatus(0);
                    notificationMapper.insert(notification);

                    // 发送手机推送
                    String clientId = user.getClientId();
                    if (clientId != null && !clientId.isEmpty() && uniPushUtil.isConfigured()) {
                        boolean pushSuccess = uniPushUtil.pushToUser(clientId, "🍽️ 今日食谱推荐", "点击查看今日三餐推荐");
                        log.info("食谱手机推送结果: userId={}, success={}", userId, pushSuccess);
                    }

                    successCount++;
                } finally {
                    com.qingtu.agent.util.RedisLockUtil.unlockTask(lockKey);
                }
            } catch (Exception e) {
                log.error("用户{}食谱预生成失败", user.getId(), e);
                failCount++;
            }
        }

        log.info("【定时任务】食谱预生成完成，成功:{}，失败:{}", successCount, failCount);
    }
}

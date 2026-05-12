package com.qingtu.agent.task;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.Constants;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.entity.po.SysNotification;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.mapper.SysNotificationMapper;
import com.qingtu.agent.service.CostService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * 月度消费分析报告任务 — 每月28日
 */
@Slf4j
@Component
public class MonthlyReportJob implements Job {

    private final UserMapper userMapper;
    private final SysNotificationMapper notificationMapper;
    private final CostService costService;

    public MonthlyReportJob(UserMapper userMapper, SysNotificationMapper notificationMapper,
                             CostService costService) {
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
        this.costService = costService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("【定时任务】执行月度消费报告生成");

        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        int year = lastMonth.getYear();
        int month = lastMonth.getMonthValue();

        var users = userMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1).eq(User::getDeleted, 0));

        int successCount = 0;

        for (User user : users) {
            Long userId = user.getId();
            String lockKey = Constants.TASK_LOCK_PREFIX + Constants.TASK_MONTHLY_REPORT + "_" + userId;

            if (!com.qingtu.agent.util.RedisLockUtil.tryLock(lockKey, 300)) continue;

            try {
                CommonResult<?> reportResult = costService.getMonthlyReport(userId, year, month);
                String reportContent = "";

                if (reportResult != null && reportResult.getData() instanceof Map) {
                    Map<?, ?> data = (Map<?, ?>) reportResult.getData();
                    Object overview = data.get("overview");
                    if (overview != null) reportContent = overview.toString();
                }
                if (reportContent.isBlank()) {
                    reportContent = year + "年" + month + "月消费分析报告已生成，请前往记账页面查看详情。";
                }

                SysNotification notification = new SysNotification();
                notification.setUserId(userId);
                notification.setType(Constants.NOTIFY_TYPE_COST_REPORT);
                notification.setTitle("📊 " + month + "月消费分析报告");
                notification.setContent(year + "年" + month + "月消费分析报告已生成，点击查看详情。");
                notification.setCachedContent(reportContent);
                notification.setTargetPage("/pages/cost/index");
                notification.setStatus(0);
                notificationMapper.insert(notification);

                successCount++;
            } catch (Exception e) {
                log.error("用户{}月度报告生成失败", userId, e);
            } finally {
                com.qingtu.agent.util.RedisLockUtil.unlock(lockKey);
            }
        }

        log.info("【定时任务】月度消费报告生成完成，成功:{}条", successCount);
    }
}
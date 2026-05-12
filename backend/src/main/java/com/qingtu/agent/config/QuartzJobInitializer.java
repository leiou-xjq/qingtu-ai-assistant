package com.qingtu.agent.config;

import com.qingtu.agent.task.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzJobInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final Scheduler scheduler;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            log.info("开始注册定时任务...");
            
            // 1. 早安推送任务 - 每天07:00
            registerJob(MorningPushJob.class, "morningPush", "daily", "0 0 7 * * ?");
            
            // 2. 食谱预生成任务 - 每天06:30
            registerJob(RecipePreGenJob.class, "recipePreGen", "daily", "0 30 6 * * ?");
            
            // 3. 课程提醒任务 - 每分钟执行
            registerJob(CourseReminderJob.class, "courseReminder", "minute", "0 * * * * ?");
            
            // 4. 学习复盘任务 - 每天22:00
            registerJob(DailySummaryJob.class, "dailySummary", "daily", "0 0 22 * * ?");
            
            // 5. 月度报告任务 - 每月1日00:00
            registerJob(MonthlyReportJob.class, "monthlyReport", "monthly", "0 0 1 1 * ?");
            
            // 6. 缓存刷新任务 - 每天04:00
            registerJob(CacheRefreshJob.class, "cacheRefresh", "daily", "0 0 4 * * ?");
            
            // 7. 课程笔记任务 - 每天19:00
            registerJob(CourseNoteJob.class, "courseNote", "daily", "0 0 20 * * ?");
            
            // 8. 穿搭缓存刷新任务 - 每2小时执行(6:00-22:00)
            registerJob(OutfitRefreshJob.class, "outfitRefresh", "twohours", "0 0 6-22/2 * * ?");
            
            log.info("定时任务注册完成");
        } catch (Exception e) {
            log.error("定时任务注册失败", e);
        }
    }

    private void registerJob(Class<? extends Job> jobClass, String jobName, String groupName, String cronExpression) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, groupName);
            
            // 如果任务已存在，先删除
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("删除已存在的任务: {}", jobName);
            }
            
            // 创建JobDetail
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobKey)
                    .storeDurably()
                    .build();
            
            // 创建Trigger
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobName + "Trigger", groupName)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .forJob(jobDetail)
                    .build();
            
            // 注册到调度器
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("注册任务成功: {} - {}", jobName, cronExpression);
            
        } catch (Exception e) {
            log.error("注册任务失败: {}", jobName, e);
        }
    }
}
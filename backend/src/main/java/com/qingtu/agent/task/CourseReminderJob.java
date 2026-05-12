package com.qingtu.agent.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.common.Constants;
import com.qingtu.agent.entity.po.CourseSchedule;
import com.qingtu.agent.entity.po.SysNotification;
import com.qingtu.agent.mapper.CourseScheduleMapper;
import com.qingtu.agent.mapper.SysNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 课前提醒任务
 */
@Slf4j
@Component
public class CourseReminderJob implements Job {

    private final CourseScheduleMapper courseMapper;
    private final SysNotificationMapper notificationMapper;

    public CourseReminderJob(CourseScheduleMapper courseMapper, SysNotificationMapper notificationMapper) {
        this.courseMapper = courseMapper;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.debug("【定时任务】执行课前提醒检查");

        int weekday = WeekUtil.getDayOfWeek();
        LocalTime now = LocalTime.now();

        List<CourseSchedule> courses = courseMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getWeekday, weekday)
                        .eq(CourseSchedule::getReminderEnabled, 1)
                        .eq(CourseSchedule::getDeleted, 0)
        );

        int reminderCount = 0;

        for (CourseSchedule course : courses) {
            LocalTime startTime = course.getStartTime();
            int reminderMinutes = course.getReminderMinutes() != null ? course.getReminderMinutes() : 15;
            
            LocalTime reminderTime = startTime.minusMinutes(reminderMinutes);
            
            if (Math.abs(now.toSecondOfDay() - reminderTime.toSecondOfDay()) <= 60) {
                String lockKey = Constants.TASK_LOCK_PREFIX + Constants.TASK_COURSE_REMINDER + "_" + course.getId();
                
                if (com.qingtu.agent.util.RedisLockUtil.tryLock(lockKey, 60)) {
                    try {
                        SysNotification notification = new SysNotification();
                        notification.setUserId(course.getUserId());
                        notification.setType(Constants.NOTIFY_TYPE_COURSE);
                        notification.setTitle("课程提醒 ⏰");
                        notification.setContent(String.format("您有课程《%s》将在%d分钟后开始\n📍 地点：%s\n🕐 时间：%s-%s",
                                course.getName(),
                                reminderMinutes,
                                course.getLocation(),
                                course.getStartTime(),
                                course.getEndTime()));
                        notification.setTargetPage("/pages/course/index");
                        notification.setDetailId(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
                        notification.setStatus(0);
                        notificationMapper.insert(notification);

                        reminderCount++;
                        log.info("发送课程提醒：{}，{}分钟后开始", course.getName(), reminderMinutes);
                    } finally {
                        com.qingtu.agent.util.RedisLockUtil.unlock(lockKey);
                    }
                }
            }
        }

        if (reminderCount > 0) {
            log.info("【定时任务】课前提醒完成，共发送{}条提醒", reminderCount);
        }
    }
}
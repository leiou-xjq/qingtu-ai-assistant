package com.qingtu.agent.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.Constants;
import com.qingtu.agent.entity.po.CourseSchedule;
import com.qingtu.agent.entity.po.SysNotification;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.mapper.CourseScheduleMapper;
import com.qingtu.agent.mapper.SysNotificationMapper;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 下课课程笔记生成任务 - 每日20:00
 */
@Slf4j
@Component
public class CourseNoteJob implements Job {

    private final CourseScheduleMapper courseMapper;
    private final SysNotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final CourseService courseService;

    public CourseNoteJob(CourseScheduleMapper courseMapper, SysNotificationMapper notificationMapper,
                         UserMapper userMapper, CourseService courseService) {
        this.courseMapper = courseMapper;
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.courseService = courseService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("【定时任务】执行课程笔记生成");

        int weekday = WeekUtil.getDayOfWeek();
        LocalDate today = LocalDate.now();

        List<CourseSchedule> courses = courseMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getWeekday, weekday)
                        .eq(CourseSchedule::getDeleted, 0)
        );

        int generatedCount = 0;

        for (CourseSchedule course : courses) {
            Long userId = course.getUserId();
            String lockKey = Constants.TASK_LOCK_PREFIX + Constants.TASK_COURSE_NOTE + "_" + course.getId();

            if (!com.qingtu.agent.util.RedisLockUtil.tryLock(lockKey, 300)) continue;

            try {
                User user = userMapper.selectById(userId);
                if (user != null && user.getSemesterStart() != null) {
                    WeekUtil.setSemesterStart(user.getSemesterStart());
                } else {
                    WeekUtil.setSemesterStart(java.time.LocalDate.of(2025, 3, 3));
                }

                CommonResult<?> result = courseService.generateTodayNotes(userId);
                String notes = "";

                if (result != null && result.getData() instanceof Map) {
                    Object notesObj = ((Map<?, ?>) result.getData()).get("notes");
                    if (notesObj != null) notes = notesObj.toString();
                }
                if (notes.isBlank()) {
                    notes = "今日《" + course.getName() + "》的AI笔记已生成，请在笔记中心查看。";
                }

                SysNotification notification = new SysNotification();
                notification.setUserId(userId);
                notification.setType(Constants.NOTIFY_TYPE_NOTE);
                notification.setTitle("📝 今日课程笔记");
                notification.setContent("今日《" + course.getName() + "》的AI课程笔记已生成，点击查看详情。");
                notification.setCachedContent(notes);
                notification.setTargetPage("/pages/note/index");
                notification.setStatus(0);
                notificationMapper.insert(notification);

                generatedCount++;
                log.info("课程笔记生成完成：userId={}, course={}", userId, course.getName());
            } catch (Exception e) {
                log.error("课程笔记生成失败：course={}", course.getName(), e);
            } finally {
                com.qingtu.agent.util.RedisLockUtil.unlock(lockKey);
                WeekUtil.clearSemesterStart();
            }
        }

        log.info("【定时任务】课程笔记生成完成，共{}条", generatedCount);
    }
}
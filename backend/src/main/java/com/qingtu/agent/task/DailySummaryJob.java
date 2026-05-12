package com.qingtu.agent.task;

import com.qingtu.agent.common.Constants;
import com.qingtu.agent.entity.po.CourseKeyPoint;
import com.qingtu.agent.entity.po.CourseSchedule;
import com.qingtu.agent.entity.po.SysNotification;
import com.qingtu.agent.mapper.CourseKeyPointMapper;
import com.qingtu.agent.mapper.CourseScheduleMapper;
import com.qingtu.agent.mapper.SysNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每日笔记汇总任务 — 每日22:00
 */
@Slf4j
@Component
public class DailySummaryJob implements Job {

    private final SysNotificationMapper notificationMapper;
    private final CourseKeyPointMapper noteMapper;
    private final CourseScheduleMapper courseMapper;

    public DailySummaryJob(SysNotificationMapper notificationMapper,
                          CourseKeyPointMapper noteMapper,
                          CourseScheduleMapper courseMapper) {
        this.notificationMapper = notificationMapper;
        this.noteMapper = noteMapper;
        this.courseMapper = courseMapper;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("【定时任务】执行每日笔记汇总");

        LocalDate today = LocalDate.now();

        List<CourseKeyPoint> todayNotes = noteMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseKeyPoint>()
                        .eq(CourseKeyPoint::getClassDate, today)
                        .eq(CourseKeyPoint::getDeleted, 0)
        );

        if (todayNotes.isEmpty()) {
            log.info("今日无课程笔记，跳过汇总");
            return;
        }

        // 按 userId 分组，每个用户只看自己的笔记
        Map<Long, List<CourseKeyPoint>> userNotes = todayNotes.stream()
                .collect(Collectors.groupingBy(CourseKeyPoint::getUserId));

        for (var entry : userNotes.entrySet()) {
            Long userId = entry.getKey();
            List<CourseKeyPoint> notes = entry.getValue();

            StringBuilder summary = new StringBuilder();
            summary.append("📚 今日学习复盘\n\n");
            summary.append("今日共完成").append(notes.size()).append("节课的学习笔记：\n\n");

            for (CourseKeyPoint note : notes) {
                summary.append("▸ ").append(note.getCourseName());
                if (note.getSummary() != null && !note.getSummary().isBlank()) {
                    summary.append("：").append(note.getSummary());
                }
                summary.append("\n");
            }

            summary.append("\n💡 建议今晚复习一下今日课程重点，为明天的学习做好准备~");

            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setType(Constants.NOTIFY_TYPE_NOTE);
            notification.setTitle("📋 今日学习复盘");
            notification.setContent(summary.toString());
            notification.setTargetPage("/pages/note/index");
            notification.setStatus(0);
            notificationMapper.insert(notification);
        }

        log.info("【定时任务】每日笔记汇总完成，共推送{}个用户", userNotes.size());
    }
}
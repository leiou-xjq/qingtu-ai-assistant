package com.qingtu.agent.task;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 周工具类
 */
public class WeekUtil {

    private static final LocalDate DEFAULT_SEMESTER_START = LocalDate.of(2025, 3, 3);

    private static final ThreadLocal<LocalDate> semesterStartHolder = ThreadLocal.withInitial(() -> DEFAULT_SEMESTER_START);

    public static void setSemesterStart(LocalDate date) {
        semesterStartHolder.set(date);
    }

    public static LocalDate getSemesterStart() {
        return semesterStartHolder.get();
    }

    public static void clearSemesterStart() {
        semesterStartHolder.remove();
    }

    public static int getDayOfWeek() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek.getValue();
    }

    public static int getWeekOfYear() {
        return getCurrentWeek(null);
    }

    public static int getCurrentWeek(Integer weekNum) {
        if (weekNum != null) return weekNum;
        LocalDate semesterStart = semesterStartHolder.get();
        long daysSinceStart = semesterStart.until(LocalDate.now()).getDays();
        if (daysSinceStart < 0) return 1;
        return (int) (daysSinceStart / 7) + 1;
    }

    public static String getWeekDateRange(LocalDate semesterStart, int weekNum) {
        if (semesterStart == null) {
            semesterStart = semesterStartHolder.get();
        }
        LocalDate weekStartDate = semesterStart.plusDays((weekNum - 1) * 7L);
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        return weekStartDate + " 至 " + weekEndDate;
    }
}

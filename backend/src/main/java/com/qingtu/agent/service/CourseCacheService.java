package com.qingtu.agent.service;

import com.qingtu.agent.entity.dto.CourseImportDTO;
import java.util.List;

public interface CourseCacheService {

    void saveCourses(Long userId, List<CourseImportDTO> courses);

    List<CourseImportDTO> getCourses(Long userId);

    void clearCourses(Long userId);

    void syncToDatabase(Long userId);
}

/**
 * 课程相关API
 */
import request from '../utils/request'

// 获取周课表
export const getWeekSchedule = (weekNum) => request.get('/course/schedule', { weekNum })

// 获取今日课程
export const getTodayCourses = () => request.get('/course/today')

// 添加课程
export const addCourse = (data) => request.post('/course/add', data)

// 更新课程
export const updateCourse = (id, data) => request.put(`/course/${id}`, data)

// 删除课程
export const deleteCourse = (id) => request.delete(`/course/${id}`)

// 导入课表
export const importSchedule = (file) => request.upload('/course/import', file, 'file')

// 下载课表模板
export const downloadTemplate = () => request.get('/course/template')

// 设置课程提醒
export const setCourseReminder = (id, enabled, minutes) => 
  request.put(`/course/${id}/reminder`, { enabled, minutes })
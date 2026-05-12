import request from '../utils/request'

export const listNotes = (page = 1, size = 20) => request.get('/note/list', { page, size })

export const getNoteDetail = (id) => request.get(`/note/${id}`)

export const deleteNote = (id) => request.del(`/note/${id}`)

export const getNotesByCourse = (courseId) => request.get(`/note/course/${courseId}`)

export const analyzeCourseProgress = (courseId) => request.get(`/note/progress/${courseId}`)

export const generateNote = (courseId, weekNum) => request.post(`/note/generate/${courseId}`, { weekNum })

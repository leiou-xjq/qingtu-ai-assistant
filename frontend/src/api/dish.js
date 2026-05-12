import request from '../utils/request'

export const listDishes = (type, category, page, size) => request.get('/dish/list', { type, category, page, size })

export const recommendDishes = (type, userId) => request.get('/dish/recommend', { type, userId })

export const getTodayRecommendation = () => request.get('/dish/today')

export const refreshTodayRecommendation = () => request.post('/dish/today/refresh')
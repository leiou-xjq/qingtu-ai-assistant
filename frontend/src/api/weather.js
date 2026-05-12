/**
 * 天气相关API
 */
import request from '../utils/request'

// 获取实时天气
export const getCurrentWeather = (location) => request.get('/weather/current', { location })

// 获取天气预报
export const getForecast = (location, days) => request.get('/weather/forecast', { location, days })

// 获取穿搭建议
export const getOutfitSuggestion = () => request.get('/weather/outfit')

// 预加载穿搭建议（异步）
export const preloadOutfitSuggestion = () => request.post('/weather/outfit/preload')

// 发送早安推送
export const sendMorningPush = () => request.post('/weather/morning-push')
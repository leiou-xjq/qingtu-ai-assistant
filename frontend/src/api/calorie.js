import request from '../utils/request'

export const recordIntake = (mealType, foodInput) => request.post('/calorie/intake', { mealType, foodInput })

export const getTodayProgress = () => request.get('/calorie/progress')
import request from '../utils/request'

export const getHealthKpi = () => request.get('/health/kpi')

export const getHealthInfo = () => request.get('/health/info')

export const updateHealth = (data) => request.put('/health/update', data)
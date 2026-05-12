/**
 * 用户相关API
 */
import request from '../utils/request'

// 用户登录
export const login = (data) => request.post('/user/login', data)

// 用户注册
export const register = (data) => request.post('/user/register', data)

// 获取用户信息
export const getUserInfo = () => request.get('/user/info')

// 更新用户信息
export const updateUserInfo = (data) => request.put('/user/info', data)

// 修改密码
export const changePassword = (data) => request.put('/user/password', data)

// 退出登录
export const logout = () => request.post('/user/logout')

// 保存客户端推送ID
export const saveClientId = (clientId) => request.post('/user/client-id', { clientId })
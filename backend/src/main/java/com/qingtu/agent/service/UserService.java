package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.entity.dto.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口
 * 
 * 定义用户相关的业务操作
 * 
 * @author 青途智伴技术团队
 */
public interface UserService {

    /**
     * 用户注册
     * 
     * @param dto 注册信息
     * @return 用户信息+Token
     */
    CommonResult<?> register(RegisterDTO dto);

    /**
     * 用户登录
     * 
     * @param dto 登录信息
     * @return 用户信息+Token
     */
    CommonResult<?> login(LoginDTO dto);

    /**
     * 获取当前用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    CommonResult<?> getUserInfo(Long userId);

    /**
     * 更新用户信息
     * 
     * @param userId 用户ID
     * @param dto 更新信息
     * @return 更新结果
     */
    CommonResult<?> updateUserInfo(Long userId, UpdateUserDTO dto);

    /**
     * 修改密码
     * 
     * @param userId 用户ID
     * @param dto 密码信息
     * @return 修改结果
     */
    CommonResult<?> changePassword(Long userId, ChangePasswordDTO dto);

    /**
     * 退出登录
     * 
     * @param userId 用户ID
     * @return 退出结果
     */
    CommonResult<?> logout(Long userId);

    /**
     * 保存客户端推送ID
     *
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @return 保存结果
     */
    CommonResult<?> saveClientId(Long userId, String clientId);

    /**
     * 上传头像
     *
     * @param userId 用户ID
     * @param file 头像文件
     * @return 上传结果
     */
    CommonResult<?> uploadAvatar(Long userId, MultipartFile file);
}
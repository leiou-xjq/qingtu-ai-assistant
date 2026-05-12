package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.UserService;
import com.qingtu.agent.util.JwtUtil;
import com.qingtu.agent.entity.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理控制器
 * 
 * 提供用户注册、登录、信息管理等功能
 * 
 * @author 青途智伴技术团队
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public CommonResult<?> register(@Valid @RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public CommonResult<?> login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public CommonResult<?> getUserInfo(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return userService.getUserInfo(userId);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public CommonResult<?> updateUserInfo(HttpServletRequest request, @RequestBody UpdateUserDTO dto) {
        Long userId = getUserIdFromRequest(request);
        return userService.updateUserInfo(userId, dto);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public CommonResult<?> changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = getUserIdFromRequest(request);
        return userService.changePassword(userId, dto);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public CommonResult<?> logout(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return userService.logout(userId);
    }

    /**
     * 保存客户端推送ID
     */
    @PostMapping("/client-id")
    public CommonResult<?> saveClientId(HttpServletRequest request, @RequestBody java.util.Map<String, String> body) {
        Long userId = getUserIdFromRequest(request);
        String clientId = body.get("clientId");
        if (clientId == null || clientId.isEmpty()) {
            return CommonResult.fail("clientId 不能为空");
        }
        return userService.saveClientId(userId, clientId);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public CommonResult<?> uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return CommonResult.fail("请选择图片文件");
        }
        Long userId = getUserIdFromRequest(request);
        return userService.uploadAvatar(userId, file);
    }

    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}
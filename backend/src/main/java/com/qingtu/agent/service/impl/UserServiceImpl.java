package com.qingtu.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.ResultCode;
import com.qingtu.agent.entity.dto.LoginDTO;
import com.qingtu.agent.entity.dto.RegisterDTO;
import com.qingtu.agent.entity.dto.UpdateUserDTO;
import com.qingtu.agent.entity.dto.ChangePasswordDTO;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.entity.po.UserHealth;
import com.qingtu.agent.entity.vo.UserVO;
import com.qingtu.agent.exception.BusinessException;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.mapper.UserHealthMapper;
import com.qingtu.agent.service.UserService;
import com.qingtu.agent.service.SchoolDataInitService;
import com.qingtu.agent.util.AliyunOssUtil;
import com.qingtu.agent.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户服务实现类
 * 
 * @author 青途智伴技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserHealthMapper userHealthMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final AliyunOssUtil aliyunOssUtil;
    private final SchoolDataInitService schoolDataInitService;

    @Override
    public CommonResult<?> register(RegisterDTO dto) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())
                .eq(User::getDeleted, 0)).intValue() > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(md5Password(dto.getPassword()));
        user.setNickname(dto.getNickname() != null && !dto.getNickname().isEmpty() ? dto.getNickname() : dto.getUsername());
        user.setSchool(dto.getSchool());
        user.setCity(dto.getCity() != null ? dto.getCity() : "北京");
        user.setStatus(1);
        user.setRole("user");
        user.setSemesterStart(LocalDate.now().withDayOfYear(1).plusMonths(1).withDayOfMonth(26));
        user.setTotalWeeks(16);

        userMapper.insert(user);

        // 异步初始化学校专属数据
        if (user.getSchool() != null && !user.getSchool().isBlank()) {
            schoolDataInitService.initSchoolData(user.getSchool());
        }

        if (dto.getHeight() != null || dto.getWeight() != null || dto.getAge() != null) {
            UserHealth health = new UserHealth();
            health.setUserId(user.getId());
            if (dto.getHeight() != null) {
                health.setHeight(java.math.BigDecimal.valueOf(dto.getHeight()));
            }
            if (dto.getWeight() != null) {
                health.setWeight(java.math.BigDecimal.valueOf(dto.getWeight()));
            }
            if (dto.getAge() != null) health.setAge(dto.getAge());
            if (dto.getGender() != null) {
                String genderStr;
                if (dto.getGender().equals(1)) {
                    genderStr = "M";
                } else if (dto.getGender().equals(2)) {
                    genderStr = "F";
                } else {
                    genderStr = "M";
                }
                health.setGender(genderStr);
            }
            if (dto.getActivityLevel() != null) {
                health.setActivityLevel(java.math.BigDecimal.valueOf(dto.getActivityLevel()));
            } else {
                health.setActivityLevel(java.math.BigDecimal.valueOf(1.2));
            }
            if (dto.getHeight() != null && dto.getWeight() != null) {
                double bmi = dto.getWeight() / Math.pow(dto.getHeight() / 100, 2);
                health.setBmi(java.math.BigDecimal.valueOf(bmi));
            }
            userHealthMapper.insert(health);
        }

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setCity(user.getCity());
        vo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername()));

        log.info("用户注册成功：{}", user.getUsername());
        return CommonResult.success("注册成功", vo);
    }

    @Override
    public CommonResult<?> login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())
                .eq(User::getDeleted, 0));

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!user.getPassword().equals(md5Password(dto.getPassword()))) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setCity(user.getCity());
        vo.setRole(user.getRole());
        vo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername()));

        log.info("用户登录成功：{}", user.getUsername());
        return CommonResult.success("登录成功", vo);
    }

    @Override
    public CommonResult<?> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setCity(user.getCity());
        vo.setSchool(user.getSchool());
        vo.setGender(user.getGender());
        vo.setSemesterStart(user.getSemesterStart() != null ? user.getSemesterStart().toString() : null);
        vo.setTotalWeeks(user.getTotalWeeks());
        vo.setRole(user.getRole());

        return CommonResult.success(vo);
    }

    @Override
    public CommonResult<?> updateUserInfo(Long userId, UpdateUserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getCity() != null) user.setCity(dto.getCity());
        if (dto.getSchool() != null) user.setSchool(dto.getSchool());
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
            
            UserHealth health = userHealthMapper.selectOne(
                new LambdaQueryWrapper<UserHealth>()
                    .eq(UserHealth::getUserId, userId)
            );
            if (health != null) {
                String genderStr;
                if (dto.getGender().equals(1)) {
                    genderStr = "M";
                } else if (dto.getGender().equals(2)) {
                    genderStr = "F";
                } else {
                    genderStr = "M";
                }
                health.setGender(genderStr);
                userHealthMapper.updateById(health);
            }
        }
        if (dto.getSemesterStart() != null) {
            user.setSemesterStart(java.time.LocalDate.parse(dto.getSemesterStart()));
        }
        if (dto.getTotalWeeks() != null) {
            user.setTotalWeeks(dto.getTotalWeeks());
        }
        userMapper.updateById(user);

        return CommonResult.success("更新成功", null);
    }

    @Override
    public CommonResult<?> changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!user.getPassword().equals(md5Password(dto.getOldPassword()))) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        user.setPassword(md5Password(dto.getNewPassword()));
        userMapper.updateById(user);

        return CommonResult.success("密码修改成功", null);
    }

    @Override
    public CommonResult<?> logout(Long userId) {
        return CommonResult.success("退出成功", null);
    }

    @Override
    public CommonResult<?> saveClientId(Long userId, String clientId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return CommonResult.fail("用户不存在");
        }
        user.setClientId(clientId);
        userMapper.updateById(user);
        return CommonResult.success("保存成功", null);
    }

    @Override
    public CommonResult<?> uploadAvatar(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return CommonResult.fail("用户不存在");
        }

        String avatarUrl = aliyunOssUtil.uploadAvatar(file, userId);
        if (avatarUrl == null) {
            return CommonResult.fail("头像上传失败");
        }

        user.setAvatar(avatarUrl);
        userMapper.updateById(user);
        return CommonResult.success("上传成功", avatarUrl);
    }

    private String md5Password(String password) {
        return DigestUtils.md5DigestAsHex(("qingtu" + password).getBytes());
    }
}
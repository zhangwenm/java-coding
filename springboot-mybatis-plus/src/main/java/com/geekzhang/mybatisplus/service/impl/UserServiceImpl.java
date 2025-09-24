package com.geekzhang.mybatisplus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geekzhang.mybatisplus.entity.User;
import com.geekzhang.mybatisplus.mapper.UserMapper;
import com.geekzhang.mybatisplus.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现类
 *
 * @author geekzhang
 * @since 2025-09-24
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return baseMapper.selectByUsername(username);
    }

    @Override
    public User getByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return baseMapper.selectByEmail(email);
    }

    @Override
    public User getByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return baseMapper.selectByPhone(phone);
    }

    @Override
    public List<User> getActiveUsers() {
        return baseMapper.selectActiveUsers();
    }

    @Override
    public List<User> searchByNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return List.of();
        }
        return baseMapper.selectByNicknameLike(nickname);
    }

    @Override
    public List<UserMapper.UserStatusStatistics> getUserStatusStatistics() {
        return baseMapper.selectUserStatusStatistics();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty() || status == null) {
            return 0;
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(User::getId, ids)
                    .set(User::getStatus, status)
                    .set(User::getUpdateTime, LocalDateTime.now());

        int updateCount = baseMapper.update(null, updateWrapper);
        log.info("批量更新用户状态完成，更新数量：{}，目标状态：{}", updateCount, status);
        return updateCount;
    }

    @Override
    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, String newPassword) {
        if (userId == null || !StringUtils.hasText(newPassword)) {
            log.warn("重置密码参数无效：userId={}, newPassword={}", userId, newPassword);
            return false;
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                    .set(User::getPassword, newPassword)
                    .set(User::getUpdateTime, LocalDateTime.now());

        int updateCount = baseMapper.update(null, updateWrapper);
        boolean success = updateCount > 0;
        
        if (success) {
            log.info("用户密码重置成功，用户ID：{}", userId);
        } else {
            log.warn("用户密码重置失败，用户ID：{}", userId);
        }
        
        return success;
    }

    /**
     * 根据多个条件组合查询用户
     *
     * @param username 用户名
     * @param email    邮箱
     * @param phone    手机号
     * @param status   状态
     * @return 查询结果
     */
    public List<User> getByMultiConditions(String username, String email, String phone, Integer status) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(username)) {
            queryWrapper.like(User::getUsername, username);
        }
        if (StringUtils.hasText(email)) {
            queryWrapper.like(User::getEmail, email);
        }
        if (StringUtils.hasText(phone)) {
            queryWrapper.like(User::getPhone, phone);
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }
        
        queryWrapper.orderByDesc(User::getCreateTime);
        return baseMapper.selectList(queryWrapper);
    }
}

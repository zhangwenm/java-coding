package com.geekzhang.mybatisplus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.geekzhang.mybatisplus.entity.User;
import com.geekzhang.mybatisplus.mapper.UserMapper;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author geekzhang
 * @since 2025-09-24
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    User getByEmail(String email);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    User getByPhone(String phone);

    /**
     * 查询启用状态的用户列表
     *
     * @return 用户列表
     */
    List<User> getActiveUsers();

    /**
     * 根据昵称模糊查询用户
     *
     * @param nickname 昵称关键词
     * @return 用户列表
     */
    List<User> searchByNickname(String nickname);

    /**
     * 获取用户状态统计信息
     *
     * @return 统计结果
     */
    List<UserMapper.UserStatusStatistics> getUserStatusStatistics();

    /**
     * 批量更新用户状态
     *
     * @param ids    用户ID列表
     * @param status 状态
     * @return 更新成功数量
     */
    int batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return 是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 重置用户密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean resetPassword(Long userId, String newPassword);
}

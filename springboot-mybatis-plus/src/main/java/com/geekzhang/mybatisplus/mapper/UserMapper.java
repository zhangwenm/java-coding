package com.geekzhang.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.geekzhang.mybatisplus.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper接口
 *
 * @author geekzhang
 * @since 2025-09-24
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted = 0")
    User selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted = 0")
    User selectByPhone(@Param("phone") String phone);

    /**
     * 查询启用状态的用户列表
     *
     * @return 用户列表
     */
    @Select("SELECT * FROM sys_user WHERE status = 1 AND deleted = 0 ORDER BY create_time DESC")
    List<User> selectActiveUsers();

    /**
     * 根据昵称模糊查询用户
     *
     * @param nickname 昵称关键词
     * @return 用户列表
     */
    @Select("SELECT * FROM sys_user WHERE nickname LIKE CONCAT('%', #{nickname}, '%') " +
            "AND deleted = 0 ORDER BY create_time DESC")
    List<User> selectByNicknameLike(@Param("nickname") String nickname);

    /**
     * 统计用户状态分布
     *
     * @return 统计结果
     */
    @Select("SELECT status, COUNT(*) as count FROM sys_user WHERE deleted = 0 GROUP BY status")
    List<UserStatusStatistics> selectUserStatusStatistics();

    /**
     * 用户状态统计结果内部类
     */
    class UserStatusStatistics {
        private Integer status;
        private Long count;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}

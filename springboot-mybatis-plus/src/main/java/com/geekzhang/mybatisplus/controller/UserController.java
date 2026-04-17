package com.geekzhang.mybatisplus.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.geekzhang.mybatisplus.entity.User;
import com.geekzhang.mybatisplus.mapper.UserMapper;
import com.geekzhang.mybatisplus.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 *
 * @author geekzhang
 * @since 2025-09-24
 */
@Slf4j
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    @Operation(summary = "测试post")
    @PostMapping("/test")
    public Map test(@RequestBody JSONObject json) {

        log.info("json:{}", JSON.toJSONString(json));
        return new JSONObject();
    }
    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Page<User> page(
            @Parameter(description = "页码") @RequestParam() Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "用户名") @RequestParam  String username,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email,
            @Parameter(description = "手机号") @RequestParam(required = false) String phone,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        if (username != null && !username.trim().isEmpty()) {
            queryWrapper.like(User::getUsername, username);
        }
        if (email != null && !email.trim().isEmpty()) {
            queryWrapper.like(User::getEmail, email);
        }
        if (phone != null && !phone.trim().isEmpty()) {
            queryWrapper.like(User::getPhone, phone);
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }
        
        queryWrapper.orderByDesc(User::getCreateTime);
        return userService.page(page, queryWrapper);
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public User getById(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "根据用户名查询用户")
    @GetMapping("/username/{username}")
    public User getByUsername(@Parameter(description = "用户名") @PathVariable String username) {
        return userService.getByUsername(username);
    }

    @Operation(summary = "根据邮箱查询用户")
    @GetMapping("/email/{email}")
    public User getByEmail(@Parameter(description = "邮箱") @PathVariable String email) {
        return userService.getByEmail(email);
    }

    @Operation(summary = "根据手机号查询用户")
    @GetMapping("/phone/{phone}")
    public User getByPhone(@Parameter(description = "手机号") @PathVariable String phone) {
        return userService.getByPhone(phone);
    }

    @Operation(summary = "查询启用状态的用户")
    @GetMapping("/active")
    public List<User> getActiveUsers() {
        return userService.getActiveUsers();
    }

    @Operation(summary = "根据昵称搜索用户")
    @GetMapping("/search")
    public List<User> searchByNickname(@Parameter(description = "昵称关键词") @RequestParam String nickname) {
        return userService.searchByNickname(nickname);
    }

    @Operation(summary = "获取用户状态统计")
    @GetMapping("/statistics/status")
    public List<UserMapper.UserStatusStatistics> getUserStatusStatistics() {
        return userService.getUserStatusStatistics();
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public boolean save(@RequestBody User user) {
        return userService.save(user);
    }

    @Operation(summary = "更新用户")
    @PutMapping
    public boolean update(@RequestBody User user) {
        return userService.updateById(user);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userService.removeById(id);
    }

    @Operation(summary = "批量更新用户状态")
    @PutMapping("/batch/status")
    public int batchUpdateStatus(@RequestBody List<Long> ids, @RequestParam Integer status) {
        return userService.batchUpdateStatus(ids, status);
    }

    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/exists/username/{username}")
    public boolean existsByUsername(@Parameter(description = "用户名") @PathVariable String username) {
        return userService.existsByUsername(username);
    }

    @Operation(summary = "检查邮箱是否存在")
    @GetMapping("/exists/email/{email}")
    public boolean existsByEmail(@Parameter(description = "邮箱") @PathVariable String email) {
        return userService.existsByEmail(email);
    }

    @Operation(summary = "检查手机号是否存在")
    @GetMapping("/exists/phone/{phone}")
    public boolean existsByPhone(@Parameter(description = "手机号") @PathVariable String phone) {
        return userService.existsByPhone(phone);
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/password")
    public boolean resetPassword(@Parameter(description = "用户ID") @PathVariable Long id, 
                               @Parameter(description = "新密码") @RequestParam String newPassword) {
        return userService.resetPassword(id, newPassword);
    }
}

package com.geekzhang.mybatisplus.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.geekzhang.mybatisplus.entity.User;
import com.geekzhang.mybatisplus.mapper.UserMapper;
import com.geekzhang.mybatisplus.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    @ApiOperation("测试post")
    @PostMapping("/test")
    public Map test(@RequestBody JSONObject json) {

        log.info("json:{}", JSON.toJSONString(json));
        return new JSONObject();
    }
    @ApiOperation("分页查询用户")
    @GetMapping("/page")
    public Page<User> page(
            @ApiParam("页码") @RequestParam() Long current,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Long size,
            @ApiParam("用户名") @RequestParam  String username,
            @ApiParam("邮箱") @RequestParam(required = false) String email,
            @ApiParam("手机号") @RequestParam(required = false) String phone,
            @ApiParam("状态") @RequestParam(required = false) Integer status) {
        
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

    @ApiOperation("根据ID查询用户")
    @GetMapping("/{id}")
    public User getById(@ApiParam("用户ID") @PathVariable Long id) {
        return userService.getById(id);
    }

    @ApiOperation("根据用户名查询用户")
    @GetMapping("/username/{username}")
    public User getByUsername(@ApiParam("用户名") @PathVariable String username) {
        return userService.getByUsername(username);
    }

    @ApiOperation("根据邮箱查询用户")
    @GetMapping("/email/{email}")
    public User getByEmail(@ApiParam("邮箱") @PathVariable String email) {
        return userService.getByEmail(email);
    }

    @ApiOperation("根据手机号查询用户")
    @GetMapping("/phone/{phone}")
    public User getByPhone(@ApiParam("手机号") @PathVariable String phone) {
        return userService.getByPhone(phone);
    }

    @ApiOperation("查询启用状态的用户")
    @GetMapping("/active")
    public List<User> getActiveUsers() {
        return userService.getActiveUsers();
    }

    @ApiOperation("根据昵称搜索用户")
    @GetMapping("/search")
    public List<User> searchByNickname(@ApiParam("昵称关键词") @RequestParam String nickname) {
        return userService.searchByNickname(nickname);
    }

    @ApiOperation("获取用户状态统计")
    @GetMapping("/statistics/status")
    public List<UserMapper.UserStatusStatistics> getUserStatusStatistics() {
        return userService.getUserStatusStatistics();
    }

    @ApiOperation("新增用户")
    @PostMapping
    public boolean save(@RequestBody User user) {
        return userService.save(user);
    }

    @ApiOperation("更新用户")
    @PutMapping
    public boolean update(@RequestBody User user) {
        return userService.updateById(user);
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/{id}")
    public boolean delete(@ApiParam("用户ID") @PathVariable Long id) {
        return userService.removeById(id);
    }

    @ApiOperation("批量更新用户状态")
    @PutMapping("/batch/status")
    public int batchUpdateStatus(@RequestBody List<Long> ids, @RequestParam Integer status) {
        return userService.batchUpdateStatus(ids, status);
    }

    @ApiOperation("检查用户名是否存在")
    @GetMapping("/exists/username/{username}")
    public boolean existsByUsername(@ApiParam("用户名") @PathVariable String username) {
        return userService.existsByUsername(username);
    }

    @ApiOperation("检查邮箱是否存在")
    @GetMapping("/exists/email/{email}")
    public boolean existsByEmail(@ApiParam("邮箱") @PathVariable String email) {
        return userService.existsByEmail(email);
    }

    @ApiOperation("检查手机号是否存在")
    @GetMapping("/exists/phone/{phone}")
    public boolean existsByPhone(@ApiParam("手机号") @PathVariable String phone) {
        return userService.existsByPhone(phone);
    }

    @ApiOperation("重置用户密码")
    @PutMapping("/{id}/password")
    public boolean resetPassword(@ApiParam("用户ID") @PathVariable Long id, 
                               @ApiParam("新密码") @RequestParam String newPassword) {
        return userService.resetPassword(id, newPassword);
    }
}

package com.geekzhang.mybatisplus;

import com.geekzhang.mybatisplus.entity.User;
import com.geekzhang.mybatisplus.service.UserService;
import com.geekzhang.mybatisplus.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * SpringBoot + MyBatis-Plus 应用测试类
 *
 * @author geekzhang
 * @since 2025-09-24
 */
@Slf4j
@SpringBootTest
public class MybatisPlusApplicationTest {

    @Autowired
    private UserService userService;

    @Test
    public void contextLoads() {
        log.info("SpringBoot + MyBatis-Plus 应用上下文加载测试通过");
    }

    @Test
    public void testUserService() {
        log.info("=== 用户服务测试开始 ===");
        
        // 测试查询所有用户
        List<User> allUsers = userService.list();
        log.info("查询所有用户数量：{}", allUsers.size());
        
        // 测试根据用户名查询
        User admin = userService.getByUsername("admin");
        if (admin != null) {
            log.info("根据用户名查询用户：{}", admin.getNickname());
        }
        
        // 测试查询启用状态的用户
        List<User> activeUsers = userService.getActiveUsers();
        log.info("启用状态用户数量：{}", activeUsers.size());
        
        // 测试用户名是否存在
        boolean exists = userService.existsByUsername("admin");
        log.info("用户名admin是否存在：{}", exists);
        
        log.info("=== 用户服务测试完成 ===");
    }

    @Test
    public void testJsonFileUtil() {
        log.info("=== JSON文件工具测试开始 ===");
        
        // 获取用户列表
        List<User> users = userService.list();
        
        // 测试写入JSON文件
        String filePath = "test-output/users.json";
        boolean success = JsonFileUtil.writeListToJsonWithJackson(users, filePath);
        log.info("写入JSON文件结果：{}", success);
        
        if (success) {
            // 测试读取JSON文件
            List<User> readUsers = JsonFileUtil.readListFromJsonWithJackson(filePath, User.class);
            log.info("从JSON文件读取用户数量：{}", readUsers.size());
        }
        
        log.info("=== JSON文件工具测试完成 ===");
    }

    @Test
    public void testUserCrud() {
        log.info("=== 用户CRUD测试开始 ===");
        
        // 创建测试用户
        User testUser = new User()
                .setUsername("testuser")
                .setPassword("123456")
                .setNickname("测试用户")
                .setEmail("testuser@example.com")
                .setPhone("13900000000")
                .setStatus(1);
        
        // 测试新增
        boolean saveResult = userService.save(testUser);
        log.info("新增用户结果：{}", saveResult);
        
        if (saveResult) {
            // 测试查询
            User savedUser = userService.getByUsername("testuser");
            log.info("查询新增用户：{}", savedUser != null ? savedUser.getNickname() : "未找到");
            
            if (savedUser != null) {
                // 测试更新
                savedUser.setNickname("更新后的测试用户");
                boolean updateResult = userService.updateById(savedUser);
                log.info("更新用户结果：{}", updateResult);
                
                // 测试删除
                boolean deleteResult = userService.removeById(savedUser.getId());
                log.info("删除用户结果：{}", deleteResult);
            }
        }
        
        log.info("=== 用户CRUD测试完成 ===");
    }


    @Test
    public void testUserSearch() {
        log.info("=== 用户搜索测试开始 ===");
        
        // 测试根据昵称搜索
        List<User> searchResults = userService.searchByNickname("用户");
        log.info("根据昵称'用户'搜索结果数量：{}", searchResults.size());
        
        // 测试邮箱查询
        User userByEmail = userService.getByEmail("admin@example.com");
        log.info("根据邮箱查询用户：{}", userByEmail != null ? userByEmail.getUsername() : "未找到");
        
        // 测试手机号查询
        User userByPhone = userService.getByPhone("13800000001");
        log.info("根据手机号查询用户：{}", userByPhone != null ? userByPhone.getUsername() : "未找到");
        
        log.info("=== 用户搜索测试完成 ===");
    }
}

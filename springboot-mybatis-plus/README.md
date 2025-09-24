# SpringBoot + MyBatis-Plus 集成模块

## 📖 项目简介

这是一个基于SpringBoot 2.7.18和MyBatis-Plus 3.5.5的完整集成示例项目，展示了现代Java Web开发的最佳实践。

## 🚀 技术栈

- **SpringBoot 2.7.18** - 主框架
- **MyBatis-Plus 3.5.5** - ORM框架
- **MySQL 8.0** - 数据库
- **Druid 1.2.20** - 数据库连接池
- **Swagger 3.0** - API文档
- **Lombok** - 代码简化
- **FastJSON2** - JSON处理
- **Hutool** - 工具类库

## 📁 项目结构

```
springboot-mybatis-plus/
├── src/main/java/com/geekzhang/mybatisplus/
│   ├── MybatisPlusApplication.java          # 主启动类
│   ├── config/                              # 配置类
│   │   ├── MybatisPlusConfig.java          # MyBatis-Plus配置
│   │   └── SwaggerConfig.java              # Swagger配置
│   ├── controller/                          # 控制器层
│   │   └── UserController.java             # 用户控制器
│   ├── entity/                             # 实体类
│   │   └── User.java                       # 用户实体
│   ├── mapper/                             # Mapper接口
│   │   └── UserMapper.java                 # 用户Mapper
│   ├── service/                            # 服务层
│   │   ├── UserService.java                # 用户服务接口
│   │   └── impl/
│   │       └── UserServiceImpl.java        # 用户服务实现
│   └── util/                               # 工具类
│       └── JsonFileUtil.java               # JSON文件工具
├── src/main/resources/
│   ├── application.yml                      # 应用配置
│   └── sql/
│       └── init.sql                        # 数据库初始化脚本
├── src/test/java/                          # 测试类
│   └── com/geekzhang/mybatisplus/
│       └── MybatisPlusApplicationTest.java # 应用测试
├── pom.xml                                 # Maven配置
└── README.md                               # 项目说明
```

## 🔧 快速开始

### 1. 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE test_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：
```bash
mysql -u root -p test_db < src/main/resources/sql/init.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test_db?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
    username: your_username
    password: your_password
```

### 4. 启动应用

```bash
# 进入项目目录
cd springboot-mybatis-plus

# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

### 5. 访问应用

- **应用首页**: http://localhost:8080
- **Swagger文档**: http://localhost:8080/swagger-ui/
- **Druid监控**: http://localhost:8080/druid/ (admin/123456)

## 📋 功能特性

### MyBatis-Plus特性

- ✅ **自动填充** - 创建时间、更新时间、创建人、更新人
- ✅ **逻辑删除** - 软删除支持
- ✅ **乐观锁** - 防止并发更新冲突
- ✅ **分页插件** - 物理分页
- ✅ **防全表更新删除** - 安全防护
- ✅ **代码生成器** - 快速生成基础代码

### 业务功能

- ✅ **用户管理** - 完整的CRUD操作
- ✅ **条件查询** - 多条件动态查询
- ✅ **批量操作** - 批量更新、删除
- ✅ **统计分析** - 用户状态统计
- ✅ **JSON导入导出** - 支持多种JSON库

### API接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/user/page` | GET | 分页查询用户 |
| `/api/user/{id}` | GET | 根据ID查询用户 |
| `/api/user/username/{username}` | GET | 根据用户名查询 |
| `/api/user/active` | GET | 查询启用用户 |
| `/api/user/search` | GET | 根据昵称搜索 |
| `/api/user/statistics/status` | GET | 用户状态统计 |
| `/api/user` | POST | 新增用户 |
| `/api/user` | PUT | 更新用户 |
| `/api/user/{id}` | DELETE | 删除用户 |

## 🧪 测试

### 运行单元测试

```bash
mvn test
```

### 测试覆盖

- ✅ 应用上下文加载测试
- ✅ 用户服务功能测试
- ✅ CRUD操作测试
- ✅ JSON文件工具测试
- ✅ 统计查询测试
- ✅ 搜索功能测试

## 📊 监控

### Druid数据源监控

访问 http://localhost:8080/druid/ 查看：

- SQL监控
- 连接池监控
- Web应用监控
- Spring监控

### 应用日志

日志文件位置：`logs/springboot-mybatis-plus.log`

## 🔨 开发指南

### 添加新实体

1. 创建实体类（参考`User.java`）
2. 创建Mapper接口（继承`BaseMapper`）
3. 创建Service接口（继承`IService`）
4. 创建Service实现类（继承`ServiceImpl`）
5. 创建Controller类

### 自定义查询

```java
// 使用LambdaQueryWrapper
LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
queryWrapper.eq(User::getStatus, 1)
           .like(User::getUsername, "admin")
           .orderByDesc(User::getCreateTime);
List<User> users = userService.list(queryWrapper);
```

### JSON文件操作

```java
// 写入JSON文件
List<User> users = userService.list();
JsonFileUtil.writeListToJsonWithJackson(users, "users.json");

// 读取JSON文件
List<User> users = JsonFileUtil.readListFromJsonWithJackson("users.json", User.class);
```

## 🐛 常见问题

### 1. 数据库连接失败

检查数据库连接配置和数据库服务状态。

### 2. Swagger访问404

确认Swagger配置正确，访问路径为 `/swagger-ui/`。

### 3. 自动填充不生效

检查实体类字段是否添加了`@TableField(fill = FieldFill.INSERT)`注解。

## 📝 更新日志

### v1.0.0 (2025-09-24)

- ✨ 初始版本发布
- ✨ 集成SpringBoot + MyBatis-Plus
- ✨ 完整的用户管理功能
- ✨ Swagger API文档
- ✨ Druid数据源监控
- ✨ JSON文件工具类
- ✨ 完整的单元测试

## 📄 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可证。

## 👥 贡献

欢迎提交Issue和Pull Request！

## 📞 联系方式

- 作者：geekzhang
- 邮箱：geekzhang@example.com
- GitHub：https://github.com/geekzhang

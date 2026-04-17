# java-coding 项目

个人学习与日常工具项目，Maven 多模块结构。

## Compact Instructions
<!-- 上下文压缩时保留此块 -->
- 项目路径：~/appstore/project/java-coding
- 多模块：algorithms（算法练习）/ worktest（工作工具脚本）/ springboot-camunda（流程引擎学习）/ springboot-mybatis-plus（ORM学习）
- 这是学习项目，代码可以不完整，优先清晰易懂而非生产级严谨
- skills/ 目录存放本项目的自定义 Claude skills
<!-- /Compact Instructions -->

## 模块说明

| 模块 | 用途 |
|------|------|
| `java-coding-algorithms` | 算法 & 数据结构练习 |
| `java-coding-worktest` | 日常工作用小工具脚本 |
| `springboot-camunda` | Camunda 流程引擎学习 demo |
| `springboot-mybatis-plus` | MyBatis-Plus ORM 学习 demo |
| `doc/` | 各技术方向学习笔记 |
| `skills/` | 本项目自定义 Claude skills |

## 开发原则
- 学习代码：优先注释清晰，不追求生产级严谨
- 工具脚本：能跑就行，关键逻辑加注释
- 新 demo 直接在对应模块下建包，不新建模块

## 常用命令
```bash
# 编译整个项目
mvn compile

# 运行某个模块
mvn spring-boot:run -pl springboot-camunda

# 数据对比工具
python3 compare_data.py
```

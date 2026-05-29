---
tags: [交接, 会话恢复, lot, yuncang, v3]
date: 2026-04-29
project: lot
branch:
task_doc: "[[arch-yuncang-unified-dispatching]]"
retrieval_triggers: [simpleDispatch, yuncang, v3, 下货, ES去依赖]
status: active
---

# 会话交接

## 一、从哪里继续

- **入口命令**：无（非 git 仓库，直接改代码）
- **待续位置**：`lot/open-yuncang-api/src/main/java/ai/yunji/rw/yuncang/openapi/service/impl/SimpleDispatchServiceImpl.java:86` — `dispatchLegacy` 方法
- **当前阶段**：v3 改造第三轮（ES 保留 + TODO 标注版本已完成，等待编译验证）

## 二、做了什么

- v2 `unifiedDispatching` 接口全量数据依赖梳理：5 类外部依赖（MySQL×2、ES×1、MQTT×1、HTTP×1）
- v3 `SimpleDispatchServiceImpl` 改造：UBOX storeId 改上游传入，Legacy 委托 YuncangTaskServiceImpl（保持 ES）
- ES 数据必要性分析：所有字段除 customer hierarchy 6 字段外均有 DB 替代，且层次字段不参与业务逻辑
- 创建 `templates/handoff.md` 卡帕西风格模板
- 创建 `projects/arch-yuncang-unified-dispatching.md` 架构文档
- 同步更新上述架构文档至 Obsidian

## 三、当前状态

| 维度 | 状态 |
|------|------|
| 编译 | 未编译（Maven 未安装） |
| 测试 | 未跑 |
| 阻塞 | 无 |
| 副作用 | `SimpleDispatchServiceImpl.java` 已改（去掉 YuncangInfoMapper/YuncangTaskMapper 直接依赖，新增 YuncangTaskService 依赖） |

## 四、关键文件

| 文件 | 角色 | 改动量 |
|------|------|--------|
| `lot/open-yuncang-api/.../SimpleDispatchServiceImpl.java` | 主改 | 重写 |
| `lot/open-yuncang-api/.../SimpleDispatchService.java` | 接口 | 未改 |
| `lot/open-yuncang-api/.../OpenYuncangApi.java` | Controller | 未改 |
| `java-coding/obsidian/templates/handoff.md` | 新增 | 新增 |
| `java-coding/obsidian/projects/arch-yuncang-unified-dispatching.md` | 文档 | 追加 v3 章节 |

## 五、关键决策

| 决策 | 选了什么 | 否决了什么 | 为什么 |
|------|---------|-----------|--------|
| v3 ES 去留 | 保留 ES + 标 TODO | 激进去掉 ES 改走 DB | ES 驱动的 isClose/status 拦截有实际业务效果，DB 兜底链未经充分验证 |
| Legacy 路径实现方式 | 委托 YuncangTaskServiceImpl | 自行实现 ES-free 版本 | 避免重复实现复杂状态机逻辑（双凯品牌、sell upsert、task_act） |
| storeId 来源 | UBOX 路径从上游传入 | 保留 DB 查询 | 调用方已知 storeId，减少一次 JOIN 查询 |

## 六、下一步

- [ ] 补 Maven 环境，`mvn compile -pl lot/open-yuncang-api -am` 验证编译
- [ ] 跑 `OpenDeviceOnlineStatusApiIT` 等已有集成测试确认不回归
- [ ] 写 SimpleDispatchServiceImpl 单元测试（注入 Mock YuncangTaskService）
- [ ] 按 TODO 顺序推进 ES → DB 迁移（先合并 3 次查询为 1 次）

## 七、遗留问题

| 问题 | 严重 | 谁来解决 |
|------|------|---------|
| `SimpleDispatchServiceImpl` 未编译验证 | 🟡 | 本会话 |
| YuncangTaskServiceImpl 中的 `insertTaskSell` 有潜在的 `ConcurrentModificationException`（enhanced-for 内 remove） | 🟡 | 后续 |

## 八、经验提取

| 经验 | 归档？ | 目标文件 |
|------|--------|---------|
| ES 数据依赖分析：区分"参与业务逻辑"和"仅报表填充"两类字段，决定去留 | O | [[arch-yuncang-unified-dispatching]] |
| 跨版本接口改造节奏：先分析依赖 → 分类必要性 → 渐进替代，避免一次性大改 | X | 已在架构文档中体现 |

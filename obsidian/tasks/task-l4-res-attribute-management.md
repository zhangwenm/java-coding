---
tags:
  - 任务
  - l4
  - res-atomization
  - attribute-management
date: 2026-06-12
status: done
priority: high
assignee: agent
created: 2026-06-12
updated: 2026-06-18
patch: S10-resource-binding-backend
deadline: null
branch: feature/attribute-management
retrieval_triggers:
  - 属性管理
  - 类型属性
  - 资源属性值
  - 完整度
  - attribute management
  - res-atomization
---

# L4 资源原子化 — 类型属性管理一期实现

## 目标

为 `l4-res-atomization` 实现静态属性管理闭环：支持一级/二级类型绑定属性、资源实例维护属性值、完整度计算与异步批量重算。

补充增强目标：支持资源实例本级属性绑定，使少量资源可以在不提升到二级类型的情况下追加差异化静态属性。

## 背景

架构方案见：[[arch-l4-res-device-type-attribute-management]]
目标项目路径：`~/appstore/project/L4/l4-res-atomization`
技术栈：NestJS 11 + TypeScript + Prisma 7 + PostgreSQL + Bull

## 范围

### 包含
- Prisma Schema 3 张新表 + `resource_profile` 新增字段
- 属性定义 CRUD（`AttributeDefinitionModule`）
- 属性继承合并工具函数（基于 `res_type_attribute_binding` 单表）
- 一级类型属性维护（`ParentTypeAttributeModule`）
- 二级类型属性维护（扩展 `resource-type` 模块）
- 资源实例属性值查询、普通更新、管理员强制更新
- 资源实例本级属性绑定维护（引用全局属性定义，不允许覆盖继承属性）
- 完整度实时计算 + 异步批量重算（Bull queue）
- 集成到资源创建、类型变更、列表接口
- **存量资源完整度一次性 backfill 脚本**（F022）
- **前端属性定义管理页**（F023，浏览/编辑/停用全局属性定义）

### 不包含
- Kafka、Redis Cluster、Elasticsearch
- realtime 属性（三期）
- 批量补录（二期）

## 里程碑

| 里程碑 | 项目 | 目标日期 | 实际完成 |
|--------|------|---------|---------|
| [后] Schema + Migration | l4-res-atomization | | |
| [后] 属性定义 + 合并工具 | l4-res-atomization | | |
| [后] 一级/二级类型属性接口 | l4-res-atomization | | |
| [后] 资源属性值 + 完整度 | l4-res-atomization | | |
| [后] 全量集成 + backfill | l4-res-atomization | | |
| [前] Service 层 + 定义管理页 | l4-res-admin | | 2026-06-13 |
| [前] 类型属性 Drawer | l4-res-admin | | 2026-06-13 |
| [前] 资源属性 Tab + 完整度角标 | l4-res-admin | | 2026-06-13 |
| [后] 资源本级属性绑定增强 | l4-res-atomization | | |
| [前] 资源本级属性绑定增强 | l4-res-admin | | |

## 开发操作指南

### 会话窗口规划

最多同时开 **2 个窗口**（后端 + 前端并行时），大部分时间只用 1 个。

| 会话      | 工作目录                 | 包含 Feature                 | 可以开始的条件                                       |
| ------- | -------------------- | -------------------------- | --------------------------------------------- |
| S1      | `l4-res-atomization` | F001                       | 随时                                            |
| S2      | `l4-res-atomization` | F002 + F003                | S1 完成                                         |
| S3      | `l4-res-atomization` | F004 + F005                | S2 完成                                         |
| S4 ‖ S3 | `l4-res-admin`       | F010 + F011 + F012 + F023  | S2 完成即可并行                                     |
| S5      | `l4-res-atomization` | F006 + F007                | S3 完成                                         |
| S6 ‖ S5 | `l4-res-admin`       | F013 + F014 + F015         | S3 完成即可并行                                     |
| S7      | `l4-res-atomization` | F008 + F009 + F022         | S5 完成                                         |
| S8 ‖ S7 | `l4-res-admin`       | F016 + F017 + F018         | S5 完成即可并行                                     |
| S9      | `l4-res-admin`       | F019 + F020 + F021         | S7 完成                                         |
| S10     | `l4-res-atomization` | F024 + F025 + F025A + F026 | F024–F026 方案已在任务文档中完整定义（含 F025A 冲突预检），可随时启动   |
| S11     | `l4-res-admin`       | F027 + F028 + F029         | S10 接口完成或 mock 契约稳定（F027/F028/F029 方案见下方前端章节） |

### 每次启动新会话的开场提示

**后端会话**（在 `l4-res-atomization` 目录启动）：

```
我在实现 task-l4-res-attribute-management 任务计划，后端项目 l4-res-atomization。
请先读：
- 架构方案：~/appstore/project/java-coding/obsidian/projects/arch-l4-res-device-type-attribute-management.md
- 任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md
S9已完成 当前实现 [F024 + F025 + F025A + F026]，请读任务计划对应章节后开始。
```

**前端会话**（在 `l4-res-admin` 目录启动）：

```
我在实现 task-l4-res-attribute-management 任务计划，前端项目 l4-res-admin。
请先读任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md
S10已完成 当前实现 [F027 + F028 + F029]，请读前端章节对应部分后开始。
```

### 会话管理规则

- 每个会话完成当前阶段全部 Feature 并通过回归验收后，执行 `/compact` 再继续下一会话
- 不要在一个会话里跨越两个阶段（如 S2 不做 F004）
- 遇到阻塞超过 30 分钟：记录到「阻塞项」表，切换到可并行的前端会话

---

## 执行计划

> **并行策略**：前端 Service 层（F010-F012）可在后端 F003 完成后立即开始（只需类型定义，无需真实接口）。F013 起需要后端 F004/F005 接口就绪。

### 后端（l4-res-atomization，`feature/attribute-management` 分支）

#### 阶段一：数据层
- [x] F001：Prisma Schema + Migration

**阶段一回归验收：**
```bash
npx tsc --noEmit                          # 类型检查通过
npx prisma migrate status                 # 迁移已应用
npm test                                  # 现有测试全量不回归（记录基线通过数）
```

---

#### 阶段二：属性定义与合并逻辑
- [x] F002：AttributeDefinitionModule
- [x] F003：属性继承合并工具函数（含单元测试）

**阶段二回归验收：**
```bash
npx tsc --noEmit
npm test -- --testPathPattern="attribute-merge|attribute-definition"   # F003 单元测试通过
npm test                                  # 全量不回归（与阶段一基线对比）
```

---

#### 阶段三：类型属性维护
- [x] F004：ParentTypeAttributeModule（一级类型属性）
- [x] F005：ResourceType 模块扩展（二级类型属性）

**阶段三回归验收：**
```bash
npx tsc --noEmit
npm test                                  # 全量不回归（F004/F005 新增逻辑不破坏现有模块）
# 重点核查：重启用父级绑定冲突检查、删类型两步校验
```

---

#### 阶段四：资源实例属性与完整度
- [x] F006：ResourceAttributeModule（实例属性值 + 完整度实时计算）
- [x] F007：CompletenessModule（Bull 异步批量重算）

**阶段四回归验收：**
```bash
npx tsc --noEmit
npm test -- --testPathPattern="resource-attribute|completeness"        # F006/F007 单元测试通过
npm test                                  # 全量不回归
```

---

#### 阶段五：接入与联调
- [x] F008：完整度 job 投递接入（解除 TODO 注释）
- [x] F009：集成到现有资源创建/变更/列表流程
- [x] F022：存量完整度 backfill 脚本

**阶段五回归验收（最严格，上线前必须全部通过）：**
```bash
npx tsc --noEmit
npm test                                  # 全量不回归，与阶段一基线对比，不允许有新失败
npx ts-node src/scripts/backfill-completeness.ts   # backfill 脚本可执行
# 验证 backfill 完成（等待 ~10 分钟后执行）：
# SELECT COUNT(*) FROM resource_profile WHERE profile_complete_flag IS NULL AND status != 'deleted';
# 结果应为 0
```

---

### 前端（l4-res-admin，`feature/attribute-management` 分支）

> 前端每个阶段开始前先执行 `yarn test` 记录基线通过数

#### 阶段六：API Service 层（依赖：后端 F003 完成即可开始）
- [x] F010：attributeDefinition.service.ts
- [x] F011：typeAttribute.service.ts
- [x] F012：resourceAttribute.service.ts
- [x] F023：AttributeDefinitionsPage（属性定义管理页）

**阶段六回归验收：**
```bash
tsc -b                                    # 类型检查通过
yarn test                                 # 全量不回归（Service 层无副作用，应与基线一致）
```

---

#### 阶段七：类型属性 Drawer（依赖：后端 F004/F005 接口就绪）
- [x] F013：TypeAttributeDrawer + OwnTab + BindingModal + QuickCreateModal
- [x] F014：TypeAttributeInheritedTab
- [x] F015：ResourceTypesPage 接入入口按钮

**阶段七回归验收：**
```bash
tsc -b
yarn test                                 # 全量不回归（F015 改动 ResourceTypesPage，需确认已有用例不回归）
```

---

#### 阶段八：资源实例属性 Tab（依赖：后端 F006 接口就绪）
- [x] F016：ResourceAttributeTab
- [x] F017：ResourceAttributeForceUpdateModal
- [x] F018：ResourceEditModal 接入属性 Tab

**阶段八回归验收（最关键，F018 改动高风险文件）：**
```bash
tsc -b
yarn test                                 # 必须全量通过，ResourceEditModal 相关用例重点检查
# 手动验证：打开资源编辑弹窗，确认基础信息 Tab 原有交互无异常
```

---

#### 阶段九：创建提示 + 列表完整度（依赖：后端 F009 集成完成）
- [x] F019：ResourceRegisterModal 创建后提示
- [x] F020：资源列表完整度角标
- [x] F021：ResourceEditModal 类型变更影响提示

**阶段九回归验收（前端收尾，完整回归）：**
```bash
tsc -b
yarn test                                 # 前端全量通过，与阶段六基线对比无新失败
yarn build                                # 生产构建通过，无 bundle 错误
```

#### 阶段十：资源本级属性绑定后端增强
- [x] F024：`res_type_attribute_binding.scope_type='resource'` 合并逻辑与冲突校验
- [x] F025：资源本级属性绑定接口（attribute-bindings CRUD）
- [x] F025A：资源类型变更冲突预检
- [x] F026：资源本级绑定变更接入完整度与审计

**阶段十回归验收：**
```bash
npx tsc --noEmit
npm test -- --testPathPattern="attribute-merge|resource-attribute|completeness"
npm test
```

#### 阶段十一：资源本级属性绑定前端增强
- [x] F027：resourceAttribute.service.ts 增加资源本级绑定 API 与类型定义
- [x] F028：ResourceAttributeTab 增加管理员可见的资源本级属性管理区
- [x] F029：资源本级绑定变更后的属性列表与完整度刷新

**阶段十一回归验收：**
```bash
tsc -b
yarn test
yarn build
```

## Feature 清单

| ID | Feature | 验收标准 | Status | Commit |
|----|---------|---------|--------|--------|
| F001 | Prisma Schema + Migration | `npx tsc --noEmit` 通过；迁移文件生成；Prisma client 包含新模型 | pending | |
| F002 | AttributeDefinitionModule | `npx tsc --noEmit` 通过；4 个接口可调用 | done | |
| F003 | 属性合并工具函数 | `npx tsc --noEmit` + 3 个单元测试（只parent/parent+subtype/冲突）通过 | done | |
| F004 | ParentTypeAttributeModule | `npx tsc --noEmit`；重启用冲突检查和必填变更 job 投递正确 | done | |
| F005 | 二级类型属性扩展 | `npx tsc --noEmit`；删类型时有资源引用返回 409；有活跃绑定返回 409 | done | |
| F006 | ResourceAttributeModule | `npx tsc --noEmit`；完整度计算单元测试通过 | done | |
| F007 | CompletenessModule | `npx tsc --noEmit`；processor 分批逻辑单元测试通过 | done | |
| F008 | Job 投递接入 | `npx tsc --noEmit`；完整度触发矩阵覆盖；无循环依赖 | done | |
| F009 | 资源流程集成 | `npx tsc --noEmit` + `npm test` 全量不回归 | done | |
| F022 | 存量完整度 backfill | script 可执行；存量资源 `profileCompleteFlag` 从 null 变为 true/false；`npx tsc --noEmit` 通过 | done | |
| F024 | 资源本级属性合并逻辑 | `sourceScope=resource` 合并在最后一组；与 parent/subtype 重复绑定返回 409；单元测试覆盖 | done | 12ac3ab |
| F025 | 资源本级属性绑定接口 | `GET/POST/PATCH/DELETE attribute-bindings` 可调用；只允许 static/enabled 定义；无权限写操作返回 403；不允许修改 attrId | done | 12ac3ab |
| F025A | 资源类型变更冲突预检 | 新类型 parent/subtype 与资源本级绑定冲突时返回 409；不更新 `resourceSubtypeId` | done | 12ac3ab |
| F026 | 资源本级绑定完整度与审计 | 新增/删除/启停/requiredFlag 变更影响必填时同步重算当前资源；绑定变更写审计 | done | 12ac3ab |

**Current**: 前后端全量完成（F001–F009 + F022 + F024–F026 + F027–F029）

## 当前状态快照
## 当前状态快照

- 最后更新：2026-06-18
- 当前进展：一期全量完成（F001–F009 + F022 + F024–F029）；二期全量完成（F030–F036）；属性默认值全量完成（F046–F054，后端 S19 + 前端 S20）
- 下次启动入口：三期（实时属性），工作目录 `l4-res-admin`
- 待续位置：三期执行计划 > 阶段十七

## 阻塞项

| 阻塞内容 | 等待对象 | 记录时间 | 解除时间 |
|---------|---------|---------|---------|
| | | | |

## 关键决策记录

| 决策 | 选项 | 结论 | 原因 |
|------|------|------|------|
| 完整度异步队列 | 新建 vs 复用现有 | 新建 CompletenessModule | discovery 模式一致，职责清晰 |
| 属性合并冲突处理 | 静默跳过 vs 抛错 | 抛 AppException 409 | 冲突代表数据旁路写入或程序 Bug，必须暴露 |
| 旧类型属性值删除 | 删除 vs 保留为孤儿值 | 保留孤儿值 | 避免误删类型配置导致历史数据永久丢失 |
| 实例层 editable_flag | 随类型层自动刷新 vs 独立维护 | 独立维护 | 实例层代表管理员显式锁定状态，不自动漂移 |
| 资源本级属性 | 独立表 vs 复用绑定表 | 复用 `res_type_attribute_binding(scope_type='resource')` | 字段和一级/二级绑定一致，减少新表和重复逻辑 |
| 资源本级属性语义 | 覆盖继承属性 vs 追加差异属性 | 只追加，不覆盖 | 避免同一资源同一 attrId 多套配置导致完整度和权限歧义 |

## 改动项目

| 项目 | 路径 | 改动内容 |
|------|------|---------|
| l4-res-atomization | `prisma/schema.prisma` | 新增 3 张表 + 1 字段 |
| l4-res-atomization | `src/modules/attribute-definition/` | 新建模块 |
| l4-res-atomization | `src/modules/attribute/` | 新建合并工具 |
| l4-res-atomization | `src/modules/parent-type-attribute/` | 新建模块 |
| l4-res-atomization | `src/modules/resource-type/` | 扩展二级属性 service + controller |
| l4-res-atomization | `src/modules/resource-attribute/` | 新建模块 |
| l4-res-atomization | `src/modules/resource-attribute/` | F025 增加资源本级属性绑定接口 |
| l4-res-atomization | `src/modules/completeness/` | 新建模块 |
| l4-res-atomization | `src/modules/resource/resource.service.ts` | 集成属性初始化 + 类型变更 + 列表扩展 |
| l4-res-atomization | `src/app.module.ts` | 注册新模块 |
| l4-res-atomization | `src/scripts/backfill-completeness.ts` | 新建 backfill 脚本 |

## 涉及文件（关键）

- `prisma/schema.prisma` — 新增 `ResAttributeDefinition` / `ResTypeAttributeBinding` / `ResResourceAttributeValue` 模型，`ResourceProfile` 追加 `profileCompleteFlag`
- `src/modules/attribute/attribute-merge.util.ts` — 核心合并逻辑，多处复用
- `src/modules/resource/resource.service.ts` — 资源创建事务内初始化属性、类型变更差异合并

## 验收标准

- [ ] 每个 Feature 的 `npx tsc --noEmit` 通过
- [ ] F003、F006、F007 单元测试通过
- [ ] F009 `npm test` 全量不回归
- [ ] 属性合并冲突（重复绑定）返回 409；合并查询使用 `res_type_attribute_binding.scopeType` 区分 parent/subtype
- [ ] 资源本级属性合并在 parent/subtype 之后；重复绑定 parent/subtype 已有 attrId 返回 409
- [ ] 资源本级绑定新增/删除/启停/requiredFlag 变化后，只重算当前资源完整度
- [ ] 资源类型变更前校验新 parent/subtype 与资源本级绑定冲突；冲突时返回 409，不更新 `resourceSubtypeId`
- [ ] 删类型：先校验资源引用（有则 409），再校验活跃属性绑定（有则 409）
- [ ] 重启用父级绑定时子类型冲突检查正确（查 `scopeType='subtype'` 的活跃绑定）
- [ ] 管理员强制更新通过 `AuditService.recordEntityChange` 写入 `operation_audit_log`（同一事务，不新建审计表）
- [ ] `profileCompleteFlag = null` 不被当作 false 处理；筛选 `profileComplete=false` 只返回明确 false 的资源
- [ ] F022 backfill script 执行后存量资源 `profileCompleteFlag` 不再为 null（过渡期前提条件）

## 各 Feature 详细说明

### F001 — Prisma Schema + Migration

`prisma/schema.prisma` 修改（**禁止使用数据库外键，所有关联均为应用层维护**；主键统一 `String @id @default(cuid())`）：

1. `ResourceProfile` 追加：
   ```prisma
   profileCompleteFlag Boolean? @map("profile_complete_flag")
   ```

2. 新增 `ResAttributeDefinition`：
   ```prisma
   model ResAttributeDefinition {
     id            String   @id @default(cuid())
     attrCode      String   @unique @map("attr_code") @db.VarChar(64)
     attrName      String   @map("attr_name") @db.VarChar(128)
     dataCategory  String   @map("data_category") @db.VarChar(16)
     dataType      String   @map("data_type") @db.VarChar(16)
     unit          String?  @db.VarChar(32)
     enumOptions   Json?    @map("enum_options")
     requiredFlag  Boolean  @default(false) @map("required_flag")
     editableFlag  Boolean  @default(true)  @map("editable_flag")
     statisticFlag Boolean  @default(false) @map("statistic_flag")
     enabledFlag   Boolean  @default(true)  @map("enabled_flag")
     remark        String?  @db.VarChar(256)
     createdAt     DateTime @default(now()) @map("created_at")
     updatedAt     DateTime @updatedAt @map("updated_at")

     @@map("res_attribute_definition")
   }
   ```

3. 新增 `ResTypeAttributeBinding`（原两张绑定表合并为一张）：

   > **注意**：`scopeType` 从建表起即支持三种值：`'parent'`（一级类型）、`'subtype'`（二级类型）、`'resource'`（资源实例，F024 实现应用层逻辑）。**禁止在 DTO 中添加 `@IsIn(['parent','subtype'])` 枚举校验**，否则 F024 上线时需额外改动校验层。

   ```prisma
   model ResTypeAttributeBinding {
     id            String   @id @default(cuid())
     scopeType     String   @map("scope_type") @db.VarChar(16)   // 'parent' | 'subtype' | 'resource'（F024 实现 resource 应用逻辑）
     scopeId       String   @map("scope_id") @db.VarChar(128)    // parent→parentType枚举；subtype→typeId；resource→resourceId
     attrId        String   @map("attr_id") @db.VarChar(128)     // 应用层关联，无 DB 外键
     requiredFlag  Boolean  @default(false) @map("required_flag")
     editableFlag  Boolean  @default(true)  @map("editable_flag")
     statisticFlag Boolean  @default(false) @map("statistic_flag")
     sortNo        Int      @default(0) @map("sort_no")
     enabledFlag   Boolean  @default(true)  @map("enabled_flag")
     createdAt     DateTime @default(now()) @map("created_at")
     updatedAt     DateTime @updatedAt @map("updated_at")

     @@unique([scopeType, scopeId, attrId])
     @@index([scopeType, scopeId, enabledFlag])
     @@map("res_type_attribute_binding")
   }
   ```

4. 新增 `ResResourceAttributeValue`（一期不含冗余统计字段）：
   ```prisma
   model ResResourceAttributeValue {
     id           String   @id @default(cuid())
     resourceId   String   @map("resource_id") @db.VarChar(128)  // 应用层关联，无 DB 外键
     attrId       String   @map("attr_id") @db.VarChar(128)      // 应用层关联，无 DB 外键
     attrValue    String?  @map("attr_value")
     editableFlag Boolean  @default(true) @map("editable_flag")
     valueSource  String   @map("value_source") @db.VarChar(16)
     updatedBy    String?  @map("updated_by") @db.VarChar(128)
     createdAt    DateTime @default(now()) @map("created_at")
     updatedAt    DateTime @updatedAt @map("updated_at")

     @@unique([resourceId, attrId])
     @@index([resourceId])
     @@map("res_resource_attribute_value")
   }
   ```

执行：`npm run prisma:migrate:dev -- --name add_attribute_management`

---

### F002 — AttributeDefinitionModule

新建 `src/modules/attribute-definition/`，接口：
```
GET    /internal/attribute-definitions?keyword=&dataCategory=&enabledFlag=
POST   /internal/attribute-definitions
GET    /internal/attribute-definitions/:attrId
PATCH  /internal/attribute-definitions/:attrId
```
GET 过滤参数：`keyword`（`attrName`/`attrCode` 模糊匹配，供前端 Select 搜索使用）、`dataCategory`（`static`/`realtime`）、`enabledFlag`（`true`/`false`；不传返回全部）。

约束：`data_category` 不可修改；有属性值记录时拒绝修改 `data_type`；无 DELETE 接口；一期允许创建 `dataCategory='realtime'` 的定义，但 POST `/attributes` 绑定接口拒绝绑定 realtime 定义（返回 400）。
注册到 `app.module.ts`。

---

### F003 — 属性合并工具函数

新建 `src/modules/attribute/attribute-merge.util.ts`

```typescript
export interface MergedAttrItem {
  relationId: string;
  attrId: string;
  attrCode: string;
  attrName: string;
  dataCategory: string;
  dataType: string;
  unit: string | null;
  enumOptions: string[] | null;
  required: boolean;
  editable: boolean;
  statistic: boolean;
  sourceScope: 'parent' | 'subtype' | 'resource';  // 已预扩展，F024 只需加 resource 查询逻辑
  sourceName: string;
}

export async function mergeTypeAttributes(
  parentType: string,
  resourceSubtypeId: string | null,
  prisma: PrismaService,
  resourceId?: string | null,  // F024 新增，可选；F003 阶段传 undefined/null，F006 调用处无需修改
): Promise<MergedAttrItem[]>
```

逻辑：
1. 查 `resTypeAttributeBinding JOIN resAttributeDefinition`（`scopeType='parent'`、两表 `enabledFlag=true`、`dataCategory='static'`），按 `sortNo` 升序
2. `resourceSubtypeId` 非空时查 `resTypeAttributeBinding JOIN` 定义表（`scopeType='subtype'`），同条件
3. ownAttrs 中有 attrId 已在 parentAttrs → `logger.error` + `throw AppException('ATTR_BINDING_CONFLICT', ..., 409)`
4. 返回 `[...parentAttrs, ...ownAttrs]`（F024 追加第三组 resource scope）

sourceName 映射规则：
- `sourceScope='parent'`：枚举映射 `Device→机器人, IOT→IOT, Human→人员, Amenities→客需品, Goods→商品`
- `sourceScope='subtype'`：从 `resource_type_option.name` 读取（合并查询时 JOIN 该表取 name 字段）
- `sourceScope='resource'`：从 `resource_profile.name` 读取（F024 实现）

单元测试 3 个 case：只 parent / parent+subtype / 重复 attrId 冲突

> **F024 改动范围**：`mergeTypeAttributes` 增加可选参数 `resourceId?: string | null`，添加第三组 resource scope 查询与冲突检测；`sourceScope` 类型已预扩展，F024 只加查询逻辑，无需改类型。**F006 调用处传参不变**（`resourceId` 为可选，省略即可）。

---

### F004 — ParentTypeAttributeModule

新建 `src/modules/parent-type-attribute/`，接口：
```
GET    /internal/resource-parent-types/:parentType/attributes
POST   /internal/resource-parent-types/:parentType/attributes
PATCH  /internal/resource-parent-types/:parentType/attributes/:relationId
DELETE /internal/resource-parent-types/:parentType/attributes/:relationId
```

GET：默认只返回 `b.enabledFlag=true AND d.enabledFlag=true AND d.dataCategory='static'` 的属性，与合并算法保持一致；三期实时属性接入后再通过 `?category=realtime` 扩展。

POST 校验：`parentType` 合法枚举；attr 定义 `dataCategory='static'` + `enabledFlag=true`；同一 scope 不可重复绑定；从定义复制默认 flag。

**POST 反向冲突检查（新增，与 PATCH re-enable 复用同一逻辑）：**
抽取公共方法 `AttributeBindingConflictChecker.checkSubtypeConflict(parentType, attrId, prisma)`：
```sql
SELECT scope_id FROM res_type_attribute_binding
WHERE scope_type='subtype' AND attr_id=? AND enabled_flag=true
  AND scope_id IN (SELECT type_id FROM resource_type_option WHERE parent_type=?)
```
有结果 → 409，返回冲突子类型名称列表（`"以下子类型已独立绑定该属性，请先解除再绑定到一级类型：[typeA, typeB]"`）。
POST 和 PATCH re-enable 均调用此方法，不重复写 SQL。

PATCH 重启用冲突检查（`enabled: false→true`）：调用 `checkSubtypeConflict`，有结果 → 409。

PATCH `enabledFlag: true→false`（软禁用）：若 `requiredFlag=true`，事务提交后投递 `{ scope: 'parentType', id: parentType }` completeness job（影响该 parentType 下所有资源）。
DELETE：`$transaction`（删绑定 + 写 audit `entityType='typeAttributeBinding'`）；`requiredFlag=true` 时投递 completeness job。

---

### F005 — 二级类型属性扩展

在 `src/modules/resource-type/` 新增 `resource-type-attribute.service.ts`，controller 新增路由：
```
GET    /internal/resource-types/:typeId/attributes
GET    /internal/resource-types/:typeId/own-attributes
GET    /internal/resource-types/:typeId/inherited-attributes
POST   /internal/resource-types/:typeId/attributes
PATCH  /internal/resource-types/:typeId/attributes/:relationId
DELETE /internal/resource-types/:typeId/attributes/:relationId
```

POST：attr 定义必须 `dataCategory='static'` + `enabledFlag=true`；查父绑定活跃状态，有则 409（"已通过一级类型继承"）；同一 subtype scope 不可重复绑定。
PATCH：禁止修改 `attrId`；只允许修改 `requiredFlag`、`editableFlag`、`statisticFlag`、`sortNo`、`enabledFlag`。
- `enabledFlag: true→false`（软禁用），若该绑定 `requiredFlag=true`：事务提交后投递 completeness job（与 DELETE 相同，禁用后该属性消失于合并结果，必填约束解除，完整度需重算）。
- `requiredFlag` 变更：事务提交后投递 completeness job（原有逻辑）。
DELETE：`$transaction`（删绑定 + 写 audit `entityType='typeAttributeBinding'`）；若删除必填 static 属性，事务提交后投递 completeness job。

`DELETE /resource-types/:typeId` 联动（两步顺序校验）：
1. 先查 `resource_profile`：有资源 `resource_subtype_id = typeId AND status != 'deleted'` → 409「该类型下仍有 N 个资源，请先变更」
2. 再查活跃绑定（`scopeType='subtype'` `enabledFlag=true`）→ 409「已绑定 N 个属性，请先删除」
两项均通过 → 级联删除该 subtype 的所有绑定记录（同事务）。

---

### F006 — ResourceAttributeModule

新建 `src/modules/resource-attribute/`，接口：
```
GET    /internal/resources/:resourceId/attributes
PATCH  /internal/resources/:resourceId/attributes/:attrId
PATCH  /internal/resources/:resourceId/attributes/:attrId/force
GET    /internal/resources/:resourceId/profile-completeness
```

GET：`mergeTypeAttributes` → LEFT JOIN 属性值 → 孤儿值过滤 → editable = 类型层 AND 实例层（无记录只看类型层）。`resourceSubtypeId=null` 时按 `resource_profile.type` 映射一级类型；非法一级类型返回空列表、完整度视为 true，并记录 warning。

PATCH（upsert）：校验 attrId 在集合内；权限检查；写入前 trim；必填属性拒绝 null/空串/纯空白；INSERT 时从类型绑定层复制 `editableFlag`；UPDATE 不修改 `editableFlag`；同步重算完整度。

PATCH /force：管理员 Guard；body 必须有 reason；`$transaction`（upsert + `auditService.recordEntityChange`）；`valueSource='force'`。

GET /profile-completeness：实时计算（不读持久化字段），返回 `{ profileComplete, missingRequiredAttributeCount, missingAttributes }`；`missingAttributes` 在 true 时为空数组，异常 unknown 时为 null。

内部函数 `computeAndUpdateCompleteness(resourceId, prismaOrTx)`：可接受 tx，写回 `resource_profile.profile_complete_flag`。

---

### F007 — CompletenessModule

新建 `src/modules/completeness/`，参照 `src/modules/discovery/` 实现：
- `COMPLETENESS_QUEUE = 'completeness'`
- `CompletenessService.enqueueRecompute({ scope: 'subtype'|'parentType', id: string })`
- Processor：分批 200 条重算；`scope='parentType'` 两步查询（先查二级类型 IDs，再查资源含无 subtype 的历史资源）
- 注册到 `app.module.ts`，exports `CompletenessService`

---

### F008 — Job 投递接入

覆盖所有完整度触发点并投递或同步重算（完整触发矩阵）：

| 触发场景 | 投递方式 |
|---|---|
| 一级/二级新增必填属性（POST） | 异步 job |
| 一级/二级**软禁用**必填属性绑定（PATCH `enabledFlag: true→false` 且 `requiredFlag=true`） | 异步 job |
| 一级/二级删除必填属性（DELETE） | 异步 job |
| 一级/二级修改 `requiredFlag` | 异步 job |
| 一级类型属性绑定重新启用（PATCH `enabledFlag: false→true`，且 `requiredFlag=true`） | 异步 job |
| 二级类型属性绑定重新启用（PATCH `enabledFlag: false→true`，且 `requiredFlag=true`） | 异步 job |
| 资源类型变更 | 同步重算（单台，代价低） |
| 单条属性值更新 | 同步重算 |
| 资源创建 | 同步重算（同一事务） |

在 F004、F005 中留的 TODO 注释处注入 `CompletenessService` 并替换为实际调用。
循环依赖优先通过模块边界规避；确有需要再使用 `forwardRef(() => CompletenessModule)`。

---

### F009 — 资源流程集成

`resource.service.ts` 改动：

**资源创建**（同一 `$transaction`）：
1. `mergeTypeAttributes` 取定义
2. 过滤 required=true + dataCategory='static'
3. `tx.resResourceAttributeValue.createMany({ skipDuplicates: true })` — `attrValue:null, valueSource:'system', editableFlag` 取类型绑定层
4. `computeAndUpdateCompleteness(resourceId, tx)`

**类型变更**（同一 `$transaction`，三步顺序执行）：

> **F025A 前置冲突预检说明**：F025A 负责在类型变更前校验新类型继承属性与资源本级绑定的冲突，一期无资源本级绑定（F024/F025 尚未实现），此处不做该校验。F025A 须在 F025 完成后、S10 内一并实现，实现前不得上线 resource scope 绑定功能。

1. 批量 UPDATE editable_flag（newAttrs 全集；F024 完成后 newAttrs 含三组，目前只含 parent+subtype）
2. INSERT 新类型必填属性缺失空值记录（skipDuplicates: true，`editableFlag` 取新类型绑定层）
3. UPDATE resource_profile.resource_subtype_id

事务提交后同步重算完整度。

**列表接口**：响应增加 `profileComplete / missingRequiredAttributeCount`；支持 `?profileComplete=true|false` 筛选；`false` 只匹配明确 false，`null` 不归入未完善。

---

### F022 — 存量完整度 backfill

**背景**：F001–F009 上线后存量资源的 `profileCompleteFlag` 全为 null（初始值）。前端 F020 列表角标
和筛选依赖该字段，需要在展示角标前完成一次全量重算。

**新建文件：** `src/scripts/backfill-completeness.ts`

**实现分两步**：

**步骤一：对 5 个已知 parentType 各投递一个 Bull job**（processor 分批处理，覆盖所有有效 type 资源）

```typescript
const PARENT_TYPES = ['Device', 'IOT', 'Human', 'Amenities', 'Goods'] as const;
for (const id of PARENT_TYPES) {
  await queue.add('recompute', { scope: 'parentType', id }, { jobId: `parentType:${id}` });
}
```

**步骤二：直接 Prisma updateMany 处理未知 type 资源**

> **必须实现**：`type NOT IN (...)` 的历史资源不会被任何 Bull job 覆盖。`completeness-calc.util.ts` 对未知 type 返回 `profileComplete: true`（无必填属性），backfill 应直接 DB 写入而非经队列。

```typescript
const prisma = new PrismaClient();
try {
  const { count } = await prisma.resourceProfile.updateMany({
    where: {
      type: { notIn: [...PARENT_TYPES] },
      profileCompleteFlag: null,
      isDeleted: false,
    },
    data: { profileCompleteFlag: true },
  });
  console.log(`[backfill-completeness] unknown-type resources set to true: ${count}`);
} finally {
  await prisma.$disconnect();
}
```

步骤二在步骤一 job 投递完成后、`queue.close()` 之前执行，同一 main() 函数内串行完成。

processor 已有分批 200 条处理逻辑，全量 backfill 不会导致单次事务过长。

**执行方式**：上线流程第 2 步由运维手动执行一次：

```bash
ENV_FILE=.env.dev npm run backfill:completeness
```

**与上线流程的对应关系**（架构文档「完整度计算 > 上线流程」4 步）：

| 步骤 | 由谁完成 |
|------|---------|
| 1. 后端上线字段和重算任务，前端不展示角标 | F001 + F007 |
| **2. 运行全量 backfill** | **F022** |
| 3. backfill 完成后前端展示角标 | F020（依赖本步） |
| 4. 过渡期 null 不展示角标 | F009 + F020 已处理 |

注册到 `package.json` scripts：`"backfill:completeness": "ts-node src/scripts/backfill-completeness.ts"`

**验证 backfill 完成**（前端展示角标的前提条件）：

```sql
-- 执行后 count 应为 0 才能上线角标
-- 注意：用 is_deleted（布尔字段），不是 status；不加 type IN 限制（覆盖全部资源）
SELECT COUNT(*) FROM resource_profile
WHERE profile_complete_flag IS NULL
  AND is_deleted = false;
```

Bull queue 处理有延迟，建议步骤一投递 job 后等待约 10 分钟再执行上述 SQL 检查。若 count 不为 0，检查 Bull 失败队列后重新投递；步骤二（未知 type）应在脚本执行完成后立即生效，若仍有 null 说明有非 `isDeleted=false` 条件外的记录，需排查数据。

---

### F024 — 资源本级属性合并逻辑

**目标**：扩展现有 `res_type_attribute_binding` 单表模型，支持 `scope_type='resource'`，资源最终属性集合变为：

```text
一级类型属性 + 二级类型本级属性 + 资源本级属性
```

无 `resourceSubtypeId` 的历史资源：

```text
resource_profile.type 对应一级类型属性 + 资源本级属性
```

**改动点：**

- `attribute-merge.util.ts` 的 `mergeTypeAttributes` 增加可选参数 `resourceId?: string | null`（**不是新的重载，直接在现有函数末尾加可选参数**）。F006 中已有的调用点省略该参数即可，**无需修改 F006 代码**。
- `resourceId` 有值时查询 `scope_type='resource' AND scope_id=resourceId` 的活跃绑定，追加为第三组。
- 合并排序固定为三组：`parent` → `subtype` → `resource`，组内按 `sort_no` 升序。
- `MergedAttrItem.sourceScope` 已在 F003 代码中预扩展为 `'parent' | 'subtype' | 'resource'`，F024 **只加查询逻辑，不改类型声明**。
- 资源本级绑定与已合并属性（parent+subtype）重复时抛 409，不静默跳过。

**测试：**

- 只 parent + resource。
- parent + subtype + resource。
- resource 绑定重复 parent attrId 返回 409。
- resource 绑定重复 subtype attrId 返回 409。

---

### F025 — 资源本级属性绑定接口

**目标**：在 `ResourceAttributeModule` 或资源属性 controller 中增加资源本级绑定维护接口。

接口：

```text
GET    /internal/resources/{resourceId}/attribute-bindings
POST   /internal/resources/{resourceId}/attribute-bindings
PATCH  /internal/resources/{resourceId}/attribute-bindings/{relationId}
DELETE /internal/resources/{resourceId}/attribute-bindings/{relationId}
```

规则：

- `GET /attribute-bindings` 只返回 `scope_type='resource'` 的绑定，`sourceScope='resource'`；返回绑定配置，不返回 `attrValue/valueSource/updatedBy`。
- `POST` 只允许绑定 `data_category='static'` 且 `enabled_flag=true` 的属性定义。
- `POST` 必须校验该 `attrId` 不在当前资源最终属性集合中。
- `POST` 如遇同一 `scopeType/scopeId/attrId` 已存在 disabled 绑定，返回 409，提示调用 `PATCH enabledFlag=true` 重新启用。
- `PATCH` 不允许修改 `attrId`；只允许修改 `requiredFlag`、`editableFlag`、`statisticFlag`、`sortNo`、`enabledFlag`。
- `PATCH enabledFlag=true` 前必须重新校验该 `attrId` 未被当前一级/二级类型继承；若已继承，返回 409。
- `DELETE` 只删除绑定关系，不删除属性定义，不删除 `res_resource_attribute_value` 记录。
- `POST/PATCH/DELETE` 必须做后端权限校验；普通用户无权限返回 403。
- 建议增加单资源本级绑定数量上限，默认 20；超过返回 400。

**测试：**

- 新增资源本级绑定成功。
- 绑定停用属性定义返回 400。
- 绑定 realtime 属性定义返回 400。
- 重复绑定继承属性返回 409。
- disabled 绑定重复 POST 返回 409，PATCH 重新启用成功。
- PATCH 重新启用时若已与新类型继承属性冲突，返回 409。
- PATCH 携带 `attrId` 返回 400。
- 普通用户 POST/PATCH/DELETE 返回 403。

---

### F025A — 资源类型变更冲突预检

**目标**：补齐资源本级属性与新二级类型继承属性的冲突处理，避免类型变更成功后资源属性合并失败。

改动点：

- 在资源 `resourceSubtypeId` 变更事务内，更新前计算 `resourceOwnAttrIds ∩ newInheritedAttrIds`。
- `newInheritedAttrIds` 只包含新 parent/subtype 属性，不包含 resource scope。
- 若交集非空，返回 409，不更新 `resource_profile.resource_subtype_id`。
- 错误文案包含冲突属性名称，提示先删除或停用资源本级绑定。

测试：

- 无冲突时类型变更成功。
- 资源本级属性与新 parent 属性冲突时返回 409。
- 资源本级属性与新 subtype 属性冲突时返回 409。
- 冲突时 `resourceSubtypeId` 不变，属性值不被修改。

---

### F026 — 资源本级绑定完整度与审计

**目标**：资源本级绑定变更只影响当前资源，避免误投 parent/subtype 批量任务。

完整度触发：

| 场景 | 处理 |
|---|---|
| 新增资源本级必填属性 | 同步重算当前资源 |
| 删除资源本级必填属性 | 同步重算当前资源 |
| `requiredFlag` 变化 | 同步重算当前资源 |
| `enabledFlag: false→true` 且 required=true | 同步重算当前资源 |
| `enabledFlag: true→false` 且 required=true | 同步重算当前资源 |

审计：

- 资源本级绑定新增、修改、删除写 `operation_audit_log`。
- `entityType` 建议仍用 `typeAttributeBinding` 或新增 `resourceAttributeBinding`，实现前统一后端已有审计命名。
- 绑定写入、审计写入、`profileCompleteFlag` 更新必须在同一 Prisma transaction 内完成。资源本级绑定只影响单个资源，允许同事务同步重算，避免读到旧绑定或出现审计成功但完整度失败。

验收：

```bash
npx tsc --noEmit
npm test -- --testPathPattern="attribute-merge|resource-attribute|completeness"
npm test
```

## 进度日志

---

## 二期执行计划

> 依赖：一期全量完成（F001–F029）且 F022 backfill 已验证通过（`profile_complete_flag IS NULL` 数量为 0）。

### 目标

- 支持类型新增必填属性后为已有资源异步补录空值记录（当前一期只重算完整度，不写 value 记录）
- 资源列表支持按完整度筛选（`profileComplete=true|false`）
- 管理员可批量补录某属性在多个资源上的值
- 类型属性统计 Tab 落地（填充率、缺失数等）

### 范围

**包含**
- F030：类型新增必填属性时触发空值记录异步补录（后端）
- F031：资源列表 `profileComplete` 筛选参数（后端）
- F032：批量补录属性值接口（后端）
- F033：静态属性统计接口（后端）
- F034：资源列表完整度筛选 UI（前端）
- F035：批量补录 UI（前端）
- F036：类型属性统计 Tab（前端）

**不包含**
- Elasticsearch 接入（按架构方案，数据量大时再评估）
- realtime 属性（三期）

### 会话规划

| 会话        | 工作目录                 | Feature     | 可以开始的条件    |
| --------- | -------------------- | ----------- | ---------- |
| S12       | `l4-res-atomization` | F030 + F031 | 一期全量验收通过   |
| S13       | `l4-res-atomization` | F032 + F033 | S12 完成     |
| S14 ‖ S12 | `l4-res-admin`       | F034        | S12 完成即可并行 |
| S15 ‖ S13 | `l4-res-admin`       | F035        | S13 完成即可并行 |
| S16 ‖ S13 | `l4-res-admin`       | F036        | S13 完成即可并行 |

### 开场提示模板

**后端会话（S12/S13）：**
```
我在实现 task-l4-res-attribute-management 任务计划二期，后端项目 l4-res-atomization。
请先读：
- 架构方案：~/appstore/project/java-coding/obsidian/projects/arch-l4-res-device-type-attribute-management.md
- 任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md
S13已完成。当前实现二期 [F034]，请读任务计划「二期执行计划」章节后开始。
```

**前端会话（S14–S16）：**
```
我在实现 task-l4-res-attribute-management 任务计划二期，前端项目 l4-res-admin。
请先读任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md
后端 S12/S13 以及S15 ‖ S13已完成。当前实现 [F036]，请读二期前端章节后开始。
```

---

### 后端（l4-res-atomization）

#### 阶段十二：补录 + 筛选

- [x] F030：类型新增必填属性时触发空值记录异步补录
- [x] F031：资源列表 `profileComplete` 筛选参数

**回归验收：**
```bash
npx tsc --noEmit
npm test -- --testPathPattern="attribute-backfill|resource-attribute|completeness"
npm test
```

---

#### 阶段十三：批量补录 + 统计

- [x] F032：批量补录属性值接口
- [x] F033：静态属性统计接口

**回归验收：**
```bash
npx tsc --noEmit
npm test
```

---

### 前端（l4-res-admin）

#### 阶段十四：资源列表完整度筛选（依赖 S12 F031）

- [x] F034：资源列表完整度筛选 UI

**回归验收：**
```bash
tsc -b
yarn test
```

---

#### 阶段十五：批量补录 UI（依赖 S13 F032）

- [x] F035：批量补录 UI（`AttributeBulkFillModal`）

**回归验收：**
```bash
tsc -b
yarn test
yarn build
```

---

#### 阶段十六：统计 Tab（依赖 S13 F033）

- [x] F036：类型属性统计 Tab（`TypeAttributeStatisticsTab`，当前 disabled）

**回归验收：**
```bash
tsc -b
yarn test
yarn build
```

---

### 二期 Feature 清单

| ID | Feature | 验收标准 | Status | Commit |
|----|---------|---------|--------|--------|
| F030 | 类型新增必填属性异步补录空值记录 | 新增 required 绑定后受影响资源均有对应 `res_resource_attribute_value` 记录（`attr_value=null`）；`tsc --noEmit` 通过；单元测试补充 | done | |
| F031 | 资源列表 `profileComplete` 筛选 | `?profileComplete=false` 只返回明确 false 的资源，不包含 null；`tsc --noEmit` 通过 | done | |
| F032 | 批量补录属性值接口 | 批量 upsert 成功；权限校验（非管理员 403）；attrId 不在资源属性集合返回 400；`tsc --noEmit` 通过 | done | |
| F033 | 静态属性统计接口 | 返回 `totalResources/filledCount/missingCount/fillRate`；`tsc --noEmit` 通过 | done | |
| F034 | 资源列表完整度筛选 UI | `tsc -b` 通过；筛选"未完善"只包含明确 false；不选时不传参 | done | |
| F035 | 批量补录 UI | `tsc -b` + `yarn build` 通过；管理员可批量填写并提交；成功后刷新列表 | done | |
| F036 | 类型属性统计 Tab | `tsc -b` + `yarn build` 通过；Tab 从 disabled 变为可用；填充率展示正确 | done | |

---

### 各 Feature 详细说明（二期）

#### F030 — 类型新增必填属性时触发空值记录异步补录

**问题背景**：一期 POST 新增 required 属性绑定到类型时，只投递了 completeness Bull job 重算完整度，但受影响资源不会自动获得对应的 `res_resource_attribute_value` 记录（`attr_value=null`）。导致用户打开属性 Tab 时，该属性按"无记录补空"规则展示，但每次都是即时计算，无法在 DB 层过滤/统计。

**实现**：以下三个场景均需投递补录 job（对应 completeness job 的同等触发时机）：

| 触发场景 | 触发文件 |
|---|---|
| POST 新增必填属性绑定（`requiredFlag=true`） | `parent-type-attribute.service.ts` / `resource-type-attribute.service.ts` |
| PATCH `requiredFlag: false→true`（改为必填） | 同上 |
| PATCH `enabledFlag: false→true` 且 `requiredFlag=true`（重新启用必填绑定） | 同上 |

在上述场景的 completeness job 投递之后，再投递一个补录 job：

新建 `AttributeBackfillModule`，queue 名称 `attribute-backfill`，job payload：
```typescript
{ scope: 'parentType' | 'subtype'; id: string; attrId: string; editableFlag: boolean }
```

Processor 逻辑：
1. 按 scope 查出受影响资源集合（复用 completeness processor 的查询逻辑）
2. 分批（200 条/批）执行：
   ```typescript
   await tx.resResourceAttributeValue.createMany({
     data: resourceIds.map(resourceId => ({
       resourceId, attrId, attrValue: null,
       editableFlag, valueSource: 'system', updatedBy: null,
     })),
     skipDuplicates: true,
   })
   ```

只补录 `requiredFlag=true` 的属性；非必填属性不触发，永久按需补空。

注意：现有资源类型变更（F009）在 `createMany({ skipDuplicates: true })` 步骤已经写入新类型必填属性空值记录，无需补录。F030 只针对"类型已存在资源后新增必填属性"场景。

---

#### F031 — 资源列表 `profileComplete` 筛选参数

**后端改动**：`src/modules/resource/resource.service.ts` 的 `getList` 方法：

1. `GetResourcesQueryDto` 增加 `profileComplete?: boolean`
2. Prisma `where` 条件：
   ```typescript
   ...(profileComplete !== undefined && {
     profileCompleteFlag: profileComplete  // true→true, false→false（不加 null）
   })
   ```
   严格等值匹配，不使用 `profileCompleteFlag: { not: true }` 等写法，避免把 null 纳入 false 筛选。

---

#### F032 — 批量补录属性值接口

```
POST /internal/resources/bulk/attribute-values
```

RequestBody：
```typescript
{
  attrId: string;
  items: Array<{ resourceId: string; attrValue: string | null }>;
}
```

规则：
- 仅管理员可调用（AdminGuard）；普通用户 403
- `items` 数量上限 500，超过返回 400
- 服务端对每个 `resourceId` 校验 `attrId` 是否在其最终属性集合内（不在则跳过并记录到 `errors` 列表）
- 忽略类型层 `editable_flag` 限制（批量补录是管理员操作）；忽略实例层 `editable_flag`（同强制更新语义）
- 批量 upsert：`value_source='import'`，`updated_by`=当前操作人
- 全部写入后，统一投递 completeness job（对每个涉及的 `resource_subtype_id` 或 `parentType` 投一个 job，不逐条同步重算）
- **审计**：每条成功 upsert 的记录写 `operation_audit_log`（`entityType='resourceAttribute'`、`action='bulkImport'`），与 upsert 在同一事务内；绕过 `editable_flag` 属于管理员操作，必须留审计痕迹
- 响应返回 `{ successCount, errors: Array<{ resourceId, reason }> }`

---

#### F033 — 静态属性统计接口

```
GET /internal/resource-parent-types/:parentType/attribute-statistics
GET /internal/resource-types/:typeId/attribute-statistics
```

响应（数组，每项对应一个属性）：
```typescript
{
  attrId: string;
  attrCode: string;
  attrName: string;
  totalResources: number;    // 该类型下活跃资源总数
  filledCount: number;       // attr_value IS NOT NULL AND TRIM(attr_value) != '' 的记录数
  missingCount: number;      // totalResources - filledCount（含无记录的资源）
  fillRate: number;          // filledCount / totalResources，保留两位小数
}
```

实现：应用层计算（一期数据量不大，不接 ES）：
1. 查该类型下活跃资源 IDs
2. 查类型最终属性集合（合并视图）
3. 查 `res_resource_attribute_value` 中该 attrId 有值的 resourceId 集合
4. 做差集得出 missingCount

边界：`totalResources = 0` 时 `fillRate` 返回 `null`（前端展示"—"），不返回 NaN 或 0。

---

#### F034 — 资源列表完整度筛选 UI

**改动文件**：`src/pages/resources/index.tsx`

在现有筛选栏末尾增加"资料完整度"筛选项（`Select`，宽度与其他筛选项一致）：

```
全部  /  完整  /  未完善
```

筛选值映射：
- 全部 → 不传 `profileComplete` 参数
- 完整 → `profileComplete=true`
- 未完善 → `profileComplete=false`

`ResourceListQuery` 增加 `profileComplete?: boolean`。

注意：下拉选项不提供"未计算（null）"选项，过渡期数据自然过滤。

---

#### F035 — 批量补录 UI

**新建文件**：`src/pages/resource-types/AttributeBulkFillModal.tsx`

入口：`TypeAttributeOwnTab` 的属性行操作区增加"批量补录"按钮（管理员可见）。

Modal 内容：
- 顶部说明：当前补录属性名称 + 数据类型
- 表格：展示该类型下所有资源（分页 20 条/页），列为 `资源名称 / 资源 ID / 当前值 / 新值（可编辑）`
- **资源列表加载**：
  - `scope='parent'` → `GET /internal/resources?type={parentType}&pageSize=20&page={current}`
  - `scope='subtype'` → `GET /internal/resources?resourceSubtypeId={typeId}&pageSize=20&page={current}`
- **当前值加载**：Modal 打开后对当前页所有 resourceId 逐条调 `GET /resources/{resourceId}/attributes` 取当前 attrId 的值；已知 N+1，一期数据量可接受，二期不引入新接口
- "新值"列根据 `dataType` 渲染对应控件（与 `ResourceAttributeTab` 行内控件保持一致）
- 底部：`[取消] [提交补录]`（只提交"新值"有改动的行，`attrValue` 未变动的行不发送）
- 提交调用 `POST /internal/resources/bulk/attribute-values`
- 响应成功后展示 `successCount` / 失败记录（若有）
- 同 `resourceAttribute.service.ts` 新增 `bulkFillAttributeValues(dto)` 方法

**新增 service 方法**（`resourceAttribute.service.ts`）：
```typescript
bulkFillAttributeValues(dto: {
  attrId: string;
  items: Array<{ resourceId: string; attrValue: string | null }>;
}): Promise<{ successCount: number; errors: Array<{ resourceId: string; reason: string }> }>
```

---

#### F036 — 类型属性统计 Tab

**新建文件**：`src/pages/resource-types/TypeAttributeStatisticsTab.tsx`

**改动文件**：`src/pages/resource-types/TypeAttributeDrawer.tsx`（统计 Tab 当前为 `disabled: true, children: null`，改为 `disabled: false` + 引入 `TypeAttributeStatisticsTab`；无需删文案）

`TypeAttributeStatisticsTab` Props：
```typescript
interface Props {
  scope: TypeAttributeScope;
}
```

内容：
- 调用 `GET .../attribute-statistics` 接口（`typeAttribute.service.ts` 新增 `getParentTypeAttributeStatistics` / `getTypeAttributeStatistics`）
- 表格展示：`attrName / attrCode / totalResources / filledCount / missingCount / 填充率（进度条或百分比）`
- 按 `fillRate` 升序排列（填充最差的排前面）

**新增 service 方法**（`typeAttribute.service.ts`）：
```typescript
getParentTypeAttributeStatistics(parentType: string): Promise<AttributeStatItem[]>
getTypeAttributeStatistics(typeId: string): Promise<AttributeStatItem[]>
```

---

## 属性默认值执行计划

> 依赖：二期全量完成（F030–F036）。
>
> **设计说明**：Pure Lazy 策略——默认值永不写 DB，仅在 `isAttributeFilled()` 中运行时 fallback。`defaultFillsRequired` 控制默认值是否满足必填完整度计算。详见架构方案「属性默认值」章节。

### 目标

- `res_attribute_definition` 支持 `default_value`（录入时填写，对所有绑定该属性的资源全局即时生效）
- `default_fills_required` 控制默认值是否算作完整度满足（物理测量属性设 false；全局配置类属性设 true）
- `GET /resources/{id}/attributes` 响应携带 `defaultValue`、`defaultFillsRequired`，前端展示灰色占位

### 范围

**包含**
- F046：Prisma migration（`default_value`、`default_fills_required`）
- F047：属性定义接口扩展 + 枚举项删除冲突检查
- F048：提取 `isAttributeFilled()` 工具函数
- F049：同步完整度路径重构（含 `mergeTypeAttributes` 返回值扩展）
- F050：Bull processor 补 JOIN + 改用 `isAttributeFilled()`
- F051：新增完整度触发场景（默认值变更影响完整度时投递 Bull job）
- F052：`GET /attributes` 响应字段扩展
- F053：前端属性定义表单扩展（`defaultValue` + `defaultFillsRequired`）
- F054：`ResourceAttributeTab` 三态展示 + 未修改不发请求

**不包含**
- 绑定层差异默认值（只在定义层维护，对所有资源全局一致）
- `default_value` 写入 DB（Pure Lazy，默认值运行时 fallback 不持久化）

### 会话规划

| 会话 | 工作目录 | Feature | 可以开始的条件 |
|------|---------|---------|--------------|
| S19 | `l4-res-atomization` | F046 + F047 + F048 + F049 + F050 + F051 + F052 | 二期全量完成 |
| S20 ‖ S19 | `l4-res-admin` | F053 + F054 | S19 F052 接口完成（或契约稳定后 mock 先行） |

### 开场提示模板

**后端会话（S19）：**
```
我在实现 task-l4-res-attribute-management 属性默认值功能，后端项目 l4-res-atomization。
请先读：
- 架构方案：~/appstore/project/java-coding/obsidian/projects/arch-l4-res-device-type-attribute-management.md（重点看「属性默认值」章节）
- 任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md（属性默认值执行计划章节）
当前实现 [F046 + F047 + F048 + F049 + F050 + F051 + F052]，请读对应章节后开始。
```

**前端会话（S20）：**
```
我在实现 task-l4-res-attribute-management 属性默认值功能，前端项目 l4-res-admin。
请先读任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md
后端 S19 已完成。当前实现 [F053 + F054]，请读属性默认值前端章节后开始。
```

---

### 后端（l4-res-atomization）

#### 阶段十九：默认值基础设施 + 完整度重构

- [ ] F046：Prisma migration（`default_value`、`default_fills_required`）
- [ ] F047：属性定义接口扩展 + 枚举项删除冲突检查
- [ ] F048：提取 `isAttributeFilled()` 工具函数
- [ ] F049：同步完整度路径重构
- [ ] F050：Bull processor 补 JOIN + 改用 `isAttributeFilled()`
- [ ] F051：新增完整度触发场景
- [ ] F052：`GET /attributes` 响应字段扩展

**回归验收：**
```bash
npx tsc --noEmit
npm test -- --testPathPattern="attribute-merge|resource-attribute|completeness|completeness-calc"
npm test
```

---

### 前端（l4-res-admin）

#### 阶段二十：属性默认值 UI

- [ ] F053：属性定义表单扩展（`defaultValue` + `defaultFillsRequired`）
- [ ] F054：`ResourceAttributeTab` 三态展示 + 未修改不发请求

**回归验收：**
```bash
tsc -b
yarn test
yarn build
```

---

### 属性默认值 Feature 清单
### 属性默认值 Feature 清单

| ID | Feature | 验收标准 | Status | Commit |
|----|---------|---------|--------|--------|
| F046 | Prisma migration `default_value` + `default_fills_required` | 迁移文件生成；`npx prisma migrate status` 通过；Prisma client 含两个新字段 | done | S19 |
| F047 | 属性定义接口扩展 + 枚举项删除冲突检查 | POST/PATCH 接收 `defaultValue`/`defaultFillsRequired`；按 `data_type` 格式校验；删除枚举项前检查 `definition.default_value` 冲突；`tsc --noEmit` 通过 | done | S19 |
| F048 | 提取 `isAttributeFilled()` | 函数在 `completeness-calc.util.ts`；签名 `(attrValue, definitionDefaultValue, defaultFillsRequired) → boolean`；单元测试覆盖 6 个边界 case | done | S19 |
| F049 | 同步完整度路径重构 | 4 个同步触发点均改用 `isAttributeFilled()`；`MergedAttrItem` 新增 `definitionDefaultValue`、`defaultFillsRequired` 字段；`tsc --noEmit` 通过 | done | S19 |
| F050 | Bull processor 重构 | processor JOIN `res_attribute_definition` 取 `default_value` 和 `default_fills_required`；改用 `isAttributeFilled()`；`tsc --noEmit` 通过 | done | S19（由 F049 自动覆盖，无额外改动） |
| F051 | 新增完整度触发场景 | `PATCH default_value`（跨有值/null 边界且 `default_fills_required=true`）→ 投递 Bull job；`PATCH default_fills_required` 变更 → 投递该属性所有绑定 scope 的 Bull job；`tsc --noEmit` 通过 | done | S19 |
| F052 | GET /attributes 响应字段扩展 | 每个属性行含 `defaultValue: string \| null`、`defaultFillsRequired: boolean`；`tsc --noEmit` 通过 | done | S19 |
| F053 | 前端属性定义表单扩展 | `AttributeDefinitionQuickCreateModal` + 属性定义管理页均支持 `defaultValue` 输入和 `defaultFillsRequired` checkbox；`enum` 类型限 Select 控件选枚举项；`tsc -b` 通过 | done | S20 |
| F054 | ResourceAttributeTab 三态展示 + 未修改不发请求 | 无值时展示默认值（灰色占位，可见）；无值且无默认时展示「—」；有值时正常展示；未修改时点保存不发 PATCH 请求；`tsc -b` 通过 | done | S20 |

---

### 各 Feature 详细说明（属性默认值）

#### F046 — Prisma migration

`prisma/schema.prisma` 中 `ResAttributeDefinition` 新增两个字段（仅定义层，`ResTypeAttributeBinding` 不改动）：

```prisma
defaultValue         String?  @map("default_value")
defaultFillsRequired Boolean  @default(false) @map("default_fills_required")
```

执行：`npm run prisma:migrate:dev -- --name add_attribute_default_value`

---

#### F047 — 属性定义接口扩展 + 枚举项删除冲突检查

**`POST /internal/attribute-definitions` 和 `PATCH /internal/attribute-definitions/:attrId` 扩展：**

新增可选字段：`defaultValue?: string | null`、`defaultFillsRequired?: boolean`

格式校验（按 `data_type`，`defaultValue` 非空时执行）：
- `string` → 不限制
- `number` → `isNaN(Number(v))` 为 true 时返回 400
- `boolean` → 不为 `'true'` / `'false'` 时返回 400
- `date` → `dayjs(v).isValid()` 为 false 时返回 400
- `enum` → 不在 `enumOptions` 列表中时返回 400
- `json` → `JSON.parse(v)` 抛出时返回 400

**枚举项删除冲突检查（`PATCH enumOptions` 删除枚举项时）：**

若 `definition.default_value` 等于被删除的枚举项之一，返回 400：
```
"该属性的默认值「XXX」是被删除的枚举项之一，请先修改默认值再删除枚举项。"
```

---

#### F048 — 提取 `isAttributeFilled()` 工具函数

**新建（或追加到现有工具文件）：** `src/modules/completeness/completeness-calc.util.ts`

```typescript
export function isAttributeFilled(
  attrValue: string | null | undefined,
  definitionDefaultValue: string | null,
  defaultFillsRequired: boolean,
): boolean {
  if (attrValue != null && attrValue.trim() !== '') return true;
  if (definitionDefaultValue != null && defaultFillsRequired) return true;
  return false;
}
```

单元测试 6 个 case：
1. `attrValue='foo'`，任意默认值 → true
2. `attrValue=''`，无默认 → false
3. `attrValue=null`，有默认 + `defaultFillsRequired=true` → true
4. `attrValue=null`，有默认 + `defaultFillsRequired=false` → false
5. `attrValue=null`，无默认 → false
6. `attrValue='  '`（纯空白），有默认 + `defaultFillsRequired=true` → true

---

#### F049 — 同步完整度路径重构

**改动文件：** `src/modules/attribute/attribute-merge.util.ts`

`MergedAttrItem` 新增字段：
```typescript
definitionDefaultValue: string | null;
defaultFillsRequired: boolean;
```

`mergeTypeAttributes` 查询时 JOIN `res_attribute_definition`，取 `default_value` 和 `default_fills_required`，填充到每项 `MergedAttrItem`。

**同步触发点（4 处），均改用 `isAttributeFilled()`：**

| 触发文件 | 场景 |
|---------|------|
| `resource-attribute.service.ts` `computeAndUpdateCompleteness` | 单条属性值更新后重算 |
| `resource.service.ts` 资源创建事务内 `computeAndUpdateCompleteness` | 创建时初始化完整度 |
| `resource.service.ts` 类型变更后 `computeAndUpdateCompleteness` | 类型变更后重算 |
| `resource-attribute.service.ts` F026 资源本级绑定变更场景 | 资源本级绑定操作后重算 |

每处用 `isAttributeFilled(item.attrValue, item.definitionDefaultValue, item.defaultFillsRequired)` 替换原有 `item.attrValue != null && item.attrValue.trim() !== ''` 判断。

---

#### F050 — Bull processor 重构

**改动文件：** `src/modules/completeness/completeness.processor.ts`

批量查询资源属性值时同步 JOIN `res_attribute_definition`，取 `default_value`、`default_fills_required`。计算每条属性是否填充时改用 `isAttributeFilled()`（从 F048 导入）。

---

#### F051 — 新增完整度触发场景

**改动文件：** `src/modules/attribute-definition/attribute-definition.service.ts`

在 `PATCH` 方法执行完字段更新后，检查以下两种情况并投递 Bull job：

1. **`default_value` 变更** 且当前 `default_fills_required=true`：
   - 旧值 null → 新值非 null（从无到有）
   - 旧值非 null → 新值 null（从有到无）
   - 上述两种情况均需按绑定层反查该 `attrId` 的所有 scope 并逐一投递

2. **`default_fills_required` 变更**（`false→true` 或 `true→false`）：
   - 查 `res_type_attribute_binding` 找到所有绑定该 `attrId` 的 scope，逐一投递 Bull job

> `default_fills_required` 变更影响面最大，可加简单防抖（相同 attrId 3 秒内合并为一次投递）。

---

#### F052 — GET /attributes 响应字段扩展

**改动文件：** `src/modules/resource-attribute/resource-attribute.service.ts`

`GET /internal/resources/{resourceId}/attributes` 响应 DTO 每项新增：
```typescript
defaultValue: string | null;      // 来自 definition.default_value
defaultFillsRequired: boolean;    // 来自 definition.default_fills_required
```

`mergeTypeAttributes` 已在 F049 中将两字段带出，此处直接透传即可。

---

#### F053 — 前端属性定义表单扩展

**改动文件：**
- `src/pages/resource-types/AttributeDefinitionQuickCreateModal.tsx`
- `src/pages/attribute-definitions/AttributeDefinitionsPage.tsx`（属性定义管理页编辑表单）

新增表单字段：

- **`defaultValue`** 输入框：根据 `dataType` 渲染对应控件（与 `ResourceAttributeTab` 控件类型保持一致）；`enum` 类型用 Select，选项来自 `enumOptions`
- **`defaultFillsRequired`** Checkbox：标签文案"默认值满足必填（适用于可选型全局配置属性）"

`updateAttributeDefinition` / `createAttributeDefinition` 请求体透传两字段。

验收：`tsc -b` 通过；`enum` 类型可选默认枚举项；删除该枚举项时后端返回 400，前端展示错误文案。

---

#### F054 — ResourceAttributeTab 三态展示 + 未修改不发请求

**改动文件：** `src/pages/resources/ResourceAttributeTab.tsx`

**三态展示逻辑（每行属性）：**

| 状态 | 条件 | 展示 |
|------|------|------|
| 有值 | `attrValue != null && attrValue.trim() !== ''` | 正常展示 `attrValue` |
| 无值有默认 | `attrValue` 空，且 `defaultValue != null` | 灰色 Placeholder 样式展示 `defaultValue`（标注"默认"，不视为已填写） |
| 无值无默认 | `attrValue` 空，且 `defaultValue == null` | 展示"—" |

**未修改不发请求：**

每行独立编辑时，仅当用户实际修改了值（新值 ≠ 原 `attrValue`）才在点击保存时发送 PATCH 请求；用户开启编辑后未修改直接点保存，静默跳过（不发请求，不报错）。

**`ResourceAttrItem` 类型扩展（`resourceAttribute.service.ts`）：**
```typescript
defaultValue?: string | null;
defaultFillsRequired?: boolean;
```

---

## 三期执行计划

> **前置条件：** 二期全量完成（F030–F036）。
>
> **阻塞说明：** 三期基础设施（F037–F041）可独立完成并用手动写入测试数据验证。上报链路（F042-TODO）需 IoT 侧对齐后才能完成端到端真实数据流。F042-TODO 未完成前不做生产上线。

### 目标

- 建设实时属性状态表并扩展合并算法，使 `GET /resources/{id}/attributes` 返回实时属性组
- 类型绑定放开 realtime 限制，绑定 UI 支持实时属性配置
- 资源属性 Tab 展示实时数据（只读，带上报时间）
- 为上报链路预留 TODO 占位，待 IoT 对齐后实现

### 范围

**包含（可立即实现）**
- F037：`res_resource_realtime_state` Prisma migration
- F038：`mergeRealtimeAttributes` 函数 + `GET /attributes` 扩展
- F039：解除类型绑定 realtime 限制（`parent` / `subtype` scope 放开；`resource` scope 保持 static-only）
- F040：前端绑定 UI 调整（`AttributeDefinitionBindingModal` + `AttributeDefinitionQuickCreateModal`）
- F041：前端 `ResourceAttributeTab` 实时属性展示

**延后（待 TODO 对齐）**
- F042-TODO：实时上报链路（TODO-1，需 IoT 侧确认接入方式）
- F043-TODO：`value_number` / `value_json` 冗余字段写入逻辑（TODO-3）
- F044-TODO：数据时效性处理（TODO-4，过期阈值 + 前端"N 分钟前上报"）
- F045-TODO：Kafka / Redis 引入判断标准（TODO-6，QPS 阈值 + 延迟基线）

### 会话规划

| 会话 | 工作目录 | Feature | 可以开始的条件 |
|------|---------|---------|--------------|
| S17 | `l4-res-atomization` | F037 + F038 + F039 | 二期全量完成 |
| S18 ‖ S17 | `l4-res-admin` | F040 + F041 | S17 完成（或接口契约稳定后 mock 先行） |
| S-upload | `l4-res-atomization` | F042-TODO | IoT 侧上报链路对齐后启动 |

### 开场提示模板

**后端会话（S17）：**
```
我在实现 task-l4-res-attribute-management 任务计划三期，后端项目 l4-res-atomization。
请先读：
- 架构方案：~/appstore/project/java-coding/obsidian/projects/arch-l4-res-device-type-attribute-management.md（重点看「三期：实时属性 > 三期已设计部分」章节）
- 任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md（三期执行计划章节）
二期全量完成。当前实现 [F037 + F038 + F039]，请读对应章节后开始。
```

**前端会话（S18）：**
```
我在实现 task-l4-res-attribute-management 任务计划三期，前端项目 l4-res-admin。
请先读任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md
后端 S17 已完成。当前实现 [F040 + F041]，请读三期前端章节后开始。
```

---

### 后端（l4-res-atomization）

#### 阶段十七：实时属性基础设施

- [ ] F037：`res_resource_realtime_state` Prisma migration
- [ ] F038：`mergeRealtimeAttributes` + `GET /attributes` 扩展
- [ ] F039：解除类型绑定 realtime 限制

**回归验收：**
```bash
npx tsc --noEmit
npx prisma migrate status
npm test -- --testPathPattern="attribute-merge|resource-attribute"
npm test
# 手动验证：向 res_resource_realtime_state 写入测试数据，
# 调用 GET /resources/{id}/attributes 确认响应末尾含 dataCategory='realtime' 行
```

---

### 前端（l4-res-admin）

#### 阶段十八：绑定 UI + 实时属性展示

- [ ] F040：绑定 UI 调整
- [ ] F041：`ResourceAttributeTab` 实时属性展示

**回归验收：**
```bash
tsc -b
yarn test
yarn build
# 手动验证：类型绑定弹窗可选 realtime 属性定义；
#          选中 realtime 时 requiredFlag/editableFlag 隐藏；
#          资源属性 Tab 实时行只读，reportTime=null 时显示"暂无数据"
```

---

### 三期 Feature 清单

| ID | Feature | 验收标准 | Status | Commit |
|----|---------|---------|--------|--------|
| F037 | `res_resource_realtime_state` migration | 迁移文件生成；`npx prisma migrate status` 通过；`tsc --noEmit` 通过 | pending | |
| F038 | `mergeRealtimeAttributes` + GET /attributes 扩展 | realtime 属性追加在静态组之后；`reportTime` 字段存在；无 realtime 绑定时返回空组；`tsc --noEmit` + 单元测试通过 | pending | |
| F039 | 解除类型绑定 realtime 限制 | parent/subtype scope POST 允许 realtime 定义；resource scope POST 仍返回 400；`tsc --noEmit` 通过 | pending | |
| F040 | 前端绑定 UI 调整 | `tsc -b` 通过；选 realtime 时隐藏 requiredFlag/editableFlag；QuickCreateModal dataCategory 可选；ResourceAttributeBindingModal 不变 | pending | |
| F041 | ResourceAttributeTab 实时属性展示 | `tsc -b` + `yarn build` 通过；realtime 行只读；reportTime 展示；无"强制更新"入口 | pending | |
| F042-TODO | 实时上报链路 | 待 TODO-1 对齐后补充验收标准 | blocked | |
| F043-TODO | value_number/value_json 写入 | 待 TODO-3 对齐后补充 | blocked | |
| F044-TODO | 数据时效性 | 待 TODO-4 对齐后补充 | blocked | |
| F045-TODO | Kafka/Redis 引入判断 | 待 TODO-6 对齐后补充 | blocked | |

---

### 各 Feature 详细说明（三期）

#### F037 — `res_resource_realtime_state` Prisma migration

在 `prisma/schema.prisma` 新增模型（架构方案已定义，直接落 schema）：

```prisma
model ResResourceRealtimeState {
  id          String   @id @default(cuid())
  resourceId  String   @map("resource_id") @db.VarChar(128)
  attrId      String   @map("attr_id") @db.VarChar(128)
  attrValue   String?  @map("attr_value")
  valueNumber Decimal? @map("value_number")
  valueJson   Json?    @map("value_json")
  reportTime  DateTime @map("report_time")
  createdAt   DateTime @default(now()) @map("created_at")
  updatedAt   DateTime @updatedAt @map("updated_at")

  @@unique([resourceId, attrId])
  @@index([resourceId])
  @@map("res_resource_realtime_state")
}
```

执行：`npm run prisma:migrate:dev -- --name add_realtime_state`

> **注意**：`reportTime` 字段非空（无 `?`），写入记录时必须携带设备上报时间戳，不允许为空；`attrValue`、`valueNumber`、`valueJson` 均可空。

---

#### F038 — `mergeRealtimeAttributes` + `GET /attributes` 扩展

**新增函数**（`src/modules/attribute/attribute-merge.util.ts`）：

见架构方案「合并算法扩展」章节，完整接口签名与查询逻辑已定义。

关键约束：
- `required` 恒 `false`，`editable` 恒 `false`
- `sourceScope` 只支持 `'parent' | 'subtype'`，无 `'resource'`
- `resourceSubtypeId=null` 时只查 parent scope 的 realtime 绑定

**改动**（`src/modules/resource-attribute/resource-attribute.service.ts`）：

```typescript
// GET /resources/{resourceId}/attributes service 方法末尾追加
const realtimeAttrs = await mergeRealtimeAttributes(parentType, resourceSubtypeId, resourceId, this.prisma);
return [...staticAttrs, ...realtimeAttrs];
```

响应 DTO 新增字段 `reportTime?: string | null`；**静态属性行统一返回 `reportTime: null`，不省略该字段**，避免前端 `undefined` vs `null` 差异引发判断问题。

> **参数顺序说明**：`mergeRealtimeAttributes(parentType, resourceSubtypeId, resourceId, prisma)` 中 `prisma` 在第 4 位，与 `mergeTypeAttributes(parentType, resourceSubtypeId, prisma, resourceId?)` 顺序不同。这是为了保持现有 `mergeTypeAttributes` 签名不变，实现时在函数定义处加注释说明，避免后续维护混淆。

**单元测试补充（3 个 case）：**
- 有 realtime 绑定且有上报值 → 返回含 `attrValue` 和 `reportTime`
- 有 realtime 绑定但无上报值 → 返回 `attrValue=null`、`reportTime=null`
- 无 realtime 绑定 → realtime 组为空数组

---

#### F039 — 解除类型绑定 realtime 限制

**改动文件：**
- `src/modules/parent-type-attribute/parent-type-attribute.service.ts`
- `src/modules/resource-type/resource-type-attribute.service.ts`

删除或改写现有校验：

```typescript
// 删除：
if (def.dataCategory !== 'static') {
  throw new AppException('ATTR_BINDING_INVALID_CATEGORY', '一期只允许绑定 static 属性', 400);
}
```

`resource-attribute.service.ts`（资源本级绑定 POST）**保持不变**，维持 static-only 校验。

**测试：**
- `parent` scope POST realtime 定义成功（原 static-only 限制已移除）。
- `subtype` scope POST realtime 定义成功。
- `resource` scope POST realtime 定义返回 400（static-only 校验保持不变）。
- `parent`/`subtype` scope POST `enabled_flag=false` 的定义仍返回 400（`enabled_flag` 校验不受影响）。

---

#### F040 — 前端绑定 UI 调整

**改动文件：**
- `src/pages/resource-types/AttributeDefinitionBindingModal.tsx`
- `src/pages/resource-types/AttributeDefinitionQuickCreateModal.tsx`

**`AttributeDefinitionBindingModal` 改动：**
- 搜索 Select 去掉 `dataCategory: 'static' as const` 过滤，改为不传（展示所有 `enabledFlag=true` 的定义）
- 选中定义后读取其 `dataCategory`，若为 `'realtime'`：
  - 隐藏 `requiredFlag` 字段
  - 隐藏 `editableFlag` 字段
  - 表单底部展示提示："实时属性由设备自动上报，不支持人工编辑，也不参与资料完整度计算。"

**`AttributeDefinitionQuickCreateModal` 改动：**
- `dataCategory` 从 `hidden` 固定 `'static'` 改为可见 `Select`（选项：静态属性 / 实时属性）
- 选中 `'realtime'` 时联动隐藏 `requiredFlag` 和 `editableFlag` 表单项
- `dataCategory` 初始值保持 `'static'`（不改变默认行为）

**`ResourceAttributeBindingModal` 不动。**

---

#### F041 — `ResourceAttributeTab` 实时属性展示

**改动文件：** `src/pages/resources/ResourceAttributeTab.tsx`

三期接口响应末尾会追加 `dataCategory='realtime'` 的行，`ResourceAttributeTab` 需要：

- `dataCategory='realtime'` 的行：渲染只读展示控件（不可编辑），不展示"编辑/保存"按钮
- 展示 `reportTime`：
  - 有值 → 转为相对时间（如"3 分钟前"）或格式化绝对时间
  - `null` → 展示"暂无数据"
- 不展示"强制更新"图标（`editable=false` 且 `dataCategory='realtime'`）
- 在静态属性组与实时属性组之间插入分组标题或分隔线（如"实时属性"小标题），提升可读性

`ResourceAttrItem` 类型扩展（`resourceAttribute.service.ts`）：
```typescript
reportTime?: string | null;  // 仅 realtime 属性携带
```

---

## 备注

- `relationId` 来自 `res_type_attribute_binding.id`（cuid 字符串，全局唯一）；前端仍按 `sourceScope` 选择一级、二级或资源本级接口
- `profileCompleteFlag = null` 表示"未计算"，禁止当作 false 处理；前端过渡期不展示角标
- 合并冲突（ATTR_BINDING_CONFLICT）是防御性断言，正常流程不可达，触发说明数据旁路写入或 Bug


---

## 前端任务（l4-res-admin）

> 项目路径：`~/appstore/project/L4/l4-res-admin`
> 技术栈：React 18 + TypeScript + Ant Design 5 + Vite + yarn 4
> 验收命令：`tsc -b`（类型检查）/ `yarn test`（单元测试）

### 执行规则（前端 Agent 必读）

- 改 `ResourceEditModal.tsx` 时**现有表单逻辑零改动**，属性 Tab 是完全独立子组件
- `ResourceAttributeTab` 只接收三个 prop：`resourceId / resourceSubtypeId / open`，自己管理数据生命周期
- 类型变更未保存时属性 Tab 必须禁用（监听 `watchedSubtypeId` vs `detail.resourceSubtypeId`）
- 资源本级属性管理只做追加，不允许覆盖一级/二级继承属性；重复绑定以服务端 409 为准
- 每个 Feature 完成后执行 `tsc -b` 验证，通过才进入下一个

### 阶段六：API Service 层

#### F010 — attributeDefinition.service.ts
**新建文件：** `src/services/attributeDefinition.service.ts`

封装接口：
```
GET    /internal/attribute-definitions          listAttributeDefinitions(params?)
POST   /internal/attribute-definitions          createAttributeDefinition(dto)
GET    /internal/attribute-definitions/:attrId  getAttributeDefinition(attrId)
PATCH  /internal/attribute-definitions/:attrId  updateAttributeDefinition(attrId, dto)
```

类型定义（含 `AttrDataCategory = 'static' | 'realtime'`，`AttrDataType = 'string'|'number'|'boolean'|'date'|'enum'|'json'`）

约束：`attr_code` 字段在编辑表单中必须禁用（只读展示），不允许修改。

消费方：`list`（含 `?keyword=` 搜索参数）/ `create` / `get` 由 F013 `AttributeDefinitionBindingModal` 和 `AttributeDefinitionQuickCreateModal` 消费；`update（PATCH）` 由 F023 属性定义管理页消费。

---

#### F011 — typeAttribute.service.ts
**新建文件：** `src/services/typeAttribute.service.ts`

封装接口：
```
// 一级类型属性
GET    /internal/resource-parent-types/:parentType/attributes        listParentTypeAttributes(parentType)
POST   /internal/resource-parent-types/:parentType/attributes        createParentTypeAttribute(parentType, dto)
PATCH  /internal/resource-parent-types/:parentType/attributes/:id    updateParentTypeAttribute(parentType, id, dto)
DELETE /internal/resource-parent-types/:parentType/attributes/:id    deleteParentTypeAttribute(parentType, id)

// 二级类型属性
GET    /internal/resource-types/:typeId/attributes                   listTypeAttributes(typeId)          ← 合并视图（inherited+own），F021 diff 计算使用
GET    /internal/resource-types/:typeId/own-attributes               listOwnTypeAttributes(typeId)
GET    /internal/resource-types/:typeId/inherited-attributes         listInheritedTypeAttributes(typeId)
POST   /internal/resource-types/:typeId/attributes                   createTypeAttribute(typeId, dto)
PATCH  /internal/resource-types/:typeId/attributes/:id               updateTypeAttribute(typeId, id, dto)
DELETE /internal/resource-types/:typeId/attributes/:id               deleteTypeAttribute(typeId, id)
```

类型定义：`MergedAttrItem`（含 `sourceScope: 'parent' | 'subtype'`、`relationId`）

注意：`listTypeAttributes` 是 F021 类型变更影响提示差集计算的唯一消费方，必须与 F011 同步实现，不可延后。

---

#### F012 — resourceAttribute.service.ts
**新建文件：** `src/services/resourceAttribute.service.ts`

封装接口：
```
GET    /internal/resources/:resourceId/attributes                    listResourceAttributes(resourceId)
PATCH  /internal/resources/:resourceId/attributes/:attrId            updateResourceAttribute(resourceId, attrId, dto)
PATCH  /internal/resources/:resourceId/attributes/:attrId/force      forceUpdateResourceAttribute(resourceId, attrId, dto)
GET    /internal/resources/:resourceId/profile-completeness          getProfileCompleteness(resourceId)
```

类型定义：`ResourceAttrItem`（含 `attrValue / editable / valueSource / updatedBy / dataType / enumOptions: string[] | null`）、`ProfileCompleteness`

---

### 阶段六·补充：属性定义管理页

#### F023 — AttributeDefinitionsPage
**新建文件：** `src/pages/attribute-definitions/AttributeDefinitionsPage.tsx`

列表列：`attrCode / attrName / dataCategory / dataType / unit / enabledFlag（启用/停用）`

功能：
- **新建**：调用 `createAttributeDefinition`，`dataCategory` 表单必填
- **编辑**：调用 `updateAttributeDefinition`；`attrCode` 禁用只读；`dataCategory` 禁用只读；后端拒绝修改 `dataType` 时展示文案"该属性已有资源值记录，数据类型不可修改"
- **停用/启用**：调用 `PATCH enabledFlag`；停用时提示"停用后该属性不再出现在类型绑定合并结果中，已有绑定记录保留。确认停用？"

路由：`src/router/index.tsx` 新增 `/attribute-definitions`（lazy + `RequireAuth`），导航菜单加对应项。

验收：`tsc -b` 通过；新建/编辑/停用闭环可用；`attrCode` 与 `dataCategory` 在编辑态只读。

---

### 阶段七：类型属性管理 Drawer

#### F013 — TypeAttributeDrawer + TypeAttributeOwnTab + AttributeDefinitionBindingModal + AttributeDefinitionQuickCreateModal
**新建文件：**
- `src/pages/resource-types/TypeAttributeDrawer.tsx`
- `src/pages/resource-types/TypeAttributeOwnTab.tsx`
- `src/pages/resource-types/AttributeDefinitionBindingModal.tsx`（新增绑定弹窗）
- `src/pages/resource-types/AttributeDefinitionQuickCreateModal.tsx`（内联快速创建属性定义）

`TypeAttributeDrawer` prop 类型：
```typescript
type TypeAttributeScope =
  | { scope: 'parent'; parentType: ResourceParentType }
  | { scope: 'subtype'; parentType: ResourceParentType; typeId: string };

interface TypeAttributeDrawerProps {
  scope: TypeAttributeScope | null;  // null 时 Drawer 关闭
  onClose: () => void;
}
```

Drawer 内 Tabs：
- 一级类型：`[本级属性]`（二期属性统计 Tab 禁用，显示"敬请期待"）
- 二级类型：`[本级属性][继承自一级类型]`（二期 Tab 禁用）

`TypeAttributeOwnTab`：
- 展示属性列表（`attrCode / attrName / dataType / required / editable / sortNo / enabled`）
- 操作：新增绑定、编辑 flag、删除
- 删除前展示确认提示："删除属性「X」后，该属性将不再展示和参与完整度计算；已有资源属性值会保留为隐藏数据。确认删除？"

**新增绑定完整交互流（内联创建，全程不离开 Drawer）：**

```
TypeAttributeOwnTab「新增绑定」
  → AttributeDefinitionBindingModal
      可搜索 Select（调用 listAttributeDefinitions，支持 name/code 模糊过滤）
      ↗ Select 下拉底部固钉：「+ 新建属性定义」
      绑定配置：requiredFlag / editableFlag / statisticFlag / sortNo（默认从定义复制）
      [取消] [确认绑定] → POST /attributes
      
  「+ 新建属性定义」→ AttributeDefinitionQuickCreateModal（嵌套 Modal）
      attrCode *（全局唯一）
      attrName *
      dataType *（string / number / boolean / date / enum / json）
      unit（number 时展示）
      enumOptions（enum 时展示，可动态增减枚举项）
      备注
      dataCategory 固定 'static'（预填隐藏，一期限制）
      [取消] [创建] → POST /internal/attribute-definitions
      创建成功 → QuickCreate 关闭 → 父 Modal Select 自动选中新定义
```

`AttributeDefinitionBindingModal` Props：
```typescript
interface AttributeDefinitionBindingModalProps {
  open: boolean;
  scope: TypeAttributeScope;
  onSuccess: () => void;   // 绑定成功后刷新列表
  onCancel: () => void;
}
```

`AttributeDefinitionQuickCreateModal` Props：
```typescript
interface AttributeDefinitionQuickCreateModalProps {
  open: boolean;
  onSuccess: (newDef: { id: string; attrCode: string; attrName: string }) => void;
  onCancel: () => void;
}
```

`listAttributeDefinitions` 需支持搜索参数（F010/F011 调用处补 `?keyword=` param，后端 F002 `GET /attribute-definitions` 支持按 `attrName` / `attrCode` 过滤）。

---

#### F014 — TypeAttributeInheritedTab
**新建文件：** `src/pages/resource-types/TypeAttributeInheritedTab.tsx`

- 调用 `listInheritedTypeAttributes(typeId)` 展示继承属性列表
- 只读，无绑定/解绑入口
- 列表底部提示："如需修改继承属性，请前往一级类型属性管理"

---

#### F015 — ResourceTypesPage 接入入口按钮
**改动文件：** `src/pages/resource-types/ResourceTypesPage.tsx`

改动范围最小：
1. 引入 `TypeAttributeDrawer`，添加 `scope` state（初始 null）
2. 一级类型区域增加"属性管理"按钮，点击设置 `scope = { scope: 'parent', parentType }`
3. 二级类型详情区增加"属性管理"按钮，点击设置 `scope = { scope: 'subtype', parentType, typeId }`
4. 渲染 `<TypeAttributeDrawer scope={scope} onClose={() => setScope(null)} />`

---

### 阶段八：资源实例属性 Tab

#### F016 — ResourceAttributeTab
**新建文件：** `src/pages/resources/ResourceAttributeTab.tsx`

（原名 `DeviceAttributeTab`，已重命名——组件覆盖所有资源类型 Device/IOT/Human/Amenities/Goods，非 Device 专属。）

Props（严格按方案）：
```typescript
interface ResourceAttributeTabProps {
  resourceId: string;
  resourceSubtypeId: string | null;
  open: boolean;
}
```

行为：
- `open` 变 true 时调用 `listResourceAttributes(resourceId)`
- 关闭时清理本地 state
- 每行属性有独立"编辑/保存"按钮，调用 `updateResourceAttribute`
- `editable=false` 的属性显示锁定图标，管理员可触发强制更新（打开 F017 `ResourceAttributeForceUpdateModal` 弹窗）
- `attrValue=null` 显示占位符"—"
- `valueSource='force'` 显示"强制更新"标签

渲染控件根据 `dataType`：
- `string` → Input
- `number` → InputNumber（含 unit 后缀）
- `boolean` → Switch
- `date` → DatePicker
- `enum` → Select（选项来自 `enumOptions`）
- `json` → 只读展示，不提供编辑（一期）

---

#### F017 — ResourceAttributeForceUpdateModal
**新建文件：** `src/pages/resources/ResourceAttributeForceUpdateModal.tsx`

（原名 `DeviceAttributeForceUpdateModal`，已随 F016 统一重命名。）

Props：
```typescript
interface Props {
  open: boolean;
  resourceId: string;
  attrId: string;
  attrName: string;
  dataType: AttrDataType;          // 决定新值输入控件类型
  enumOptions: string[] | null;    // dataType='enum' 时使用
  currentValue: string | null;
  onSuccess: () => void;
  onCancel: () => void;
}
```

表单字段：新值（按 `dataType` 渲染对应控件，`json` 类型一期只读不可强制更新）+ 必填原因输入框
提交调用 `forceUpdateResourceAttribute`，成功后调 `onSuccess` 刷新属性列表

---

#### F018 — ResourceEditModal 接入属性 Tab
**改动文件：** `src/pages/resources/ResourceEditModal.tsx`

**约束：现有 form 逻辑一行不改。**

改动步骤（最小范围）：
1. 引入 `Tabs`（antd）和 `ResourceAttributeTab`
2. 现有表单内容整体包裹进 `<Tabs.TabPane tab="基础信息" key="base">`
3. 新增 `<Tabs.TabPane tab="属性" key="attrs">`，内容为 `<ResourceAttributeTab resourceId={record.resourceId} resourceSubtypeId={detail?.resourceSubtypeId ?? null} open={open} />`
4. 监听 `watchedSubtypeId`（已有）vs `detail?.resourceSubtypeId`：不一致时属性 Tab disabled，Tooltip 提示"类型已变更但尚未保存，请先保存基础信息后再编辑属性"
5. **类型变更确认提示**（见 F021）：当 `watchedSubtypeId !== detail?.resourceSubtypeId` 时，用户点击基础信息"保存"前先弹出确认弹窗；确认后再提交 PATCH resource 请求
6. Modal 底部"保存"按钮只触发基础信息提交，不触发属性保存
7. 支持外部指定初始/当前 Tab，用于 F019 创建后点击"去补录"直接打开属性 Tab

---

### 阶段九：创建提示 + 列表完整度

#### F019 — ResourceRegisterModal 创建后提示
**改动文件：** `src/pages/resources/ResourceRegisterModal.tsx`

资源创建成功后，调用 `getProfileCompleteness(resourceId)`：
- 若 `profileComplete=false`，展示提示："该资源有 N 个必填属性未完善，是否立即补录？"提供"去补录"按钮（触发打开 ResourceEditModal 并切换到属性 Tab；F018 需支持外部指定初始/当前 Tab）
- `profileComplete=true` 或 `null` 不提示

---

#### F021 — ResourceEditModal 类型变更影响提示
**改动文件：** `src/pages/resources/ResourceEditModal.tsx`

在基础信息表单的"保存"流程中插入确认步骤（**不改动现有 validateFields / buildPayload 逻辑**，只在 `handleOk` 调用提交前增加一次 confirm）：

触发条件：`watchedSubtypeId !== detail?.resourceSubtypeId`（即用户修改了二级类型且尚未保存）。

实现方式：
1. 调用 `listResourceAttributes(resourceId)` 获取旧类型属性集合
2. 调用 `typeAttribute.service.listTypeAttributes(newTypeId)`（合并视图，F011 已补充）获取新类型属性集合
3. 差集 N = 旧类型属性集合 中 attrId 不在新类型集合的数量
4. 用 antd `Modal.confirm` 弹出提示：
   ```
   变更类型后，N 个旧类型独有属性将不再展示，已有属性值会保留为隐藏数据。确认继续？
   ```
5. 用户确认后执行原有提交逻辑；取消则中断提交，表单保持当前编辑状态

若 N = 0（无独有属性），跳过弹窗直接提交。

---

#### F020 — 资源列表完整度角标
**改动文件：** `src/pages/resources/index.tsx`（或列表所在文件）

- 列表接口响应新增 `profileComplete: boolean | null` 字段（后端已扩展）
- `profileComplete=false`：在资源名称旁展示橙色"资料未完善"Tag，带 `missingRequiredAttributeCount` 数字
- `profileComplete=null`：不展示任何角标（过渡期）
- `profileComplete=true`：不展示

---

### 阶段十一：资源本级属性绑定前端增强

#### F027 — resourceAttribute.service.ts 增加资源本级绑定 API
**改动文件：** `src/services/resourceAttribute.service.ts`

类型调整：

- `TypeAttributeSourceScope` 或资源属性来源类型扩展为 `parent | subtype | resource`。
- `ResourceAttrItem.sourceScope` 支持 `resource`。
- 新增 `ResourceAttributeBindingItem`，用于资源本级绑定列表；该类型包含 `relationId/attrId/attrCode/attrName/dataCategory/dataType/unit/enumOptions/required/editable/statistic/sortNo/enabledFlag/sourceScope/sourceName`，不包含 `attrValue/valueSource/updatedBy`。
- 新增资源本级绑定 payload 类型，复用 `CreateTypeAttributePayload` / `UpdateTypeAttributePayload` 字段形态或在本 service 内定义同构类型。

新增方法：

```typescript
listResourceAttributeBindings(resourceId: string): Promise<ResourceAttributeBindingItem[]>
createResourceAttributeBinding(resourceId: string, payload: CreateResourceAttributeBindingPayload): Promise<ResourceAttributeBindingItem>
updateResourceAttributeBinding(resourceId: string, relationId: string, payload: UpdateResourceAttributeBindingPayload): Promise<ResourceAttributeBindingItem>
deleteResourceAttributeBinding(resourceId: string, relationId: string): Promise<{ success: boolean }>
```

验收：`tsc -b` 通过；现有属性值更新方法不受影响。

---

#### F028 — ResourceAttributeTab 增加资源本级属性管理区
**改动文件：** `src/pages/resources/ResourceAttributeTab.tsx`

UI 结构：

```text
[属性值] [本级属性]
```

- 属性值：继续展示 `GET /internal/resources/{resourceId}/attributes` 的最终属性列表。
- 本级属性：管理员可见，展示 `GET /internal/resources/{resourceId}/attribute-bindings`。
- 新增绑定**直接新建 `ResourceAttributeBindingModal`**（`src/pages/resources/ResourceAttributeBindingModal.tsx`），不改造 `AttributeDefinitionBindingModal`。理由：后者内部路由调用 `typeAttributeService`（`createParentTypeAttribute` / `createTypeAttribute`），资源本级绑定需要调用 `resourceAttributeService.createResourceAttributeBinding`，两者接口语义完全不同，抽成纯 UI 的改造成本等同于新建且会引入额外风险。两个 Modal 各自独立，共用属性定义搜索 Select 逻辑（调用 `listAttributeDefinitions`）即可。
- 绑定 Select 默认过滤 `dataCategory='static'`、`enabledFlag=true`。
- 本级属性删除确认文案说明：删除绑定不删除已有属性值，属性值会变成隐藏孤儿值。
- 继承属性不可在本级属性 Tab 中编辑；如后端返回重复绑定 409，提示服务端错误文案。
- 后端返回 403 时提示无权限，不允许前端仅靠隐藏入口作为权限控制。

验收：`tsc -b` 通过；普通用户无管理入口；管理员可完成新增、编辑、删除绑定流程。

---

#### F029 — 资源本级绑定变更后的刷新与完整度提示
**改动文件：**
- `src/pages/resources/ResourceAttributeTab.tsx`
- 必要时 `src/pages/resources/ResourceEditModal.tsx`

行为：

- 新增、编辑、删除资源本级绑定成功后，重新拉取最终属性列表。
- 重新调用 `getProfileCompleteness(resourceId)`，刷新“必填未完善”提示。
- 若当前正在编辑某个属性值，绑定变更前需阻止或确认，避免本地未保存值丢失。
- `resourceSubtypeId` 未保存变更时，仍沿用 F018 规则禁用整个属性 Tab。

验收：

```bash
tsc -b
yarn test
yarn build
```

---

### 前端 Feature 清单补充

| ID | Feature | 验收标准 | Status | Commit |
|----|---------|---------|--------|--------|
| F010 | attributeDefinition.service.ts | `tsc -b` 通过；类型定义完整 | done | 5cd5169 |
| F011 | typeAttribute.service.ts | `tsc -b` 通过；一级/二级接口均覆盖；含 `listTypeAttributes` 合并视图方法 | done | 5cd5169 |
| F012 | resourceAttribute.service.ts | `tsc -b` 通过；类型定义完整 | done | 5cd5169 |
| F013 | TypeAttributeDrawer + OwnTab + BindingModal + QuickCreateModal | `tsc -b` 通过；删除确认文案正确；新建属性定义后 Select 自动选中 | done | 5cd5169 |
| F014 | TypeAttributeInheritedTab | `tsc -b` 通过；只读，无操作按钮 | done | 5cd5169 |
| F015 | ResourceTypesPage 入口按钮 | `tsc -b` 通过；两种 scope 均可打开 Drawer | done | 5cd5169 |
| F016 | ResourceAttributeTab | `tsc -b` 通过；6 种 dataType 控件正确渲染 | done | 44e8382 |
| F017 | ResourceAttributeForceUpdateModal | `tsc -b` 通过；reason 字段必填校验 | done | 44e8382 |
| F018 | ResourceEditModal 接入属性 Tab | `tsc -b` 通过；`npm test` 原有测试不回归；类型变更未保存时属性 Tab disabled | done | 44e8382 |
| F019 | ResourceRegisterModal 创建后提示 | `tsc -b` 通过；`profileComplete=null` 不提示 | done | 50a3174 |
| F020 | 资源列表完整度角标 | `tsc -b` 通过；`null` 不展示角标 | done | 50a3174 |
| F021 | ResourceEditModal 类型变更影响提示 | `tsc -b` 通过；N=0 时无弹窗；N>0 时弹窗展示正确数量；取消不提交 | done | 50a3174 |
| F023 | AttributeDefinitionsPage | `tsc -b` 通过；新建/编辑/停用闭环；`attrCode` 编辑态只读 | done | 5cd5169 |
| F027 | resourceAttribute.service.ts 资源本级绑定 API | `tsc -b` 通过；类型覆盖 `sourceScope=resource` | done | |
| F028 | ResourceAttributeTab 本级属性管理区 | `tsc -b` 通过；管理员可新增/编辑/删除资源本级绑定 | done | |
| F029 | 绑定变更刷新与完整度提示 | `tsc -b` + `yarn test` + `yarn build` 通过；绑定变更后属性列表和完整度同步刷新 | done | |

### 前端改动项目

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `src/services/attributeDefinition.service.ts` | 新建 | 属性定义 API |
| `src/services/typeAttribute.service.ts` | 新建 | 类型属性 API（一级+二级） |
| `src/services/resourceAttribute.service.ts` | 新建 + 增强 | 资源实例属性 API；F027 增加资源本级绑定 API |
| `src/pages/resource-types/TypeAttributeDrawer.tsx` | 新建 | 类型属性管理 Drawer |
| `src/pages/resource-types/TypeAttributeOwnTab.tsx` | 新建 | 本级属性列表 Tab |
| `src/pages/resource-types/AttributeDefinitionBindingModal.tsx` | 新建（F013） | 新增绑定弹窗（可搜索 Select + 绑定配置） |
| `src/pages/resource-types/AttributeDefinitionQuickCreateModal.tsx` | 新建（F013） | 内联快速创建属性定义（嵌套于 BindingModal） |
| `src/pages/resource-types/TypeAttributeInheritedTab.tsx` | 新建 | 继承属性展示 Tab（只读） |
| `src/pages/resource-types/ResourceTypesPage.tsx` | 小改 | 新增入口按钮 + Drawer 引入 |
| `src/pages/resources/ResourceAttributeTab.tsx` | 新建 + 增强 | 资源实例属性 Tab；F028/F029 增加资源本级属性管理与刷新 |
| `src/pages/resources/ResourceAttributeForceUpdateModal.tsx` | 新建 | 管理员强制更新弹窗 |
| `src/pages/resources/ResourceEditModal.tsx` | 小改 | 套 Tabs 壳 + 引入属性 Tab；现有 form 零改动 |
| `src/pages/resources/ResourceRegisterModal.tsx` | 小改 | 创建成功后完整度提示 |
| `src/pages/resources/index.tsx` | 小改 | 列表增加完整度角标 |
| `src/pages/resources/ResourceEditModal.tsx` | 小改（F021） | handleOk 前插入类型变更确认弹窗（已含于 F018 改动文件，此行标注 F021 增量） |
| `src/pages/attribute-definitions/AttributeDefinitionsPage.tsx` | 新建（F023） | 属性定义列表 + 新建/编辑/停用 |
| `src/router/index.tsx` | 小改（F023） | 新增 `/attribute-definitions` 路由 + 导航菜单项 |

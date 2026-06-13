---
tags: [任务, l4, res-atomization, attribute-management]
date: 2026-06-12
status: in-progress
priority: high
assignee: agent
created: 2026-06-12
updated: 2026-06-13
patch: gap-review-补全
deadline:
branch: feature/attribute-management
retrieval_triggers: [属性管理, 类型属性, 资源属性值, 完整度, attribute management, res-atomization]
---

# L4 资源原子化 — 类型属性管理一期实现

## 目标

为 `l4-res-atomization` 实现静态属性管理闭环：支持一级/二级类型绑定属性、资源实例维护属性值、完整度计算与异步批量重算。

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
| [前] 资源属性 Tab + 完整度角标 | l4-res-admin | | |

## 开发操作指南

### 会话窗口规划

最多同时开 **2 个窗口**（后端 + 前端并行时），大部分时间只用 1 个。

| 会话      | 工作目录                 | 包含 Feature                | 可以开始的条件   |
| ------- | -------------------- | ------------------------- | --------- |
| S1      | `l4-res-atomization` | F001                      | 随时        |
| S2      | `l4-res-atomization` | F002 + F003               | S1 完成     |
| S3      | `l4-res-atomization` | F004 + F005               | S2 完成     |
| S4 ‖ S3 | `l4-res-admin`       | F010 + F011 + F012 + F023 | S2 完成即可并行 |
| S5      | `l4-res-atomization` | F006 + F007               | S3 完成     |
| S6 ‖ S5 | `l4-res-admin`       | F013 + F014 + F015        | S3 完成即可并行 |
| S7      | `l4-res-atomization` | F008 + F009 + F022        | S5 完成     |
| S8 ‖ S7 | `l4-res-admin`       | F016 + F017 + F018        | S5 完成即可并行 |
| S9      | `l4-res-admin`       | F019 + F020 + F021        | S7 完成     |

### 每次启动新会话的开场提示

**后端会话**（在 `l4-res-atomization` 目录启动）：

```
我在实现 task-l4-res-attribute-management 任务计划，后端项目 l4-res-atomization。
请先读：
- 架构方案：~/appstore/project/java-coding/obsidian/projects/arch-l4-res-device-type-attribute-management.md
- 任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md
S6 ‖ S5已完成 当前实现 [F008 + F009 + F022]，请读任务计划对应章节后开始。
```

**前端会话**（在 `l4-res-admin` 目录启动）：

```
我在实现 task-l4-res-attribute-management 任务计划，前端项目 l4-res-admin。
请先读任务计划：~/appstore/project/java-coding/obsidian/tasks/task-l4-res-attribute-management.md
S7已完成 当前实现 [F016 + F017 + F018]，请读前端章节对应部分后开始。
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
- [ ] F016：ResourceAttributeTab
- [ ] F017：ResourceAttributeForceUpdateModal
- [ ] F018：ResourceEditModal 接入属性 Tab

**阶段八回归验收（最关键，F018 改动高风险文件）：**
```bash
tsc -b
yarn test                                 # 必须全量通过，ResourceEditModal 相关用例重点检查
# 手动验证：打开资源编辑弹窗，确认基础信息 Tab 原有交互无异常
```

---

#### 阶段九：创建提示 + 列表完整度（依赖：后端 F009 集成完成）
- [ ] F019：ResourceRegisterModal 创建后提示
- [ ] F020：资源列表完整度角标
- [ ] F021：ResourceEditModal 类型变更影响提示

**阶段九回归验收（前端收尾，完整回归）：**
```bash
tsc -b
yarn test                                 # 前端全量通过，与阶段六基线对比无新失败
yarn build                                # 生产构建通过，无 bundle 错误
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

**Current**: 后端全量完成（S7 完成 F008+F009+F022）

## 当前状态快照

- 最后更新：2026-06-13
- 当前进展：后端全量完成（F001–F009 + F022）；前端 S4/S6 已完成（F010–F015 + F023），待 S8/S9
- 下次启动入口：`cd ~/appstore/project/L4/l4-res-admin`
- 待续位置：S8 — F016 + F017 + F018（需后端 F006 接口就绪，已就绪）

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

## 改动项目

| 项目 | 路径 | 改动内容 |
|------|------|---------|
| l4-res-atomization | `prisma/schema.prisma` | 新增 3 张表 + 1 字段 |
| l4-res-atomization | `src/modules/attribute-definition/` | 新建模块 |
| l4-res-atomization | `src/modules/attribute/` | 新建合并工具 |
| l4-res-atomization | `src/modules/parent-type-attribute/` | 新建模块 |
| l4-res-atomization | `src/modules/resource-type/` | 扩展二级属性 service + controller |
| l4-res-atomization | `src/modules/resource-attribute/` | 新建模块 |
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
   ```prisma
   model ResTypeAttributeBinding {
     id            String   @id @default(cuid())
     scopeType     String   @map("scope_type") @db.VarChar(16)   // 'parent' | 'subtype'
     scopeId       String   @map("scope_id") @db.VarChar(128)    // parentType 或 resourceSubtypeId
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
  sourceScope: 'parent' | 'subtype';
  sourceName: string;
}

export async function mergeTypeAttributes(
  parentType: string,
  resourceSubtypeId: string | null,
  prisma: PrismaService,
): Promise<MergedAttrItem[]>
```

逻辑：
1. 查 `resTypeAttributeBinding JOIN resAttributeDefinition`（`scopeType='parent'`、两表 `enabledFlag=true`、`dataCategory='static'`），按 `sortNo` 升序
2. `resourceSubtypeId` 非空时查 `resTypeAttributeBinding JOIN` 定义表（`scopeType='subtype'`），同条件
3. ownAttrs 中有 attrId 已在 parentAttrs → `logger.error` + `throw AppException('ATTR_BINDING_CONFLICT', ..., 409)`
4. 返回 `[...parentAttrs, ...ownAttrs]`

sourceName 映射规则：
- `sourceScope='parent'`：枚举映射 `Device→机器人, IOT→IOT, Human→人员, Amenities→客需品, Goods→商品`
- `sourceScope='subtype'`：从 `resource_type_option.name` 读取（合并查询时 JOIN 该表取 name 字段）

单元测试 3 个 case：只 parent / parent+subtype / 重复 attrId 冲突

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
1. 批量 UPDATE editable_flag（newAttrs 全集）
2. INSERT 新类型必填属性缺失空值记录（skipDuplicates: true，`editableFlag` 取新类型绑定层）
3. UPDATE resource_profile.resource_subtype_id

事务提交后同步重算完整度。

**列表接口**：响应增加 `profileComplete / missingRequiredAttributeCount`；支持 `?profileComplete=true|false` 筛选；`false` 只匹配明确 false，`null` 不归入未完善。

---

### F022 — 存量完整度 backfill

**背景**：F001–F009 上线后存量资源的 `profileCompleteFlag` 全为 null（初始值）。前端 F020 列表角标
和筛选依赖该字段，需要在展示角标前完成一次全量重算。

**新建文件：** `src/scripts/backfill-completeness.ts`

实现方式（复用 `CompletenessService`，无需新写遍历逻辑）：

```typescript
// 对 5 个 parentType 各投递一个 scope='parentType' job
// processor 已覆盖「含无 resourceSubtypeId 但 type=parentType」的历史资源
const PARENT_TYPES = ['Device', 'IOT', 'Human', 'Amenities', 'Goods'];
for (const id of PARENT_TYPES) {
  await completenessService.enqueueRecompute({ scope: 'parentType', id });
}
```

processor 已有分批 200 条处理逻辑，全量 backfill 不会导致单次事务过长。

**执行方式**：上线流程第 2 步由运维手动执行一次：

```bash
npx ts-node src/scripts/backfill-completeness.ts
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
SELECT COUNT(*) FROM resource_profile
WHERE profile_complete_flag IS NULL
  AND status != 'deleted'
  AND type IN ('Device','IOT','Human','Amenities','Goods');
```

Bull queue 处理有延迟，建议脚本投递 job 后等待约 10 分钟再执行上述检查。若 count 不为 0，说明 processor 有 job 失败，检查 Bull 失败队列后重新投递。

## 进度日志

## 备注

- `relationId` 来自 `res_type_attribute_binding.id`（cuid 字符串，全局唯一）；前端仍按 `sourceScope` 选择一级或二级接口
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

### 前端 Feature 清单补充

| ID | Feature | 验收标准 | Status | Commit |
|----|---------|---------|--------|--------|
| F010 | attributeDefinition.service.ts | `tsc -b` 通过；类型定义完整 | done | 5cd5169 |
| F011 | typeAttribute.service.ts | `tsc -b` 通过；一级/二级接口均覆盖；含 `listTypeAttributes` 合并视图方法 | done | 5cd5169 |
| F012 | resourceAttribute.service.ts | `tsc -b` 通过；类型定义完整 | done | 5cd5169 |
| F013 | TypeAttributeDrawer + OwnTab + BindingModal + QuickCreateModal | `tsc -b` 通过；删除确认文案正确；新建属性定义后 Select 自动选中 | done | 5cd5169 |
| F014 | TypeAttributeInheritedTab | `tsc -b` 通过；只读，无操作按钮 | done | 5cd5169 |
| F015 | ResourceTypesPage 入口按钮 | `tsc -b` 通过；两种 scope 均可打开 Drawer | done | 5cd5169 |
| F016 | ResourceAttributeTab | `tsc -b` 通过；6 种 dataType 控件正确渲染 | pending | |
| F017 | ResourceAttributeForceUpdateModal | `tsc -b` 通过；reason 字段必填校验 | pending | |
| F018 | ResourceEditModal 接入属性 Tab | `tsc -b` 通过；`yarn test` 原有测试不回归；类型变更未保存时属性 Tab disabled | pending | |
| F019 | ResourceRegisterModal 创建后提示 | `tsc -b` 通过；`profileComplete=null` 不提示 | pending | |
| F020 | 资源列表完整度角标 | `tsc -b` 通过；`null` 不展示角标 | pending | |
| F021 | ResourceEditModal 类型变更影响提示 | `tsc -b` 通过；N=0 时无弹窗；N>0 时弹窗展示正确数量；取消不提交 | pending | |
| F023 | AttributeDefinitionsPage | `tsc -b` 通过；新建/编辑/停用闭环；`attrCode` 编辑态只读 | done | 5cd5169 |

### 前端改动项目

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `src/services/attributeDefinition.service.ts` | 新建 | 属性定义 API |
| `src/services/typeAttribute.service.ts` | 新建 | 类型属性 API（一级+二级） |
| `src/services/resourceAttribute.service.ts` | 新建 | 资源实例属性 API |
| `src/pages/resource-types/TypeAttributeDrawer.tsx` | 新建 | 类型属性管理 Drawer |
| `src/pages/resource-types/TypeAttributeOwnTab.tsx` | 新建 | 本级属性列表 Tab |
| `src/pages/resource-types/AttributeDefinitionBindingModal.tsx` | 新建（F013） | 新增绑定弹窗（可搜索 Select + 绑定配置） |
| `src/pages/resource-types/AttributeDefinitionQuickCreateModal.tsx` | 新建（F013） | 内联快速创建属性定义（嵌套于 BindingModal） |
| `src/pages/resource-types/TypeAttributeInheritedTab.tsx` | 新建 | 继承属性展示 Tab（只读） |
| `src/pages/resource-types/ResourceTypesPage.tsx` | 小改 | 新增入口按钮 + Drawer 引入 |
| `src/pages/resources/ResourceAttributeTab.tsx` | 新建 | 资源实例属性 Tab |
| `src/pages/resources/ResourceAttributeForceUpdateModal.tsx` | 新建 | 管理员强制更新弹窗 |
| `src/pages/resources/ResourceEditModal.tsx` | 小改 | 套 Tabs 壳 + 引入属性 Tab；现有 form 零改动 |
| `src/pages/resources/ResourceRegisterModal.tsx` | 小改 | 创建成功后完整度提示 |
| `src/pages/resources/index.tsx` | 小改 | 列表增加完整度角标 |
| `src/pages/resources/ResourceEditModal.tsx` | 小改（F021） | handleOk 前插入类型变更确认弹窗（已含于 F018 改动文件，此行标注 F021 增量） |
| `src/pages/attribute-definitions/AttributeDefinitionsPage.tsx` | 新建（F023） | 属性定义列表 + 新建/编辑/停用 |
| `src/router/index.tsx` | 小改（F023） | 新增 `/attribute-definitions` 路由 + 导航菜单项 |

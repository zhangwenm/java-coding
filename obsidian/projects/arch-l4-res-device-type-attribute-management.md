---
tags: [l4, res-admin, resource-type, attribute-management, architecture]
date: 2026-06-16
project: l4-res-admin
status: draft
retrieval_triggers: [资源类型属性管理, 类型属性, 属性继承, 资源属性值, 资源原子化, resource_type_option, resource_profile]
---

# L4 资源原子化类型属性管理方案

## 背景

`l4-res-admin` 当前类型结构不是数据库驱动的任意层级树，而是：

- 一级类型由前端硬编码：`Device`、`IOT`、`Human`、`Amenities`、`Goods`。
- 二级类型由后端 `/internal/resource-types` 维护，对应 `resource_type_option.typeId`。
- 资源实例由 `/internal/resources` 维护，对应 `resource_profile.resourceId`，二级类型字段为 `resourceSubtypeId`。

因此本方案不再按“多级设备类型树”设计，不引入 `parent_id/path/type_path` 这类通用树结构。属性能力作为现有资源原子化模型的扩展：一级类型属性可被二级类型继承，二级类型可维护自己的本级属性，最终属性落到资源实例。

> **后端工程边界：** 本文后端实现细节以 `l4-res-atomization` 为准（NestJS + Prisma + PostgreSQL + Bull）。当前 `l4-res-admin` 仓库只承载前端页面和 API service 封装。

## 目标

- 支持固定一级类型维护属性。
- 支持二级类型维护本级属性，并继承一级类型属性。
- 支持资源实例维护属性值。
- 支持资源实例维护本级属性绑定，用于少量资源级差异化扩展。
- 属性名称使用单语言。
- 属性定义可弹性扩展。
- 属性值可配置为普通用户可更新或不可更新。
- 不可更新属性允许管理员强制更新，并写审计日志。
- 不保存属性值历史版本。
- 资料完整度只基于静态必填属性计算。
- 一期优先落地 PostgreSQL 静态属性闭环，Kafka、Redis Cluster、Elasticsearch 后置。

## 当前类型结构

### 一级类型

一级类型在前端 `RESOURCE_PARENT_TYPE_OPTIONS` 中硬编码：

| label | value |
|---|---|
| 机器人 | Device |
| IOT | IOT |
| 人员 | Human |
| 客需品 | Amenities |
| 商品 | Goods |

一级类型不走新增、删除接口，不作为数据库树节点维护。后端接口只需要识别这些固定枚举值。

### 二级类型

二级类型由后端维护：

```text
GET    /internal/resource-types?parentType=Device
POST   /internal/resource-types
PATCH  /internal/resource-types/{typeId}
DELETE /internal/resource-types/{typeId}
```

核心字段：

| 字段 | 说明 |
|---|---|
| typeId | 二级类型 ID |
| parentType | 所属一级类型 |
| name | 类型名称 |
| status | 启停状态 |
| sortOrder | 排序 |
| configJson | 当前已有能力配置 |

### 继承关系

当前支持三层属性来源：

```text
一级类型属性(parentType)
  ↓
二级类型属性(resourceSubtypeId)
  ↓
资源本级属性(resourceId)
  ↓
资源实例属性值(resourceId)
```

最终属性计算：

```text
resourceSubtypeId 有值：最终属性 = 一级类型属性 + 二级类型本级属性 + 资源本级属性
resourceSubtypeId 为空：最终属性 = resource_profile.type 对应的一级类型属性 + 资源本级属性
```

不支持任意多级祖先继承，不支持二级类型覆盖一级类型属性配置，也不支持资源本级属性覆盖一级/二级类型属性配置。资源本级属性只用于少量实例差异化扩展，不能重复绑定已经从一级或二级类型继承来的 `attrId`。

### 无二级类型资源的兜底

部分历史资源可能没有 `resourceSubtypeId`。这类资源不阻塞属性能力上线，后端按 `resource_profile.type` 解析一级类型，展示一级类型属性并合并资源本级属性。

映射规则与前端 `RESOURCE_PARENT_TYPE_OPTIONS` 保持一致：

| resource_profile.type | parentType |
|---|---|
| Device | Device |
| IOT | IOT |
| Human | Human |
| Amenities | Amenities |
| Goods | Goods |

如果 `resource_profile.type` 不在上述枚举内，属性接口返回空列表，完整度视为 `true`（无必填属性），同时服务端记录 warning 日志便于后续数据治理。

## 总体架构

### 一期架构

一期只建设静态属性管理闭环：

| 组件 | 职责 |
|---|---|
| PostgreSQL + Prisma | 属性定义、一级类型属性、二级类型属性、资源本级属性、资源属性值、完整度 |
| Bull + Redis（已有） | 完整度异步批量重算 |
| 后端服务（NestJS） | 属性合并、权限校验、完整度计算、审计 |
| 前端 | 类型属性管理、资源属性 Tab、完整度标识 |

一期不依赖 Kafka、Redis Cluster、Elasticsearch。管理页和资源编辑页访问频率低，直接 PostgreSQL 查询和应用层合并即可。完整度异步重算复用项目已有的 Bull 队列基础设施（`BullModule.forRootAsync` 已在 `app.module.ts` 全局注册），无需引入新依赖。

### 后续扩展

| 阶段 | 能力 | 可选组件 |
|---|---|---|
| 二期 | 属性筛选、统计、批量补录 | PostgreSQL 优先，数据量大时接 ES |
| 三期 | 实时属性接入 | Kafka、Redis、PostgreSQL latest-state |
| 四期 | 百万级多属性检索和聚合 | Elasticsearch |

## 数据模型

> **实现说明：** 项目使用 PostgreSQL + Prisma ORM。以下各表的唯一约束用 Prisma schema `@@unique` 声明，迁移 SQL 由 `prisma migrate dev` 生成，无需手写 DDL。
>
> **与现有 `configJson` 的边界：** `resource_type_option.configJson` 存储的是类型**能力配置**（任务并发、舱格、待机点位等开关），属于系统行为控制字段，不属于业务属性。本方案的属性表存储的是面向业务的**自定义描述属性**（如轴距、额定功率、负责人等），两者并行存在，互不重叠，实现时不要混淆。

### 属性定义表

表名：`res_attribute_definition`

用于维护属性的基础定义。

| 字段 | 说明 |
|---|---|
| id | 主键 |
| attr_code | 属性编码，全局唯一 |
| attr_name | 属性名称 |
| data_category | 数据类别：`static` / `realtime` |
| data_type | 数据类型：`string` / `number` / `boolean` / `date` / `enum` / `json` |
| unit | 单位 |
| enum_options | 枚举选项，JSON 字符串数组，格式 `["选项A","选项B"]`，`data_type != 'enum'` 时为 null |
| required_flag | 默认是否必填，仅 static 有意义 |
| editable_flag | 默认是否允许普通用户更新，仅 static 有意义 |
| statistic_flag | 是否支持统计 |
| default_value | 默认值，格式与 `attr_value` 相同；运行时全局生效，修改后立即对所有 `attrValue=null` 的资源生效；不在绑定层存储，直接由属性定义层提供 |
| default_fills_required | 默认值是否满足必填要求：`false`（默认）= 必须显式录入才算完整；`true` = 有有效默认值即视为完整。仅对 `data_category='static'` 且 `required_flag=true` 的属性有意义 |
| enabled_flag | 是否启用 |
| remark | 备注 |
| create_time | 创建时间 |
| update_time | 更新时间 |

约束：

- `attr_code` 创建后不建议修改。
- `data_category` 创建后不可修改。
- 已存在资源属性值后，不允许随意修改 `data_type`。
- 枚举属性允许新增枚举项；删除枚举项前需检查是否已有资源使用（`res_resource_attribute_value.attr_value`），**同时**检查该枚举项是否被设为属性定义的默认值（`res_attribute_definition.default_value = removedOption`），若有则禁止删除或强制清空 `default_value`。
- `default_value` 格式必须符合 `data_type`：`number` 可解析为数字、`boolean` 为 `"true"`/`"false"`、`date` 为 ISO 8601、`enum` 为 `enum_options` 中的值、`json` 为合法 JSON；服务端在创建/更新属性定义时校验。

### 一级类型属性表

> **已优化：** 原"一级类型属性表"（`res_parent_type_attribute`）与"二级类型属性表"（`res_resource_type_attribute`）字段完全相同，且资源本级属性绑定也可复用同一结构，因此合并为一张 **`res_type_attribute_binding`**，通过 `scope_type + scope_id` 区分绑定对象。

表名：`res_type_attribute_binding`

| 字段 | 说明 |
|---|---|
| id | 主键，`cuid()` |
| scope_type | 绑定范围：`parent`（一级类型）/ `subtype`（二级类型）/ `resource`（资源实例） |
| scope_id | 绑定对象：`scope_type=parent` 时为一级类型枚举值（`Device` 等）；`scope_type=subtype` 时为 `resource_type_option.type_id`；`scope_type=resource` 时为 `resource_profile.resource_id` |
| attr_id | 属性定义 ID，对应 `res_attribute_definition.id`（**应用层关联，无数据库外键**） |
| required_flag | 当前类型下是否必填 |
| editable_flag | 当前类型下是否允许普通用户更新 |
| statistic_flag | 当前类型下是否允许统计 |
| sort_no | 排序 |
| enabled_flag | 是否启用 |
| create_time | 创建时间 |
| update_time | 更新时间 |

Prisma schema：

```prisma
model ResTypeAttributeBinding {
  id            String   @id @default(cuid())
  /// 'parent'（一级类型）| 'subtype'（二级类型）| 'resource'（资源实例，F024 实现应用层逻辑）
  /// 禁止在 DTO 中添加 @IsIn(['parent','subtype']) 枚举校验，否则 F024 上线需改校验层
  scopeType     String   @map("scope_type") @db.VarChar(16)
  /// parent→parentType枚举值（Device/IOT/…）；subtype→resource_type_option.type_id；resource→resource_profile.resource_id
  scopeId       String   @map("scope_id") @db.VarChar(128)
  attrId        String   @map("attr_id") @db.VarChar(128)
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

绑定校验（三类绑定共用同一张表，校验逻辑统一）：

- 二级类型新增绑定时，检查同 `attrId` 是否已存在 `scope_type='parent' AND scope_id=parentType AND enabled_flag=true` 的记录，有则 409。
- 资源实例新增绑定时，检查同 `attrId` 是否已存在该资源最终继承属性集合（一级 + 二级）或该资源本级活跃绑定中，有则 409。
- `@@unique([scopeType, scopeId, attrId])` 会阻止同一范围内重复写入。若同一 `scopeType/scopeId/attrId` 已存在 `enabled_flag=false` 的绑定，`POST` 不创建新记录，返回 409 并提示调用 `PATCH` 重新启用；只有 `PATCH enabledFlag=true` 能恢复该绑定。
- 服务端必须校验，不能只依赖前端。

### 二级类型属性表

> **已合并。** 二级类型属性绑定与一级类型属性绑定已统一使用 `res_type_attribute_binding` 表，通过 `scope_type='subtype'` 区分。详见上方「一级类型属性表」章节。

### 资源本级属性表

> **已合并。** 资源本级属性绑定同样使用 `res_type_attribute_binding` 表，通过 `scope_type='resource'` 区分，`scope_id` 写入 `resource_profile.resource_id`。

资源本级属性用于少量资源实例差异化字段，不作为常规建模入口。若多个资源需要同一个属性，应优先提升到二级类型；若某个一级类型都需要，应提升到一级类型。

约束：

- 资源本级属性必须引用全局属性定义，不允许创建匿名字段。
- 资源本级属性只能绑定 `data_category='static'` 且 `enabled_flag=true` 的属性定义。
- 资源本级属性不能与该资源从一级/二级类型继承来的活跃属性重复。
- 资源本级属性不能与同一资源已有活跃资源本级绑定重复。
- 资源本级属性默认只允许管理员维护；普通用户只维护属性值。
- 可按业务配置单资源绑定数量上限，一期建议限制为 20 个，避免绕过类型模型。

### 资源属性值表

表名：`res_resource_attribute_value`

用于保存 `data_category = static` 的资源属性值。

| 字段 | 说明 |
|---|---|
| id | 主键，`cuid()` |
| resource_id | 资源 ID，对应 `resource_profile.resourceId`（**应用层关联，无数据库外键**） |
| attr_id | 属性定义 ID，对应 `res_attribute_definition.id`（**应用层关联，无数据库外键**） |
| attr_value | 原始属性值，字符串存储。规范：`number`→`"120.5"`；`boolean`→`"true"`/`"false"`；`date`→ISO 8601；`enum`→枚举值字符串；`json`→JSON 字符串 |
| editable_flag | 实例级锁定标记，独立于类型绑定层策略 |
| value_source | 来源：`manual` / `import` / `sync` / `system` / `force` |
| updated_by | 最后更新人 |
| create_time | 创建时间 |
| update_time | 更新时间 |

Prisma schema：

```prisma
model ResResourceAttributeValue {
  id           String   @id @default(cuid())
  resourceId   String   @map("resource_id") @db.VarChar(128)
  attrId       String   @map("attr_id") @db.VarChar(128)
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

说明：

- 不冗余 `parent_type` 或 `resource_subtype_id`，避免资源改类型时出现不一致。
- **一期不保留 `value_number / value_date / value_json` 冗余字段**，二期统计/筛选时再通过 migration 追加。
- 不保存业务历史版本，更新时直接覆盖当前值。

### 资源属性审计（复用现有 operation_audit_log）

**不新建审计表。** 项目已有 `operation_audit_log` 表，字段完备（含 `entityType`、`entityId`、`beforeJson`、`afterJson`、`operatorUserId`、`storeId` 等），通过 `AuditService.recordEntityChange` 写入，所有已有模块（resource-type、resource 等）均已接入。

管理员强制更新时，将属性变更作为一次实体变更事件写入：

| 字段 | 写入值 |
|---|---|
| `entityType` | `resourceAttribute` |
| `entityId` | `{resourceId}:{attrId}` |
| `action` | `forceUpdate` |
| `beforeJson` | `{ attrValue: 旧值 }`；首次写入（记录不存在）时为 `null` |
| `afterJson` | `{ attrValue: 新值, reason }` |
| `operatorUserId` | 当前操作人 |

强制更新的属性值修改和审计写入必须在同一 Prisma 事务内完成（`prisma.$transaction`）。

### 实时属性状态表（后续）

表名：`res_resource_realtime_state`

三期接入实时属性时再建设。

| 字段 | 说明 |
|---|---|
| id | 主键 |
| resource_id | 资源 ID |
| attr_id | 属性定义 ID |
| attr_value | 原始值 |
| value_number | 数值型冗余值 |
| value_json | JSON 型冗余值 |
| report_time | 设备上报时间 |
| update_time | 写库时间 |

约束（Prisma schema）：

```prisma
@@unique([resourceId, attrId])
```

## 属性继承与合并

### 合并输入

合并某个资源或二级类型的最终属性时，输入为：

- `parentType`：
  - 若 `resourceSubtypeId` 有值，来自 `resource_type_option.parentType`。
  - 若 `resourceSubtypeId` 为空，来自 `resource_profile.type` 到一级类型枚举的映射。
- `resourceSubtypeId`：来自 `resource_type_option.typeId`；历史资源可为空。
- `resourceId`：计算资源实例最终属性时传入；仅计算二级类型最终属性时可为空。

### 合并算法

```text
// 只合并 data_category = 'static' 的属性；realtime 属性三期接入时另行处理
// b. = res_type_attribute_binding，d. = res_attribute_definition
parentAttrs = 查询 res_type_attribute_binding b
              JOIN res_attribute_definition d ON b.attr_id = d.id
              WHERE b.scope_type = 'parent'
                AND b.scope_id = parentType
                AND b.enabled_flag = true   -- 绑定关系启用
                AND d.enabled_flag = true   -- 属性定义全局启用
                AND d.data_category = 'static'
              ORDER BY b.sort_no ASC

if resourceSubtypeId 有值:
    ownAttrs = 查询 res_type_attribute_binding b
               JOIN res_attribute_definition d ON b.attr_id = d.id
               WHERE b.scope_type = 'subtype'
                 AND b.scope_id = resourceSubtypeId
                 AND b.enabled_flag = true      -- 绑定关系启用
                 AND d.enabled_flag = true      -- 属性定义全局启用
                 AND d.data_category = 'static'
               ORDER BY b.sort_no ASC
else:
    ownAttrs = []

if resourceId 有值:
    resourceOwnAttrs = 查询 res_type_attribute_binding b
                       JOIN res_attribute_definition d ON b.attr_id = d.id
                       WHERE b.scope_type = 'resource'
                         AND b.scope_id = resourceId
                         AND b.enabled_flag = true
                         AND d.enabled_flag = true
                         AND d.data_category = 'static'
                       ORDER BY b.sort_no ASC
else:
    resourceOwnAttrs = []

// 排序规则：一级继承属性组整体在前，二级本级属性组第二，资源本级属性组最后。
// 各组内部按 sort_no 升序，不支持跨组交叉排序。
merged = parentAttrs（按 sort_no 升序，整体作为第一分组）
for attr in ownAttrs 按 sort_no 升序:
    if attr.attr_id 已存在于 parentAttrs:
        // 防御性断言：正常流程下此路径不可达。
        // 写入时已有两道防线：POST /attributes 的重复绑定校验（见"属性维护规则"章节），
        // 以及重新启用父级绑定时的冲突检查（见"重新启用父级绑定的冲突校验"章节）。
        // 若此处触发，说明存在数据旁路写入或程序 Bug，必须抛错（而非静默跳过）以暴露问题。
        logger.error(`属性合并冲突：resourceSubtypeId=${attr.resourceSubtypeId} 的 attrId=${attr.attrId} 与一级类型重复绑定`)
        throw new AppException('ATTR_BINDING_CONFLICT', `属性绑定冲突，禁止重复绑定 attrId=${attr.attrId}`, 409)
    else:
        加入 merged（追加到继承属性组之后，作为第二分组）

for attr in resourceOwnAttrs 按 sort_no 升序:
    if attr.attr_id 已存在于 merged:
        // 防御性断言：资源本级绑定写入时必须拦截与一级/二级类型属性重复。
        logger.error(`资源本级属性合并冲突：resourceId=${resourceId} 的 attrId=${attr.attrId} 与继承属性重复绑定`)
        throw new AppException('ATTR_BINDING_CONFLICT', `资源本级属性绑定冲突，禁止重复绑定 attrId=${attr.attrId}`, 409)
    else:
        加入 merged（追加到资源本级属性组，作为第三分组）
```

返回字段需要包含：

| 字段 | 说明 |
|---|---|
| attrId | 属性 ID |
| attrCode | 属性编码 |
| attrName | 属性名称 |
| dataCategory | 一期只返回 `static`；三期实时链路接入后支持 `realtime` |
| dataType | 数据类型 |
| unit | 单位，`data_type` 为 `number` 时展示用，无单位返回 null |
| enumOptions | 字符串数组（`["选项A","选项B"]`），`data_type` 为 `enum` 时前端渲染下拉控件，value 与 label 相同；其他类型返回 null |
| required | 是否必填 |
| editable | 普通用户是否可编辑 |
| statistic | 是否支持统计 |
| sourceScope | `parent` / `subtype` / `resource` |
| sourceName | 一级类型名称（服务端通过枚举映射表转换：`Device`→`机器人`、`IOT`→`IOT`、`Human`→`人员`、`Amenities`→`客需品`、`Goods`→`商品`）、二级类型名称（从 `resource_type_option.name` 读取）或资源名称（从 `resource_profile.name` 读取） |
| relationId | 绑定关系 ID |

**`relationId` 说明：**

合并后 `relationId` 统一来自 `res_type_attribute_binding.id`（`cuid` 字符串，全局唯一），前端调用 PATCH / DELETE 时仍按 `sourceScope` 路由到不同接口：

| sourceScope | relationId 来自 | 前端操作接口 |
|---|---|---|
| `parent` | `res_type_attribute_binding.id` | `/internal/resource-parent-types/{parentType}/attributes/{relationId}` |
| `subtype` | `res_type_attribute_binding.id` | `/internal/resource-types/{typeId}/attributes/{relationId}` |
| `resource` | `res_type_attribute_binding.id` | `/internal/resources/{resourceId}/attribute-bindings/{relationId}` |

两者均来自同一张表，ID 全局唯一，不存在旧方案中的数值重叠问题。

### 缓存策略

一期可以不做 Redis 缓存。若接口压力增大，可加本地缓存或 Redis：

```text
resource_type_attrs:{parentType}:{resourceSubtypeId}
resource_attrs:{resourceId}
parent_type_attrs:{parentType}
subtype_own_attrs:{resourceSubtypeId}
resource_own_attrs:{resourceId}
```

失效规则：

- 修改一级类型属性：失效该 `parentType` 下所有二级类型 merged cache。
- 修改二级类型属性：只失效该 `resourceSubtypeId` 的 merged cache。
- 修改资源本级属性：只失效该 `resourceId` 的 merged cache。
- 修改属性定义：失效引用该 `attr_id` 的相关 cache，需要先反查所有引用方：

```text
1. SELECT DISTINCT scope_id FROM res_type_attribute_binding
   WHERE scope_type = 'parent' AND attr_id = #{attrId}
   → 失效这些 parent_type_attrs:{parentType}
   → 失效这些 parentType 下所有二级类型的 resource_type_attrs:{parentType}:{subtypeId}
       （SELECT type_id FROM resource_type_option WHERE parent_type IN (...)）

2. SELECT DISTINCT b.scope_id, rto.parent_type
   FROM res_type_attribute_binding b
   JOIN resource_type_option rto ON b.scope_id = rto.type_id
   WHERE b.scope_type = 'subtype' AND b.attr_id = #{attrId}
   → 失效这些 subtype_own_attrs:{scopeId}
   → 失效这些 resource_type_attrs:{parentType}:{scopeId}
```

属性定义修改频率极低，多次反查 SQL 开销可以接受。

不需要 `type_children:{typeId}`，不需要 `path LIKE` 查询。

## 属性维护规则

### 新增属性

支持两种方式：

- 新建属性定义后绑定到一级或二级类型。
- 引用已有属性定义并绑定到一级或二级类型。

新增类型属性关系时，从属性定义表复制默认 flag：

- `required_flag`
- `editable_flag`
- `statistic_flag`

之后类型绑定层独立维护这些 flag，不反向影响属性定义。

新增资源本级属性关系也从属性定义表复制默认 flag，之后资源本级绑定层独立维护这些 flag，不反向影响属性定义，也不反向影响一级/二级类型绑定。

> **`default_value` 不参与绑定层复制**：`default_value` 存储在 `res_attribute_definition`，运行时直接读取，不复制到 `res_type_attribute_binding`。修改属性定义的 `default_value` 后，所有 `attrValue=null` 的资源立即使用新默认值，无需任何绑定层操作。

### 二级类型重复绑定校验

二级类型绑定属性时，如果该属性已经通过一级类型继承（父绑定 `enabled_flag = true`），禁止重复绑定。

错误提示：

```text
该属性已通过一级类型继承，如需修改配置请前往一级类型属性维护。
```

### 资源本级重复绑定校验

资源实例绑定本级属性时，如果该属性已经通过一级或二级类型继承，或该资源已经存在同一 `attrId` 的活跃本级绑定，禁止重复绑定。

错误提示：

```text
该属性已存在于当前资源的属性集合中，如需修改配置请前往对应来源维护。
```

资源本级属性不是覆盖机制，不允许用资源本级绑定修改继承属性的 `required_flag`、`editable_flag`、`statistic_flag` 或排序。若确实需要例外，应先评估是否应调整二级类型建模。

若同一资源曾经绑定过同一属性但当前绑定已软禁用，新增时不自动复用，服务端返回 409，提示管理员在本级属性列表中重新启用该绑定。重新启用时仍需执行继承属性冲突校验：如果当前一级/二级类型已经提供该 `attrId`，禁止启用。

### 重新启用父级绑定的冲突校验

当一级类型属性绑定从 `enabled_flag = false` 改回 `true` 时，必须检查该 `parentType` 下所有二级类型是否有本级活跃绑定指向同一 `attr_id`：

```text
SELECT scope_id
FROM res_type_attribute_binding
WHERE scope_type = 'subtype'
  AND attr_id = #{attrId}
  AND enabled_flag = true
  AND scope_id IN (
    SELECT type_id FROM resource_type_option WHERE parent_type = #{parentType}
  )
```

若查询返回任何记录，**禁止启用**，返回 409：

```text
以下二级类型已独立绑定该属性，请先解除再启用继承：[typeA, typeB]
```

若查询为空，正常启用：
1. 失效该 `parentType` 下所有 merged cache（等同"修改一级类型属性"失效规则）。
2. 若重新启用的绑定 `required_flag = true`，向 `COMPLETENESS_QUEUE` 投递 `{ scope: 'parentType', id: parentType }` Bull job（触发路径与"一级类型新增必填属性"一致，见完整度更新策略表）。
3. 若 `required_flag = false`，不触发完整度重算。

### 删除类型属性

删除一级或二级类型属性关系时，采用**保留属性值**策略，避免误删类型配置导致资源历史数据永久丢失。

删除规则：

1. 只删除类型与属性的绑定关系。
2. 不删除属性定义，`res_attribute_definition` 记录保留。
3. 不删除已有 `res_resource_attribute_value` 记录。
4. 残留属性值成为孤儿值，默认不可见、不可编辑、不参与统计。**服务端过滤**：`GET /internal/resources/{resourceId}/attributes` 只返回当前资源最终属性集合内的 attrId，孤儿值不出现在响应中，前端无感知。
5. 若删除的是必填 static 属性，需要重算受影响资源完整度。

前端确认提示：

```text
删除属性「轴距」后，该属性将不再展示和参与完整度计算；已有资源属性值会保留为隐藏数据。确认删除？
```

事务建议：

```ts
// 删除二级类型属性绑定（scope_type='subtype'）
await prisma.$transaction(async (tx) => {
  await tx.resTypeAttributeBinding.delete({ where: { id: relationId } });
  await tx.operationAuditLog.create({
    data: {
      entityType: 'typeAttributeBinding',
      entityId: String(relationId),
      action: 'deleteBinding',
      beforeJson: oldRelationSnapshot,
    },
  });
});

// 删除一级类型属性绑定（scope_type='parent'）：逻辑相同，entityType 同样为 'typeAttributeBinding'
```

完整度重算：

- 删除二级类型必填属性：受影响范围为该二级类型资源，事务提交后投递异步重算任务。
- 删除一级类型必填属性：受影响范围为该 `parentType` 下所有资源，事务提交后投递异步重算任务。
- 删除资源本级必填属性：受影响范围为该资源本身，事务提交后同步重算该资源完整度。

为保证查询一致性，资源属性查询、更新、统计都必须先计算当前资源所属类型的最终属性集合，只允许处理集合内的 `attrId`。

## 属性值更新规则

`editable_flag` 有两层含义：

- 类型属性层：策略层，定义该类型下这类属性是否允许普通用户编辑。
- 资源属性值层：实例层，定义某个资源的这条属性值是否被锁定。

普通用户更新 static 属性时权限校验逻辑：

```text
记录已存在（UPDATE）：
  类型属性 editable_flag = true
  AND 资源属性值 editable_flag = true

记录不存在（INSERT，首次写入）：
  只检查类型属性 editable_flag = true
  （实例层记录不存在，权限以类型层为准；INSERT 成功后实例层 editable_flag 从类型绑定层复制）
```

管理员强制更新：

- 需要具备强制更新权限。
- 可绕过 editable 限制。
- 必须填写原因。
- 必须通过 `AuditService.recordEntityChange` 写入 `operation_audit_log`，与属性值更新在同一事务内。

realtime 属性不允许人工编辑，只读展示。

## 属性默认值

### 设计原则

默认值是**查询时 fallback**，不是存储态。`res_resource_attribute_value` 只在用户/系统显式写入时才创建记录；未写入时默认值在查询层动态补全，不预先写入 DB。

### 存储位置

| 字段 | 位置 | 用途 |
|---|---|---|
| `default_value` | `res_attribute_definition` | **唯一来源**：创建属性定义时录入，运行时直接读取，对所有绑定该属性的资源全局生效 |
| `default_fills_required` | `res_attribute_definition` | 控制"有默认值但无显式录入"时是否满足完整度要求 |

**有效默认值计算：**

```typescript
// src/modules/attribute/attribute-merge.util.ts
export function resolveEffectiveDefault(definitionDefaultValue: string | null): string | null {
  return definitionDefaultValue; // 运行时直接读属性定义层，无中间层
}
```

`res_type_attribute_binding` **不存储** `default_value`。

### 查询行为

`GET /internal/resources/{resourceId}/attributes` 中，无 `res_resource_attribute_value` 记录时：

```text
attrValue    = null（DB 实际状态，永远如实反映）
defaultValue = definition.default_value（null 则无默认值）
```

前端判断 `attrValue === null && defaultValue !== null` 时展示"（默认值）"角标，表单输入框预填 `defaultValue`。

### `isAttributeFilled` 函数

所有完整度计算路径（同步 + 异步 Bull processor）统一调用此函数，消除两条路径的逻辑分歧：

```typescript
// src/modules/attribute/completeness-calc.util.ts
export function isAttributeFilled(
  attrValue: string | null | undefined,
  definitionDefaultValue: string | null,   // 来自 res_attribute_definition.default_value
  defaultFillsRequired: boolean,            // 来自 res_attribute_definition.default_fills_required
): boolean {
  if (attrValue != null && attrValue.trim() !== '') return true;
  if (definitionDefaultValue != null && defaultFillsRequired) return true;
  return false;
}
```

### 默认值变更的传播

- 修改 `definition.default_value`：立即对**所有绑定了该属性且 `attrValue=null`** 的资源生效（查询时即刻体现新默认值）。若 `default_fills_required=true` 且修改前后跨越"有默认值/无默认值"边界（如从非空改为 null 或反向），需触发批量完整度重算（见完整度更新策略表）。
- 修改 `definition.default_fills_required`：影响所有绑定了该属性的资源完整度，需触发批量重算（见完整度更新策略表）。

## 资源属性初始化与完整度

### 新建资源

资源写入 `resource_profile` 时，**在同一 Prisma 事务内**计算最终 static 属性集合并执行初始化：

- `resourceSubtypeId` 有值：按“一级类型属性 + 二级类型本级属性”初始化。
- `resourceSubtypeId` 为空：按 `resource_profile.type` 对应的一级类型属性初始化。

初始化规则：

- 必填 static 属性创建空值记录（`attr_value = null`，`value_source = 'system'`），使用 `prisma.resResourceAttributeValue.createMany({ data: [...], skipDuplicates: true })`，保证幂等（事务重试或历史孤儿值存在时均不覆盖已有记录）。
- 非必填 static 属性不创建空记录，查询展示时按定义补空。
- realtime 属性不初始化。

**`res_resource_attribute_value.editable_flag` 初始值来源：**

实例层 `editable_flag` 初始值从**当前来源绑定层**复制（而非属性定义层），确保一级/二级类型管理员或资源管理员的策略正确落到实例：

```text
1. sourceScope = parent  → 从 res_type_attribute_binding（scope_type='parent'）的 editable_flag 复制
2. sourceScope = subtype → 从 res_type_attribute_binding（scope_type='subtype'）的 editable_flag 复制
3. sourceScope = resource → 从 res_type_attribute_binding（scope_type='resource'）的 editable_flag 复制
```

绑定层后续修改 `editable_flag` 时，不回溯修改已有实例的锁定状态；查询权限时以**当前来源绑定层 AND 实例层**的结果为准。

> **已知不对称行为（设计决策）：**
> - 类型层**收紧**（`true→false`）：实例层保持旧值（可能是 true），AND 结果 = false，**立即对所有已有实例生效**。
> - 类型层**放开**（`false→true`）：实例层保持旧值（false），AND 结果仍为 false，**对已有实例不生效**，仅新建资源后续会以新策略初始化。
>
> 管理员若需对已有资源放开编辑权限，必须逐个或通过批量工具对 `res_resource_attribute_value.editable_flag` 执行强制更新（记录审计日志）。这是有意设计：实例层 `editable_flag` 代表"该记录是否被管理员显式锁定/解锁"，不自动随类型策略漂移。

### 属性绑定新增后已有值记录的处理

当一级、二级类型或资源本级新增属性绑定时，已存在的资源实例不会被立即补录 `res_resource_attribute_value` 记录（该补录行为延至二期异步批量处理）。查询层必须处理"有属性定义、无值记录"的情况。

无二级类型资源同样适用该规则：一级类型新增属性后，这类资源按 `resource_profile.type = parentType` 纳入受影响范围。

资源本级新增属性同样适用该规则：只影响当前资源，查询时返回该属性且 `attrValue = null`。

**查询合并规则：**

```text
资源属性查询结果 = 资源最终属性定义（全量） LEFT JOIN 已有属性值记录（部分）
```

对于有属性定义但无 `res_resource_attribute_value` 记录的属性：

| 字段 | 取值规则 |
|---|---|
| `attrValue` | 返回 null，前端统一展示占位符 |
| `editable` | 以当前来源绑定层 `editable_flag` 为准（规则同"新建资源 > editable_flag 初始值来源"章节），与有记录时的合并计算规则保持一致 |
| `valueSource` | 返回 null（无记录时字段存在但值为 null，与响应 Schema 结构一致） |
| `updatedBy` | 返回 null |

此规则同时适用于必填属性（**新增绑定后的已有资源实例**：一期不补创空记录，二期异步补录后转为有记录）和非必填属性（永久按需补空，不主动创建记录）。注意：新建资源时继承来的必填属性**会**创建空记录；资源本级绑定通常发生在资源已存在之后，因此按本节规则处理。

**完整度影响：**

一级/二级类型新增必填属性后，受影响资源的 `profile_complete_flag` 由后台异步批量重算；资源本级新增必填属性后，同步重算当前资源。完整度由 `isAttributeFilled(attrValue, bindingDefaultValue, defaultFillsRequired)` 判定：

- 若该属性 `definition.default_value` 非空且 `definition.default_fills_required = true`，则即使无值记录，`isAttributeFilled = true`，完整度不降级。
- 否则，无值记录视为空，完整度标记为未完善；用户在属性 Tab 填写并保存后，首次写入 `res_resource_attribute_value` 记录，完整度同步重算。

### 完整度计算

完整度只看 static 必填属性，所有路径（同步/异步）统一调用 `isAttributeFilled()`：

```typescript
// 对每条 required_flag=true 且 data_category='static' 的属性执行：
isAttributeFilled(
  attrValueRecord?.attrValue ?? null,   // res_resource_attribute_value.attr_value，无记录时为 null
  definition.defaultValue,               // res_attribute_definition.default_value，全局唯一来源
  definition.defaultFillsRequired,       // res_attribute_definition.default_fills_required
)
// true  → 该属性算作已填写
// false → 该属性算作缺失，进入 missingAttributes

完整   = 所有 required=true 属性的 isAttributeFilled 均为 true
未完善 = 任意 required=true 属性的 isAttributeFilled 为 false
```

`missingAttributes` 只列 `isAttributeFilled = false` 的属性；有有效默认值且 `default_fills_required=true` 的属性不进入该列表。

> **`required_flag` 来源：** 完整度使用**当前来源绑定层**的 `required_flag`（`res_type_attribute_binding.required_flag`，覆盖 `parent` / `subtype` / `resource` 三类 scope），而非 `res_attribute_definition.required_flag`。绑定层创建时从定义层复制，之后独立维护，两者可能不同，以绑定层为准。
>
> **空串处理：** `attr_value = ''` 或纯空白视为未填写，不计入完整（`isAttributeFilled` 内 `trim` 判空）。前端保存属性值时应在提交前 trim 并拒绝纯空白提交，后端同样在写入前 trim 校验必填属性。
>
> **Bull processor 实现：** `mergeTypeAttributes` 已 JOIN `res_attribute_definition`，需额外取 `defaultValue` 和 `defaultFillsRequired` 两个字段。processor 对每个 required 属性调用 `isAttributeFilled()` 而非直接判 `attrValue IS NULL`。

`resource_profile` 必须新增完整度字段，通过 Prisma migration 完成。建议字段可空，避免上线初期存量资源在重算完成前全部显示为“未完善”：

```prisma
// prisma/schema.prisma - ResourceProfile model 新增字段
profileCompleteFlag Boolean? @map("profile_complete_flag")
```

执行 `prisma migrate dev --name add_profile_complete_flag` 生成迁移。存量数据初始为 `null`，表示”未计算”。上线流程：

1. 后端先上线字段和重算任务，不在前端展示角标。
2. 运行全量完整度 backfill（分两步，详见任务文档 F022）：
   - 对 5 个已知 parentType 各投递一个 Bull job（processor 分批处理）。
   - 直接 Prisma `updateMany` 将 `type NOT IN (已知枚举)` 且 `profileCompleteFlag = null` 的历史资源设置为 `true`（这类资源没有必填属性，`completeness-calc.util.ts` 已有对应 warning + true 返回，backfill 脚本不经过 Bull 直接 DB 写入）。
   - 验证：`SELECT COUNT(*) FROM resource_profile WHERE profile_complete_flag IS NULL AND is_deleted = false;` 结果为 0 才可进入步骤 3。
3. backfill 完成后，前端再展示”资料未完善”角标和筛选。
4. 若列表接口在过渡期返回 `null`，前端展示为”计算中”或不展示角标，不允许把 `null` 当作 `false`。

更新策略：

| 触发场景 | 更新方式 |
|---|---|
| 单条资源属性值更新 | 同步重算该资源 |
| 资源创建 | 同步初始化并计算 |
| 资源本级新增必填属性 | 同步重算该资源 |
| **二级**类型新增必填属性 | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| **一级**类型新增必填属性 | 事务提交后投递 `{ scope: 'parentType', id: parentType }`（影响该 parentType 下所有资源） |
| 删除**资源本级**必填属性 | 同步重算该资源 |
| 删除**二级类型**必填属性 | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| 删除**一级类型**必填属性 | 事务提交后投递 `{ scope: 'parentType', id: parentType }`（影响该 parentType 下所有资源，数量可能大） |
| 资源本级修改 `required_flag` | 同步重算该资源 |
| **二级**类型修改 `required_flag` | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| **一级**类型修改 `required_flag` | 事务提交后投递 `{ scope: 'parentType', id: parentType }` |
| 资源本级属性绑定重新启用（`enabled_flag: false→true`），且 `required_flag=true` | 同步重算该资源 |
| **一级**类型属性绑定重新启用（`enabled_flag: false→true`），且 `required_flag=true` | 事务提交后投递 `{ scope: 'parentType', id: parentType }`（触发路径与"一级类型新增必填属性"一致） |
| **二级**类型属性绑定重新启用（`enabled_flag: false→true`），且 `required_flag=true` | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| 资源本级属性绑定软禁用（`enabled_flag: true→false`），且 `required_flag=true` | 同步重算该资源 |
| **一级**类型属性绑定软禁用（`enabled_flag: true→false`），且 `required_flag=true` | 事务提交后投递 `{ scope: 'parentType', id: parentType }`（必填属性从合并结果消失，受影响资源可能从"未完善"变为"完善"） |
| **二级**类型属性绑定软禁用（`enabled_flag: true→false`），且 `required_flag=true` | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| 资源二级类型变更 | 同步重算该资源（单台，代价低） |
| `definition.default_value` 变更，且 `default_fills_required=true`，且跨越"有值/null"边界 | 查询该 attrId 所有 `scope_type='parent'` 的绑定，对每个 parentType 投递 `{ scope: 'parentType', id }`（影响面可能大，需防抖或合并投递） |
| `definition.default_fills_required` 变更（任意方向） | 同上，全量重算绑定了该属性的所有资源 |

所有异步行均投递到 `COMPLETENESS_QUEUE`，processor 处理逻辑见下方。

异步重算模式参照项目已有的 `DiscoveryModule` 实现：新建 `CompletenessModule`，注册 `BullModule.registerQueue({ name: 'completeness' })`，processor 接收 `{ scope: 'subtype' | 'parentType', id: string }` job，批量查询受影响资源后逐条计算并更新 `profileCompleteFlag`。

processor 受影响资源查询逻辑：

```text
scope = 'subtype'：
  SELECT resource_id FROM resource_profile
  WHERE resource_subtype_id = id AND status != 'deleted'

scope = 'parentType'：
  // 第一步：查该 parentType 下所有二级类型 ID
  subtypeIds = SELECT type_id FROM resource_type_option WHERE parent_type = id
  // 第二步：查这些二级类型下的所有资源，以及没有 resource_subtype_id 但 resource_profile.type = parentType 的历史资源
  SELECT resource_id FROM resource_profile
  WHERE (
      resource_subtype_id IN (subtypeIds)
      OR (resource_subtype_id IS NULL AND type = id)
    )
    AND status != 'deleted'
```

`scope = 'parentType'` 影响资源数量可能较大，processor 内应分批处理（每批建议 200 条），避免单次事务过长。

### 资源二级类型变更

当前前端 `ResourceEditModal` 支持提交 `resourceSubtypeId`，因此方案不能假设资源类型不可变。

类型变更规则（差异合并，旧值保留为孤儿值）：

设旧类型最终属性集合为 `oldAttrs`，新类型最终属性集合为 `newAttrs`：

```text
旧类型独有 = oldAttrs - newAttrs → 保留属性值，但变为孤儿值，不再展示/编辑/统计；editable_flag 无需更新（孤儿值不可编辑）
共有属性 = oldAttrs ∩ newAttrs  → 保留 attr_value；editable_flag 更新为新类型绑定层值（见下方步骤说明）
新类型独有 = newAttrs - oldAttrs → 必填属性：若记录不存在则 INSERT 空值（attr_value=null, value_source='system'）；若记录已存在（A→B→A 孤儿值重激活场景），保留历史值；无论是否已有记录，editable_flag 均更新为新类型绑定层值（见下方步骤说明）
```

**类型变更前置冲突校验：**

资源本级属性不是覆盖机制。变更二级类型前，服务端必须先计算：

```text
resourceOwnAttrIds = 当前资源 scope_type='resource' 且 enabled_flag=true 的 attrId
newInheritedAttrIds = 新 parent/subtype 最终继承属性 attrId（不含 resource scope）
conflicts = resourceOwnAttrIds ∩ newInheritedAttrIds
```

若 `conflicts` 非空，禁止类型变更，返回 409：

```text
新类型已包含以下资源本级属性，请先删除或停用资源本级绑定后再变更类型：[属性A, 属性B]
```

不自动禁用资源本级绑定，避免隐藏数据和权限策略被静默改变。

**类型变更事务执行步骤（三步按序，同一事务）：**

```text
Step 1 — 批量 UPDATE editable_flag（newAttrs 全集，含共有属性 + 孤儿值重激活属性）：
  UPDATE res_resource_attribute_value
  SET editable_flag = <新类型绑定层 editable_flag>
  WHERE resource_id = #{resourceId}
    AND attr_id IN (newAttrs 的所有 attrId)
  （每个 attrId 的 editable_flag 来源：sourceScope=parent/subtype/resource 均取 res_type_attribute_binding）

Step 2 — INSERT 新类型必填属性中缺失的空值记录（仅新类型独有必填属性）：
  使用 createMany({ data: missingMandatoryAttrs, skipDuplicates: true })
  （skipDuplicates 保证孤儿值重激活场景不覆盖历史 attr_value；editable_flag 已在 Step 1 更新，此处不需再设）

Step 3 — UPDATE resource_profile.resource_subtype_id = #{newSubtypeId}
```

> `createMany({ skipDuplicates: true })` 只负责 INSERT 缺失记录，不更新已有记录。Step 1 的 UPDATE 必须在 Step 2 之前或独立执行，确保孤儿值的 `editable_flag` 也得到刷新。

新类型必填属性初始化、资源本级冲突校验与 `resourceSubtypeId` 字段更新在同一事务内完成，事务提交后同步重算该资源完整度。

前端在提交类型变更前，展示影响提示：

```text
变更类型后，N 个旧类型独有属性将不再展示，已有属性值会保留为隐藏数据。确认继续？
```

服务端约束（查询一致性通用规则见"属性维护规则 > 删除类型属性"末段）：

- 属性更新接口必须校验 `attrId` 属于变更后新类型的最终属性集合，否则返回 400/409。
- 类型变更与属性更新并发时，以事务提交时资源当前类型为准，禁止写入旧类型独有属性。

如果后续业务要求资源类型不可变，应在前后端同时禁止编辑 `resourceSubtypeId`。

## 后端接口建议

接口统一使用当前项目的 `/internal` 资源语义，不使用 `/devices`。

### 属性定义

```text
GET    /internal/attribute-definitions?keyword=&dataCategory=&enabledFlag=
POST   /internal/attribute-definitions
GET    /internal/attribute-definitions/{attrId}
PATCH  /internal/attribute-definitions/{attrId}
```

说明：

- `GET /attribute-definitions` 支持过滤参数：`keyword`（按 `attr_name` / `attr_code` 模糊匹配）、`dataCategory`（`static` / `realtime`）、`enabledFlag`（`true` / `false`；不传则返回全部）。前端 Select 搜索使用 `?keyword=` 参数。
- **不提供 DELETE 接口**。属性定义的"停用"通过 `PATCH enabled_flag=false` 实现（软删除）。停用后该属性不再出现在类型合并结果中，已有绑定关系记录保留。若需彻底清除，需先确认没有活跃绑定（`res_type_attribute_binding` 中无 `scope_type` 匹配且 `enabled_flag=true` 的记录），再由 DBA 手动操作，不走接口。
- `PATCH` 时约束：`data_category` 不可修改；已存在 `res_resource_attribute_value` 记录后，`data_type` 修改需二次确认（由服务端拒绝或告警，一期建议直接拒绝）；`attr_code` 不建议修改（前端应禁用该字段）。
- **`data_category = 'realtime'` 的属性定义一期允许创建**（提前建库为三期做准备），但 `POST /attributes`（一级或二级类型属性绑定）服务端校验 `dataCategory != 'static'` 时返回 400，禁止绑定到类型。前端新增绑定时的定义搜索 Select 默认只展示 `dataCategory='static'` 且 `enabledFlag=true` 的定义。

### 一级类型属性

```text
GET    /internal/resource-parent-types/{parentType}/attributes
POST   /internal/resource-parent-types/{parentType}/attributes
PATCH  /internal/resource-parent-types/{parentType}/attributes/{relationId}
DELETE /internal/resource-parent-types/{parentType}/attributes/{relationId}
```

说明：

- `GET /attributes` 默认只返回 `data_category = 'static'` 的属性（`b.enabled_flag = true AND d.enabled_flag = true`），与合并算法保持一致。三期实时属性接入后再通过 `?category=realtime` 参数扩展。
- `POST /attributes` 服务端校验两条规则（与二级类型属性 POST 规则相同）：① **只允许绑定 `data_category = 'static'` 的属性定义**；② **只允许绑定 `enabled_flag = true` 的属性定义**，违反返回 400。

### 二级类型属性

```text
GET    /internal/resource-types/{typeId}/attributes
GET    /internal/resource-types/{typeId}/own-attributes
GET    /internal/resource-types/{typeId}/inherited-attributes
POST   /internal/resource-types/{typeId}/attributes
PATCH  /internal/resource-types/{typeId}/attributes/{relationId}
DELETE /internal/resource-types/{typeId}/attributes/{relationId}
```

说明：

- `/attributes` 返回最终属性：一级继承 + 二级本级，排序规则为继承属性组整体在前，本级属性组整体在后。
- `/own-attributes` 只返回二级本级属性（`sourceScope = subtype`），供"本级属性" Tab 使用。
- `/inherited-attributes` 只返回从一级类型继承的属性（`sourceScope = parent`），供"继承自一级类型" Tab 使用。实现上等价于查询该二级类型对应 `parentType` 的 `res_parent_type_attribute` 并附加 `sourceScope = parent` 标记；复用一级类型属性查询逻辑，不新增数据库表。
- `POST /attributes` 服务端校验两条规则：① **只允许绑定 `data_category = 'static'` 的属性定义**，禁止绑定 `realtime`（三期上线后放开）；② **只允许绑定 `enabled_flag = true` 的属性定义**，传入已停用属性定义 ID 返回 400（停用定义无法出现在合并结果中，绑定无意义）。同理，`POST /resource-parent-types/{parentType}/attributes` 执行相同校验。
- `PATCH /attributes/{relationId}` **不允许修改 `attr_id`**：`attr_id` 是绑定关系的业务主体，修改等于换绑，需先 DELETE 再 POST。PATCH 只允许修改 `required_flag`、`editable_flag`、`statistic_flag`、`sort_no`、`enabled_flag`；服务端校验，若请求体包含 `attrId` 字段一律忽略或报 400。
- 同理，`PATCH /resource-parent-types/{parentType}/attributes/{relationId}` 也不允许修改 `attr_id`，规则相同。

**删除二级类型时的属性绑定处理：**

`DELETE /internal/resource-types/{typeId}` 执行时，按以下顺序校验：

**Step 1 — 资源引用校验**（先于绑定校验）：
检查是否有资源的 `resource_subtype_id = typeId`（`status != 'deleted'`），若有则**禁止删除**，返回 409：

```text
该类型下仍有 N 个资源，请先将这些资源变更至其他类型后再删除。
```

**Step 2 — 属性绑定校验**：
若 `res_type_attribute_binding` 中存在该 `typeId` 的活跃绑定（`scope_type='subtype'` 且 `enabled_flag=true`），**禁止删除**，返回 409：

```text
该类型已绑定 N 个属性，请先删除所有属性绑定后再删除类型。
```

两项校验均通过后，允许删除类型，级联删除 `res_type_attribute_binding` 中该 `typeId`（`scope_type='subtype'`）的所有记录（无论 `enabled_flag`），在同一事务内完成。

### 资源实例属性

```text
GET    /internal/resources/{resourceId}/attributes
GET    /internal/resources/{resourceId}/attribute-bindings
POST   /internal/resources/{resourceId}/attribute-bindings
PATCH  /internal/resources/{resourceId}/attribute-bindings/{relationId}
DELETE /internal/resources/{resourceId}/attribute-bindings/{relationId}
PATCH  /internal/resources/{resourceId}/attributes/{attrId}
PATCH  /internal/resources/{resourceId}/attributes/{attrId}/force
GET    /internal/resources/{resourceId}/profile-completeness
```

接口语义：

- `GET /attributes` 返回资源最终属性：一级继承 + 二级本级 + 资源本级，并 LEFT JOIN 当前资源属性值。
- `GET /attribute-bindings` 只返回资源本级属性绑定（`sourceScope = resource`），供资源编辑弹窗中的"本级属性"管理区使用。该接口返回绑定配置，不返回属性值字段。
- `POST /attribute-bindings` 新增资源本级属性绑定，只允许绑定 `static` 且启用的属性定义，并执行资源本级重复绑定校验。
- `POST /attribute-bindings` 如遇同一 `scope_type/scope_id/attr_id` 已存在软禁用绑定，返回 409，提示通过 `PATCH enabled_flag=true` 重新启用。
- `PATCH /attribute-bindings/{relationId}` 不允许修改 `attr_id`，只允许修改 `required_flag`、`editable_flag`、`statistic_flag`、`sort_no`、`enabled_flag`；重新启用前必须校验该属性未被当前一级/二级类型继承；若必填或启停状态影响完整度，同步重算该资源。
- `DELETE /attribute-bindings/{relationId}` 删除资源本级绑定，不删除属性定义，不删除已有属性值；若删除必填属性，同步重算该资源。
- `POST/PATCH/DELETE /attribute-bindings` 的绑定写入、审计写入、`profile_complete_flag` 更新必须在同一 Prisma transaction 内完成；资源本级绑定只影响单个资源，允许同事务同步重算。

权限约束：

- `GET /attributes` 和属性值更新按现有资源属性权限执行。
- `GET /attribute-bindings` 需要资源属性管理读权限。
- `POST/PATCH/DELETE /attribute-bindings` 只允许管理员或具备资源属性绑定管理权限的用户调用；无权限返回 403。前端隐藏入口不能替代后端权限校验。

`GET /internal/resources/{resourceId}/attributes` 响应字段（列表，每项一个属性）：

| 字段 | 说明 |
|---|---|
| attrId | 属性定义 ID |
| attrCode | 属性编码 |
| attrName | 属性名称 |
| dataCategory | `static` / `realtime`（一期只返回 `static`） |
| dataType | 数据类型 |
| unit | 单位，无则返回 null |
| enumOptions | 字符串数组（`["选项A","选项B"]`），`enum` 类型时返回，其他返回 null |
| required | 是否必填（来自当前来源绑定层） |
| editable | 当前用户是否可编辑（**服务端已合并**：类型层 `editable_flag AND` 实例层 `editable_flag`，不拆开返回两层） |
| statistic | 是否支持统计 |
| sourceScope | `parent` / `subtype` / `resource` |
| attrValue | DB 实际属性值，永远反映存储状态；无记录时返回 null（不替换为默认值） |
| defaultValue | 有效默认值（来自 `definition.default_value`，全局唯一来源）；无默认值时返回 null。前端判断 `attrValue === null && defaultValue !== null` 时显示"（默认）"角标并预填表单 |
| defaultFillsRequired | 是否允许默认值满足必填完整度（来自 `definition.default_fills_required`）；前端可据此决定是否提示"请填写实际值" |
| valueSource | `manual` / `import` / `sync` / `system` / `force`；无记录时返回 null |
| updatedBy | 最后更新人；无记录时返回 null |

说明：

- `GET /attributes` 由服务端按当前资源类型和资源本级绑定计算最终属性。若资源 `resourceSubtypeId` 为空，则按 `resource_profile.type` 解析一级类型，并合并资源本级属性。
- `PATCH /attributes/{attrId}` 采用 **upsert 语义**：记录不存在时 INSERT，记录存在时 UPDATE。INSERT 时 `value_source = 'manual'`，`editable_flag` 来源规则见"资源属性初始化 > editable_flag 初始值来源"章节，`updated_by` 设为当前用户；UPDATE 时不修改 `editable_flag`，只更新 `attr_value`、各冗余字段、`value_source = 'manual'`、`updated_by`、`update_time`。**服务端必须校验 `attrId` 属于当前资源最终属性集合**（合并算法结果），否则返回 400；详见"属性维护规则 > 删除类型属性"末段通用规则。
- 管理员强制更新走 `/force`，body 需要包含原因；同样采用 upsert 语义，INSERT 时 `editable_flag` 来源规则与普通 upsert 一致（强制更新绕过的是本次写入的权限检查，不改变该属性的编辑策略）；`value_source` 写入 `'force'`（区别于普通 `'manual'`）；审计写入规则见"数据模型 > 资源属性审计"章节，与属性值修改在同一事务。
- realtime 属性不提供人工更新接口。

`GET /internal/resources/{resourceId}/profile-completeness` **实时计算**（不读 `profile_complete_flag` 字段），每次调用执行类型合并查询 + 属性值查询后即时得出结果，确保 `missingAttributes` 明细准确。列表接口才读 `profile_complete_flag` 字段（读持久化结果）。

三种响应状态：

**完整（profileComplete = true）：**
```json
{ "profileComplete": true, "missingRequiredAttributeCount": 0, "missingAttributes": [] }
```

**未完善（profileComplete = false）：**
```json
{
  "profileComplete": false,
  "missingRequiredAttributeCount": 3,
  "missingAttributes": [
    { "attrId": 1, "attrCode": "axis_distance", "attrName": "轴距", "sourceScope": "parent" }
  ]
}
```

**未计算（profileComplete = null，存量数据过渡期，仅当 `profile_complete_flag = null` 且用户通过列表接口跳转进来时出现）：**

> 注意：本接口是实时计算，正常情况下不会返回 null。此状态保留用于极端场景（如数据库异常无法完成合并查询），前端按未知处理即可。

```json
{ "profileComplete": null, "missingRequiredAttributeCount": null, "missingAttributes": null }
```

- `missingAttributes` 三种状态均返回（`true` 时为空数组，`null` 时为 null），Response Schema 结构固定。
- `missingRequiredAttributeCount`：`true` 时为 0，`false` 时为正整数，`null` 时为 null。
- 前端"该资源有 N 个必填属性未完善"提示中 N 取 `missingRequiredAttributeCount`。
- `profileComplete = null` 时前端不展示角标，不允许将 `null` 当作 `false` 处理。

### 资源列表扩展

```text
GET /internal/resources?profileComplete=true|false
```

列表项建议增加：

| 字段 | 说明 |
|---|---|
| profileComplete | 资料是否完善 |
| missingRequiredAttributeCount | 缺失必填属性数量 |
| hasRealtimeReport | 后续实时属性接入后返回 |

`profileComplete = null` 表示完整度尚未计算，前端不展示”未完善”角标，`missingRequiredAttributeCount` 同为 null；筛选 `profileComplete=false` 只返回明确计算为 false 的资源。

## 前端功能入口设计

### 类型管理页

页面：`/resource-types`

新增两个入口：

1. 一级类型属性管理：在左侧一级类型区域或详情区增加“一级属性管理”按钮。
2. 二级类型属性管理：在选中二级类型详情区增加“属性管理”按钮。

`TypeAttributeDrawer` 需要支持两种类型 scope；资源本级属性管理在资源编辑弹窗内完成，不复用类型管理页入口：

```ts
type TypeAttributeScope =
  | { scope: 'parent'; parentType: ResourceParentType }
  | { scope: 'subtype'; parentType: ResourceParentType; typeId: string };
```

一级类型 Drawer：

```text
[本级属性] [属性统计（二期）]
```

Tab 对应接口：
- 本级属性：`GET /internal/resource-parent-types/{parentType}/attributes`
- 属性统计：二期实现，一期 Tab 禁用并显示"敬请期待"

**本级属性 Tab — 新增绑定交互（内联创建，不跳转页面）：**

点击"新增绑定"打开 `AttributeDefinitionBindingModal`：

```
AttributeDefinitionBindingModal
  可搜索 Select（调用 GET /internal/attribute-definitions?keyword=，支持 name/code 模糊过滤）
  └── Select 下拉底部固钉：「+ 新建属性定义」
       → 嵌套打开 AttributeDefinitionQuickCreateModal
            attrCode * / attrName * / dataType * / unit（number）/ enumOptions（enum）
            defaultValue（可选，格式提示随 dataType 变化；enum 类型时渲染下拉选择）
            defaultFillsRequired（checkbox，仅在 defaultValue 非空时展示：□ 默认值可代替显式录入）
            备注 / dataCategory 固定 'static'，预填隐藏（一期限制）
            创建成功 → QuickCreate 关闭 → 父 Select 自动选中新定义
  绑定配置：requiredFlag / editableFlag / statisticFlag / sortNo（初始值从属性定义复制）
  [取消]  [确认绑定] → POST /attributes
```

全程不离开 TypeAttributeDrawer；属性定义的完整管理（编辑/停用）走独立的 `/attribute-definitions` 管理页。

二级类型 Drawer：

```text
[本级属性] [继承自一级类型] [属性统计（二期）]
```

Tab 对应接口：
- 本级属性：`GET /internal/resource-types/{typeId}/own-attributes`
- 继承自一级类型：`GET /internal/resource-types/{typeId}/inherited-attributes`（只读，不提供绑定/解绑入口；如需修改，跳转提示"请前往一级类型属性管理"）
- 属性统计：二期实现，一期 Tab 禁用并显示"敬请期待"

### 资源编辑弹窗

文件：`ResourceEditModal.tsx`

在现有 Modal 内增加 Tabs，但属性 Tab 必须与现有表单**完全解耦**：

```text
[基础信息] [属性]
```

**关键约束：现有表单逻辑零改动。**

`ResourceEditModal` 当前有一个单一 `form` 实例管理所有 editKind（device / iot / staff / simple / amenities / goods）的字段，`handleAfterOpenChange`、`validateFields`、`buildPayload`、`handleOk` 均强依赖这个 `form`。如果将属性字段整合进同一个 `form`，会破坏现有的字段初始化、校验、提交逻辑。

正确做法：**属性 Tab 是一个完全自治的子组件**，只接收三个 prop：

```tsx
// ResourceEditModal.tsx 内新增，基础信息 Tab 代码一行不改
<Tabs.TabPane tab="属性" key="attrs">
  <DeviceAttributeTab
    resourceId={record.resourceId}
    resourceSubtypeId={detail?.resourceSubtypeId ?? null}
    open={open}
  />
</Tabs.TabPane>
```

三个 prop 职责：
- `resourceId`：查询和更新属性值的目标资源。
- `resourceSubtypeId`：已保存的类型 ID，作为 `useEffect` 依赖，类型变更保存后触发属性列表重新拉取；允许为 `null`。组件内部不用它直接发请求，属性列表通过 `GET /internal/resources/{resourceId}/attributes` 由服务端按当前资源最终属性集合返回；当 `resourceSubtypeId = null` 时，服务端按 `resource_profile.type` 返回一级类型属性并合并资源本级属性。
- `open`：Modal 打开时触发首次拉取，关闭时清理本地 state。

`DeviceAttributeTab` 自己管理数据生命周期：

- `open` 变为 true 时自行调用 `GET /internal/resources/{resourceId}/attributes`
- 维护本地属性值 state，与外层 `form` 完全隔离
- 每行属性展示规则：
  - `attrValue !== null`：显示实际值，无角标
  - `attrValue === null && defaultValue !== null`：输入框预填 `defaultValue`，行尾显示"（默认）"灰色角标；`defaultFillsRequired=false` 时额外提示"请确认或录入实际值"
  - `attrValue === null && defaultValue === null`：显示占位符"—"
- 每行属性有独立的"编辑/保存"按钮；用户**实际修改内容后**点保存才调用 `PATCH /resources/{resourceId}/attributes/{attrId}`（未修改不发请求，保持 `attrValue=null` 状态，下次打开仍展示默认值）
- 增加资源本级属性管理区，仅管理员可见；调用 `GET /internal/resources/{resourceId}/attribute-bindings` 展示本级绑定，调用 `POST/PATCH/DELETE /internal/resources/{resourceId}/attribute-bindings` 维护绑定。
- 资源本级绑定新增、修改、删除成功后，重新拉取最终属性列表和实时完整度。
- Modal 底部"保存"按钮只负责"基础信息"Tab 的提交，属性保存不经过它

这样 Modal 改动范围仅限于 Tabs 壳层包裹，基础信息 Tab 的所有表单逻辑原封不动，回归测试范围最小。

资源本级属性管理交互：

```text
[属性值]  最终属性列表：一级继承 / 二级本级 / 资源本级，支持逐行编辑值
[本级属性] 管理员维护当前资源独有属性绑定，复用 AttributeDefinitionBindingModal
```

约束：

- 新增绑定时 Select 默认只展示 `dataCategory='static'` 且 `enabledFlag=true` 的属性定义。
- 已存在于当前资源最终属性集合中的属性不可再次绑定。
- 资源本级属性不允许覆盖继承属性；如需修改继承属性配置，回到一级/二级类型属性管理。
- 资源本级绑定变更如果影响必填属性，前端刷新完整度提示。

**类型变更与属性 Tab 的交互约束：**

基础信息 Tab 当前允许修改 `resourceSubtypeId`，属性 Tab 又依赖当前类型最终属性集合。为避免用户在未保存的新类型和旧属性集合之间来回操作导致错写，必须增加以下约束：

1. `ResourceEditModal` 监听基础信息表单中的 `resourceSubtypeId`。
2. 如果表单中的 `resourceSubtypeId` 与详情中的 `detail.resourceSubtypeId` 不一致，属性 Tab 禁用，并提示：

   ```text
   类型已变更但尚未保存，请先保存基础信息后再编辑属性。
   ```

3. 基础信息保存成功后，重新拉取资源详情和属性列表，再允许打开属性 Tab。
4. `DeviceAttributeTab` 保存属性时，服务端必须重新校验该 `attrId` 仍属于资源当前类型最终属性集合。
5. 如果服务端返回类型冲突，前端提示“资源类型已变化，请刷新属性列表后重试”。

### 添加资源弹窗

文件：`ResourceRegisterModal.tsx`

一期不在添加弹窗内展示类型属性，继续只录入基础信息。

资源创建成功后：

- 若存在未完善必填属性，提示“该资源有 N 个必填属性未完善，是否立即补录？”
- 可引导打开资源编辑弹窗的属性 Tab。

## 新建前端文件建议

| 文件 | 位置 | 说明 |
|---|---|---|
| `TypeAttributeDrawer.tsx` | `pages/resource-types/` | 类型属性管理 Drawer |
| `TypeAttributeOwnTab.tsx` | `pages/resource-types/` | 本级属性列表 |
| `AttributeDefinitionBindingModal.tsx` | `pages/resource-types/` | 新增绑定弹窗（可搜索 Select + 绑定配置） |
| `AttributeDefinitionQuickCreateModal.tsx` | `pages/resource-types/` | 内联快速创建属性定义（嵌套于 BindingModal） |
| `TypeAttributeInheritedTab.tsx` | `pages/resource-types/` | 二级类型继承属性展示 |
| `TypeAttributeStatisticsTab.tsx` | `pages/resource-types/` | 后续属性统计 |
| `ResourceAttributeTab.tsx` | `pages/resources/` | 资源实例属性 Tab（原 DeviceAttributeTab） |
| `ResourceAttributeForceUpdateModal.tsx` | `pages/resources/` | 管理员强制更新（原 DeviceAttributeForceUpdateModal） |
| `attributeDefinition.service.ts` | `services/` | 属性定义 API（含 keyword 搜索参数） |
| `typeAttribute.service.ts` | `services/` | 类型属性 API（含合并视图 listTypeAttributes） |
| `resourceAttribute.service.ts` | `services/` | 资源属性值 API + 资源本级属性绑定 API（原 deviceAttribute.service.ts） |

## 实施顺序

### 一期：静态属性闭环

- Prisma migration：`res_attribute_definition`、`res_type_attribute_binding`、`res_resource_attribute_value` 三张新表，`resource_profile` 新增 `profile_complete_flag` 字段。
- 新增属性定义表 CRUD（`AttributeDefinitionModule`）。
- 新增一级类型属性维护（`ParentTypeAttributeModule` 或归入 `ResourceTypeModule`）。
- 新增二级类型属性维护，包含重复绑定校验。
- 新增资源本级属性维护，包含与一级/二级继承属性的重复绑定校验。
- 实现一级 + 二级 + 资源本级属性合并查询。
- 实现 `resourceSubtypeId = null` 资源的一级类型属性兜底：按 `resource_profile.type` 映射 `parentType`，并合并资源本级属性。
- 实现资源 static 属性值查询：资源最终属性定义全量 LEFT JOIN 属性值记录，无记录属性补空展示，editable_flag 以当前来源绑定层为准。
- 实现资源创建时必填 static 属性空值记录初始化（同一 Prisma 事务，具体规则见"资源属性初始化与完整度 > 新建资源"章节）。
- 实现资源二级类型变更时的属性差异合并：共有属性 `editable_flag` 刷新为新类型绑定层值、新类型独有必填属性插入空值记录（`skipDuplicates: true`）、`resourceSubtypeId` 更新，三步在同一事务内完成，事务提交后同步重算完整度（具体规则见"资源属性初始化与完整度 > 资源二级类型变更"章节）。
- 实现资源 static 属性值单属性更新（普通）。
- 实现管理员强制更新，通过 `AuditService.recordEntityChange` 写 `operation_audit_log`，与属性值更新同事务。
- 新建 `CompletenessModule`（Bull queue + processor），实现异步批量完整度重算。
- 实现必填属性完整度同步计算（资源创建、单属性更新、类型变更、资源本级绑定变更四类同步触发点）。
- 资源列表展示”资料未完善”（读 `profileCompleteFlag`；`null` 不展示角标）。
- 资源编辑弹窗新增属性 Tab（`DeviceAttributeTab` 独立组件，现有表单逻辑零改动），并提供管理员可见的资源本级属性绑定管理入口。

### 二期：补录、筛选、统计

- 类型新增必填属性后异步补录空值记录。
- 批量补录资源属性。
- 资源列表按资料完整度筛选（只筛选明确 `true` / `false`，不把 `null` 归为未完善）。
- 静态属性统计。
- 数据量上来后再评估是否接 Elasticsearch。

### 属性默认值

> 依赖一期静态属性闭环完成后实施。

**后端**

- Prisma migration：`res_attribute_definition` 新增 `default_value VARCHAR NULL`、`default_fills_required BOOLEAN DEFAULT false`，执行 `prisma migrate dev --name add_attribute_default_value`。
- 属性定义 POST/PATCH 接口：接收 `defaultValue`、`defaultFillsRequired` 字段；写入前按 `data_type` 做格式校验（`number` 可解析为数字、`boolean` 限 `"true"`/`"false"`、`date` 限 ISO 8601、`enum` 必须在 `enum_options` 内、`json` 合法 JSON）。
- 枚举项删除校验扩展：`PATCH /attribute-definitions/{attrId}` 移除枚举项时，检查 `definition.default_value` 是否等于被删枚举值，若是则返回 409 并提示先清空默认值。
- 提取 `isAttributeFilled(attrValue, definitionDefaultValue, defaultFillsRequired)` 工具函数至 `completeness-calc.util.ts`，替换所有原有 `attrValue IS NULL` 判断。
- 同步完整度路径（资源创建事务、单属性更新后重算、类型变更后重算、资源本级绑定变更后重算）统一改用 `isAttributeFilled()`；`mergeTypeAttributes` 返回结果中增加 `definitionDefaultValue`、`defaultFillsRequired` 字段。
- Bull processor 重构：查询时补充 JOIN `res_attribute_definition` 取 `default_value`、`default_fills_required`，完整度判定改用 `isAttributeFilled()`。
- 完整度新增触发场景（在 `PATCH /attribute-definitions/{attrId}` 服务层实现）：
  - `default_value` 从非空变为 null（或反向）且 `default_fills_required=true`：查询该 `attrId` 所有 `scope_type='parent'` 的活跃绑定，对每个 parentType 投递 `{ scope: 'parentType', id }` Bull job。
  - `default_fills_required` 变更（任意方向）：同上逻辑，全量重算绑定了该属性的所有资源。
- `GET /resources/{resourceId}/attributes` 响应每条属性追加 `defaultValue`（`definition.default_value`）、`defaultFillsRequired`（`definition.default_fills_required`）字段；`attrValue` 始终返回 DB 实际值，不替换为默认值。

**前端**

- `AttributeDefinitionQuickCreateModal`（内联新建属性定义弹窗）：表单新增 `defaultValue` 输入框（格式提示随 `dataType` 变化，`enum` 类型渲染下拉选择）+ `defaultFillsRequired` checkbox（仅 `defaultValue` 非空时展示：`□ 默认值可代替显式录入`）。
- 属性定义完整管理页（`/attribute-definitions`）：编辑表单同步增加上述两字段。
- `ResourceAttributeTab` 属性行三态展示：
  - `attrValue !== null`：显示实际值，无角标。
  - `attrValue === null && defaultValue !== null`：输入框预填 `defaultValue`，行尾灰色"（默认）"角标；`defaultFillsRequired=false` 时追加提示"请确认或录入实际值"。
  - `attrValue === null && defaultValue === null`：显示占位符"—"。
- `ResourceAttributeTab` 保存逻辑：用户**实际修改输入框内容后**才调用 `PATCH /resources/{resourceId}/attributes/{attrId}`；未修改直接关闭编辑态，不发请求，保持 `attrValue=null` 状态（下次打开仍展示默认值）。

### 三期：实时属性

> **TODO（待 IoT 侧对齐后补充）：**
> - TODO-1：上报链路设计（谁调什么接口、HTTP/MQTT/队列消费哪条路径）
> - TODO-3：`value_number` / `value_json` 冗余字段由谁在何时填入
> - TODO-4：数据时效性处理（多久算"过期"、前端是否展示"N 分钟前上报"）
> - TODO-6：Kafka/Redis 引入的量化判断标准（QPS 阈值、延迟基线）

#### 三期已设计部分

---

##### 合并算法扩展（对应 review item 2）

**设计决策：保持 `GET /internal/resources/{resourceId}/attributes` 单一接口**

现有响应字段 `dataCategory` 已预留 `'realtime'` 值且一期注释写明"三期支持 realtime"，不新增独立接口。三期在静态三组（parent → subtype → resource）之后追加第四组实时属性（realtime group），`dataCategory='realtime'`。

**不修改 `mergeTypeAttributes`**，新增独立函数：

```typescript
// src/modules/attribute/attribute-merge.util.ts 新增
export interface MergedRealtimeAttrItem {
  relationId: string;         // res_type_attribute_binding.id
  attrId: string;
  attrCode: string;
  attrName: string;
  dataCategory: 'realtime';
  dataType: string;
  unit: string | null;
  enumOptions: string[] | null;
  required: false;            // realtime 属性不参与完整度，required 恒为 false
  editable: false;            // 不允许人工编辑
  statistic: boolean;
  sourceScope: 'parent' | 'subtype';   // realtime 不支持 resource scope
  sourceName: string;
  attrValue: string | null;   // 来自 res_resource_realtime_state.attr_value
  reportTime: string | null;  // 来自 res_resource_realtime_state.report_time，ISO 8601
}

export async function mergeRealtimeAttributes(
  parentType: string,
  resourceSubtypeId: string | null,
  resourceId: string,
  prisma: PrismaService,
): Promise<MergedRealtimeAttrItem[]>
```

查询逻辑：

```text
realtimeBindings = 查询 res_type_attribute_binding b
                   JOIN res_attribute_definition d ON b.attr_id = d.id
                   WHERE (
                       (b.scope_type = 'parent' AND b.scope_id = parentType)
                       OR (resourceSubtypeId 有值 AND b.scope_type = 'subtype' AND b.scope_id = resourceSubtypeId)
                     )
                     AND b.enabled_flag = true
                     AND d.enabled_flag = true
                     AND d.data_category = 'realtime'
                   ORDER BY
                     CASE b.scope_type WHEN 'parent' THEN 0 ELSE 1 END,
                     b.sort_no ASC

realtimeValues = 查询 res_resource_realtime_state
                 WHERE resource_id = resourceId
                   AND attr_id IN (realtimeBindings 的 attrIdSet)

合并：realtimeBindings LEFT JOIN realtimeValues ON attr_id
```

**`GET /resources/{resourceId}/attributes` 改动（三期）：**

```typescript
// 现有调用（不变）
const staticAttrs = await mergeTypeAttributes(parentType, resourceSubtypeId, prisma, resourceId);
// 三期新增
const realtimeAttrs = await mergeRealtimeAttributes(parentType, resourceSubtypeId, resourceId, prisma);
// 合并返回，静态在前，实时在后
return [...staticAttrs, ...realtimeAttrs];
```

**响应字段扩展（三期新增字段）：**

| 字段 | 说明 |
|---|---|
| `reportTime` | 设备最后上报时间，ISO 8601；无记录时为 null；**静态属性行统一返回 null，不省略该字段**，避免前端 undefined vs null 差异（新增字段） |

**不受影响的逻辑：**

- `mergeTypeAttributes` 不修改
- `computeAndUpdateCompleteness` 不修改（仍只看 static required 属性）
- 资源本级绑定（scope_type='resource'）**不支持** realtime，backend 保持原有 static-only 校验
- `res_resource_realtime_state` 不写入 `profile_complete_flag` 相关逻辑

**后端需同步解除的限制（三期 POST /attributes 校验调整）：**

现有代码在 `parent-type-attribute.service.ts` 和 `resource-type-attribute.service.ts` 的 POST 中有校验：

```typescript
if (def.dataCategory !== 'static') {
  throw new AppException('ATTR_BINDING_INVALID_CATEGORY', '一期只允许绑定 static 属性', 400);
}
```

三期上线时删除该校验（或改为：`scope_type='resource'` 时仍禁止 realtime，`scope_type='parent'|'subtype'` 时放开）。

---

##### 绑定 UI 调整（对应 review item 5）

**变更范围：类型绑定放开 realtime；资源本级绑定维持 static-only。**

**`AttributeDefinitionBindingModal`（类型绑定弹窗，`pages/resource-types/`）：**

三期去掉 `dataCategory: 'static' as const` 固定过滤，改为展示所有 `enabledFlag=true` 的属性定义（含 realtime）。

选中属性定义后，根据 `dataCategory` 动态调整绑定配置表单：

| 字段 | static | realtime |
|---|---|---|
| `requiredFlag` | 展示（可选） | **不展示**（realtime 不参与完整度，required 恒 false，后端忽略） |
| `editableFlag` | 展示（可选） | **不展示**（realtime 恒不可编辑，后端强制为 false） |
| `statisticFlag` | 展示（可选） | 展示（可选） |
| `sortNo` | 展示 | 展示 |

当 `dataCategory='realtime'` 时，表单底部展示提示文案：

```
实时属性由设备自动上报，不支持人工编辑，也不参与资料完整度计算。
```

**`AttributeDefinitionQuickCreateModal`（内联快速创建弹窗，`pages/resource-types/`）：**

三期将 `dataCategory` 从隐藏固定值改为可见 Select（`static` / `realtime` 两个选项）。

选中 `realtime` 时，联动隐藏以下字段（因对 realtime 无意义）：

- `requiredFlag`（隐藏）
- `editableFlag`（隐藏）

**`ResourceAttributeBindingModal`（资源本级绑定弹窗，`pages/resources/`）：**

**不修改**，维持 `dataCategory: 'static' as const` 固定过滤。资源本级绑定只允许追加差异化静态属性，realtime 属性是设备上报的类型级能力，不适合逐实例绑定。

**前端 `ResourceAttributeTab` 展示调整：**

三期接口会在静态属性行之后追加 realtime 属性行。`ResourceAttributeTab` 需要：

- `dataCategory='realtime'` 行禁用编辑控件（只读展示）
- 展示 `reportTime` 字段（如"3 分钟前上报"；`reportTime=null` 时展示"暂无数据"）
- 不展示"强制更新"入口（realtime 无此操作）
- 可选：在静态与实时属性组之间加分隔线或分组标题

### 四期：搜索与聚合增强

- 多属性组合筛选。
- 跨类型属性统计。
- ES 索引和聚合。
- 统计导出。

## 关键边界

- 本方案不支持任意多层类型树。
- 一级类型仍由前端硬编码，不提供后台增删改。
- 二级类型继续使用现有 `/internal/resource-types`。
- 属性接口统一使用 `/internal/resources` 和 `/internal/resource-types` 语义，不新建 `/devices` 体系。
- 一期不强依赖 Kafka、Redis、Elasticsearch。
- `resourceSubtypeId = null` 的历史资源按 `resource_profile.type` 映射一级类型，只展示和计算一级类型属性。
- 资源二级类型当前允许变更，属性展示和完整度按最新类型计算。
- 一期合并算法只处理 `data_category = 'static'` 的属性；**一期禁止绑定 realtime 属性定义**（POST /attributes 服务端校验），三期实时属性上线后放开绑定限制并将 realtime 属性并入合并查询和展示。
- 合并后属性列表排序规则见"属性继承与合并 > 合并算法"章节（继承属性组整体在前，本级属性组整体在后，组内按 `sort_no` 升序，不支持跨组交叉排序）。
- 孤儿属性值默认保留但不可见、不可编辑、不参与统计。

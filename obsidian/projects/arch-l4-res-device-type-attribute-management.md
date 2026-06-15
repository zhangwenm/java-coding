---
tags: [l4, res-admin, resource-type, attribute-management, architecture]
date: 2026-06-11
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

当前只支持两层继承：

```text
一级类型属性(parentType)
  ↓
二级类型属性(resourceSubtypeId)
  ↓
资源实例属性值(resourceId)
```

最终属性计算：

```text
resourceSubtypeId 有值：最终属性 = 一级类型属性 + 二级类型本级属性
resourceSubtypeId 为空：最终属性 = resource_profile.type 对应的一级类型属性
```

不支持任意多级祖先继承，不支持二级类型覆盖一级类型属性配置。

### 无二级类型资源的兜底

部分历史资源可能没有 `resourceSubtypeId`。这类资源不阻塞属性能力上线，后端按 `resource_profile.type` 解析一级类型，仅展示和维护一级类型属性。

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
| PostgreSQL + Prisma | 属性定义、一级类型属性、二级类型属性、资源属性值、完整度 |
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
| enabled_flag | 是否启用 |
| remark | 备注 |
| create_time | 创建时间 |
| update_time | 更新时间 |

约束：

- `attr_code` 创建后不建议修改。
- `data_category` 创建后不可修改。
- 已存在资源属性值后，不允许随意修改 `data_type`。
- 枚举属性允许新增枚举项；删除枚举项前需检查是否已有资源使用。

### 一级类型属性表

> **已优化：** 原"一级类型属性表"（`res_parent_type_attribute`）与"二级类型属性表"（`res_resource_type_attribute`）字段完全相同，合并为一张 **`res_type_attribute_binding`**，通过 `scope_type + scope_id` 区分绑定对象。

表名：`res_type_attribute_binding`

| 字段 | 说明 |
|---|---|
| id | 主键，`cuid()` |
| scope_type | 绑定范围：`parent`（一级类型）/ `subtype`（二级类型） |
| scope_id | 绑定对象：`scope_type=parent` 时为一级类型枚举值（`Device` 等）；`scope_type=subtype` 时为 `resource_type_option.type_id` |
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
  scopeType     String   @map("scope_type") @db.VarChar(16)
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

绑定校验（两类绑定共用同一张表，校验逻辑统一）：

- 二级类型新增绑定时，检查同 `attrId` 是否已存在 `scope_type='parent' AND scope_id=parentType AND enabled_flag=true` 的记录，有则 409。
- 服务端必须校验，不能只依赖前端。

### 二级类型属性表

> **已合并。** 二级类型属性绑定与一级类型属性绑定已统一使用 `res_type_attribute_binding` 表，通过 `scope_type='subtype'` 区分。详见上方「一级类型属性表」章节。

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

// 排序规则：继承属性组整体在前，本级属性组整体在后；resourceSubtypeId 为空时只有继承属性组
// 两组内部各自按 sort_no 升序，不支持跨组交叉排序
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
| sourceScope | `parent` / `subtype` |
| sourceName | 一级类型名称（服务端通过枚举映射表转换：`Device`→`机器人`、`IOT`→`IOT`、`Human`→`人员`、`Amenities`→`客需品`、`Goods`→`商品`）或二级类型名称（从 `resource_type_option.name` 读取） |
| relationId | 绑定关系 ID |

**`relationId` 说明：**

合并后 `relationId` 统一来自 `res_type_attribute_binding.id`（`cuid` 字符串，全局唯一），前端调用 PATCH / DELETE 时仍按 `sourceScope` 路由到不同接口：

| sourceScope | relationId 来自 | 前端操作接口 |
|---|---|---|
| `parent` | `res_type_attribute_binding.id` | `/internal/resource-parent-types/{parentType}/attributes/{relationId}` |
| `subtype` | `res_type_attribute_binding.id` | `/internal/resource-types/{typeId}/attributes/{relationId}` |

两者均来自同一张表，ID 全局唯一，不存在旧方案中的数值重叠问题。

### 缓存策略

一期可以不做 Redis 缓存。若接口压力增大，可加本地缓存或 Redis：

```text
resource_type_attrs:{parentType}:{resourceSubtypeId}
parent_type_attrs:{parentType}
subtype_own_attrs:{resourceSubtypeId}
```

失效规则：

- 修改一级类型属性：失效该 `parentType` 下所有二级类型 merged cache。
- 修改二级类型属性：只失效该 `resourceSubtypeId` 的 merged cache。
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

### 二级类型重复绑定校验

二级类型绑定属性时，如果该属性已经通过一级类型继承（父绑定 `enabled_flag = true`），禁止重复绑定。

错误提示：

```text
该属性已通过一级类型继承，如需修改配置请前往一级类型属性维护。
```

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
4. 残留属性值成为孤儿值，默认不可见、不可编辑、不参与统计。**服务端过滤**：`GET /internal/resources/{resourceId}/attributes` 只返回当前类型最终属性集合内的 attrId，孤儿值不出现在响应中，前端无感知。
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

实例层 `editable_flag` 初始值从**类型绑定层**复制（而非属性定义层），确保类型管理员的策略正确落到实例：

```text
1. sourceScope = parent  → 从 res_type_attribute_binding（scope_type='parent'）的 editable_flag 复制
2. sourceScope = subtype → 从 res_type_attribute_binding（scope_type='subtype'）的 editable_flag 复制
```

类型绑定层后续修改 `editable_flag` 时，不回溯修改已有实例的锁定状态；查询权限时以**类型层 AND 实例层**的结果为准。

> **已知不对称行为（设计决策）：**
> - 类型层**收紧**（`true→false`）：实例层保持旧值（可能是 true），AND 结果 = false，**立即对所有已有实例生效**。
> - 类型层**放开**（`false→true`）：实例层保持旧值（false），AND 结果仍为 false，**对已有实例不生效**，仅新建资源后续会以新策略初始化。
>
> 管理员若需对已有资源放开编辑权限，必须逐个或通过批量工具对 `res_resource_attribute_value.editable_flag` 执行强制更新（记录审计日志）。这是有意设计：实例层 `editable_flag` 代表"该记录是否被管理员显式锁定/解锁"，不自动随类型策略漂移。

### 类型新增属性后已有实例的处理

当一级或二级类型新增属性时，已存在的资源实例不会被立即补录 `res_resource_attribute_value` 记录（该补录行为延至二期异步批量处理）。查询层必须处理"有属性定义、无值记录"的情况。

无二级类型资源同样适用该规则：一级类型新增属性后，这类资源按 `resource_profile.type = parentType` 纳入受影响范围。

**查询合并规则：**

```text
资源属性查询结果 = 类型最终属性定义（全量） LEFT JOIN 已有属性值记录（部分）
```

对于有属性定义但无 `res_resource_attribute_value` 记录的属性：

| 字段 | 取值规则 |
|---|---|
| `attrValue` | 返回 null，前端统一展示占位符 |
| `editable` | 以类型绑定层 `editable_flag` 为准（规则同"新建资源 > editable_flag 初始值来源"章节），与有记录时的合并计算规则保持一致 |
| `valueSource` | 返回 null（无记录时字段存在但值为 null，与响应 Schema 结构一致） |
| `updatedBy` | 返回 null |

此规则同时适用于必填属性（**类型新增属性后的已有资源实例**：一期不补创空记录，二期异步补录后转为有记录）和非必填属性（永久按需补空，不主动创建记录）。注意：新建资源时必填属性**会**创建空记录，此处仅指已有资源实例在类型扩展后的处理。

**完整度影响：**

类型新增必填属性后，受影响资源的 `profile_complete_flag` 由后台异步批量重算。由于不存在值记录，该必填属性被视为空，完整度标记为未完善。用户在属性 Tab 填写并保存后，首次写入 `res_resource_attribute_value` 记录，完整度同步重算。

### 完整度计算

完整度只看 static 必填属性：

```text
完整   = 合并属性集合中 data_category=static 且类型绑定层 required_flag=true 的属性均有值（非 null 且非空白串）
未完善 = 上述属性中任意一条满足以下任意条件：
          · 无对应 res_resource_attribute_value 记录
          · attr_value IS NULL
          · TRIM(attr_value) = ''
```

> **`required_flag` 来源：** 完整度使用**类型绑定层**的 `required_flag`（`res_parent_type_attribute.required_flag` 或 `res_resource_type_attribute.required_flag`），而非 `res_attribute_definition.required_flag`。绑定层创建时从定义层复制，之后独立维护，两者可能不同，以绑定层为准。
>
> **空串处理：** `attr_value = ''` 或纯空白视为未填写，不计入完整。前端保存属性值时应在提交前 trim 并拒绝纯空白提交，后端同样在写入前 trim 校验必填属性。

`resource_profile` 必须新增完整度字段，通过 Prisma migration 完成。建议字段可空，避免上线初期存量资源在重算完成前全部显示为“未完善”：

```prisma
// prisma/schema.prisma - ResourceProfile model 新增字段
profileCompleteFlag Boolean? @map("profile_complete_flag")
```

执行 `prisma migrate dev --name add_profile_complete_flag` 生成迁移。存量数据初始为 `null`，表示“未计算”。上线流程：

1. 后端先上线字段和重算任务，不在前端展示角标。
2. 运行全量完整度 backfill，将存量资源更新为 `true` 或 `false`。
3. backfill 完成后，前端再展示“资料未完善”角标和筛选。
4. 若列表接口在过渡期返回 `null`，前端展示为“计算中”或不展示角标，不允许把 `null` 当作 `false`。

更新策略：

| 触发场景 | 更新方式 |
|---|---|
| 单条资源属性值更新 | 同步重算该资源 |
| 资源创建 | 同步初始化并计算 |
| **二级**类型新增必填属性 | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| **一级**类型新增必填属性 | 事务提交后投递 `{ scope: 'parentType', id: parentType }`（影响该 parentType 下所有资源） |
| 删除**二级类型**必填属性 | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| 删除**一级类型**必填属性 | 事务提交后投递 `{ scope: 'parentType', id: parentType }`（影响该 parentType 下所有资源，数量可能大） |
| **二级**类型修改 `required_flag` | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| **一级**类型修改 `required_flag` | 事务提交后投递 `{ scope: 'parentType', id: parentType }` |
| **一级**类型属性绑定重新启用（`enabled_flag: false→true`），且 `required_flag=true` | 事务提交后投递 `{ scope: 'parentType', id: parentType }`（触发路径与"一级类型新增必填属性"一致） |
| **二级**类型属性绑定重新启用（`enabled_flag: false→true`），且 `required_flag=true` | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| **一级**类型属性绑定软禁用（`enabled_flag: true→false`），且 `required_flag=true` | 事务提交后投递 `{ scope: 'parentType', id: parentType }`（必填属性从合并结果消失，受影响资源可能从"未完善"变为"完善"） |
| **二级**类型属性绑定软禁用（`enabled_flag: true→false`），且 `required_flag=true` | 事务提交后投递 `{ scope: 'subtype', id: subtypeId }` |
| 资源二级类型变更 | 同步重算该资源（单台，代价低） |

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

**类型变更事务执行步骤（三步按序，同一事务）：**

```text
Step 1 — 批量 UPDATE editable_flag（newAttrs 全集，含共有属性 + 孤儿值重激活属性）：
  UPDATE res_resource_attribute_value
  SET editable_flag = <新类型绑定层 editable_flag>
  WHERE resource_id = #{resourceId}
    AND attr_id IN (newAttrs 的所有 attrId)
  （每个 attrId 的 editable_flag 来源：sourceScope=parent 取 res_parent_type_attribute，
                                      sourceScope=subtype 取 res_resource_type_attribute）

Step 2 — INSERT 新类型必填属性中缺失的空值记录（仅新类型独有必填属性）：
  使用 createMany({ data: missingMandatoryAttrs, skipDuplicates: true })
  （skipDuplicates 保证孤儿值重激活场景不覆盖历史 attr_value；editable_flag 已在 Step 1 更新，此处不需再设）

Step 3 — UPDATE resource_profile.resource_subtype_id = #{newSubtypeId}
```

> `createMany({ skipDuplicates: true })` 只负责 INSERT 缺失记录，不更新已有记录。Step 1 的 UPDATE 必须在 Step 2 之前或独立执行，确保孤儿值的 `editable_flag` 也得到刷新。

新类型必填属性初始化与 `resourceSubtypeId` 字段更新在同一事务内完成，事务提交后同步重算该资源完整度。

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
PATCH  /internal/resources/{resourceId}/attributes/{attrId}
PATCH  /internal/resources/{resourceId}/attributes/{attrId}/force
GET    /internal/resources/{resourceId}/profile-completeness
```

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
| required | 是否必填（来自类型绑定层） |
| editable | 当前用户是否可编辑（**服务端已合并**：类型层 `editable_flag AND` 实例层 `editable_flag`，不拆开返回两层） |
| statistic | 是否支持统计 |
| sourceScope | `parent` / `subtype` |
| attrValue | 属性值，无记录时返回 null |
| valueSource | `manual` / `import` / `sync` / `system` / `force`；无记录时返回 null |
| updatedBy | 最后更新人；无记录时返回 null |

说明：

- `GET /attributes` 由服务端按当前资源类型计算最终属性。若资源 `resourceSubtypeId` 为空，则按 `resource_profile.type` 解析一级类型，只返回一级类型属性。
- `PATCH /attributes/{attrId}` 采用 **upsert 语义**：记录不存在时 INSERT，记录存在时 UPDATE。INSERT 时 `value_source = 'manual'`，`editable_flag` 来源规则见"资源属性初始化 > editable_flag 初始值来源"章节，`updated_by` 设为当前用户；UPDATE 时不修改 `editable_flag`，只更新 `attr_value`、各冗余字段、`value_source = 'manual'`、`updated_by`、`update_time`。**服务端必须校验 `attrId` 属于当前资源类型最终属性集合**（合并算法结果），否则返回 400；详见"属性维护规则 > 删除类型属性"末段通用规则。
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

`TypeAttributeDrawer` 需要支持两种 scope：

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
            attrCode * / attrName * / dataType * / unit（number）/ enumOptions（enum）/ 备注
            dataCategory 固定 'static'，预填隐藏（一期限制）
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
- `resourceSubtypeId`：已保存的类型 ID，作为 `useEffect` 依赖，类型变更保存后触发属性列表重新拉取；允许为 `null`。组件内部不用它直接发请求，属性列表通过 `GET /internal/resources/{resourceId}/attributes` 由服务端按当前类型返回；当 `resourceSubtypeId = null` 时，服务端按 `resource_profile.type` 只返回一级类型属性。
- `open`：Modal 打开时触发首次拉取，关闭时清理本地 state。

`DeviceAttributeTab` 自己管理数据生命周期：

- `open` 变为 true 时自行调用 `GET /internal/resources/{resourceId}/attributes`
- 维护本地属性值 state，与外层 `form` 完全隔离
- 每行属性有独立的"编辑/保存"按钮，调用 `PATCH /resources/{resourceId}/attributes/{attrId}`
- Modal 底部"保存"按钮只负责"基础信息"Tab 的提交，属性保存不经过它

这样 Modal 改动范围仅限于 Tabs 壳层包裹，基础信息 Tab 的所有表单逻辑原封不动，回归测试范围最小。

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
| `resourceAttribute.service.ts` | `services/` | 资源属性 API（原 deviceAttribute.service.ts） |

## 实施顺序

### 一期：静态属性闭环

- Prisma migration：`res_attribute_definition`、`res_type_attribute_binding`、`res_resource_attribute_value` 三张新表，`resource_profile` 新增 `profile_complete_flag` 字段。
- 新增属性定义表 CRUD（`AttributeDefinitionModule`）。
- 新增一级类型属性维护（`ParentTypeAttributeModule` 或归入 `ResourceTypeModule`）。
- 新增二级类型属性维护，包含重复绑定校验。
- 实现一级 + 二级属性合并查询。
- 实现 `resourceSubtypeId = null` 资源的一级类型属性兜底：按 `resource_profile.type` 映射 `parentType`，只展示一级类型属性。
- 实现资源 static 属性值查询：类型定义全量 LEFT JOIN 属性值记录，无记录属性补空展示，editable_flag 以类型绑定层为准。
- 实现资源创建时必填 static 属性空值记录初始化（同一 Prisma 事务，具体规则见"资源属性初始化与完整度 > 新建资源"章节）。
- 实现资源二级类型变更时的属性差异合并：共有属性 `editable_flag` 刷新为新类型绑定层值、新类型独有必填属性插入空值记录（`skipDuplicates: true`）、`resourceSubtypeId` 更新，三步在同一事务内完成，事务提交后同步重算完整度（具体规则见"资源属性初始化与完整度 > 资源二级类型变更"章节）。
- 实现资源 static 属性值单属性更新（普通）。
- 实现管理员强制更新，通过 `AuditService.recordEntityChange` 写 `operation_audit_log`，与属性值更新同事务。
- 新建 `CompletenessModule`（Bull queue + processor），实现异步批量完整度重算。
- 实现必填属性完整度同步计算（资源创建、单属性更新、类型变更三个同步触发点）。
- 资源列表展示”资料未完善”（读 `profileCompleteFlag`；`null` 不展示角标）。
- 资源编辑弹窗新增属性 Tab（`DeviceAttributeTab` 独立组件，现有表单逻辑零改动）。

### 二期：补录、筛选、统计

- 类型新增必填属性后异步补录空值记录。
- 批量补录资源属性。
- 资源列表按资料完整度筛选（只筛选明确 `true` / `false`，不把 `null` 归为未完善）。
- 静态属性统计。
- 数据量上来后再评估是否接 Elasticsearch。

### 三期：实时属性

- 接入实时属性上报链路。
- 建设 `res_resource_realtime_state`。
- 资源属性 Tab 展示实时状态，只读。
- 如实时读压力较大，再引入 Redis。
- 如需事件削峰和重放，再引入 Kafka。

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

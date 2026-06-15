---
tags: [架构, scene, hk, 海外, 分支合并, REST, Hessian, RPC]
date: 2026-06-10
project: base/scene
status: draft
scope: base
generalized: false
retrieval_triggers: [scene统一, hk合并, master-hk, 海内外统一, REST迁RPC, scene接口兼容]
---

# scene 服务海内外分支统一方案

> 分析日期：2026-06-10  
> 涉及仓库：`base/scene`  
> 分支：`master`（国内）vs `hk`（海外）

---

## 背景

`hk` 分支是早期从 `master` 拆出的海外部署版本，两者已深度分叉。现需统一为单一分支同时支撑海内外部署。

---

## 现状差距

### 分叉量级

| 维度 | 数值 |
|------|------|
| master 独有提交 | 472 个 |
| hk 独有提交 | 28 个 |
| 涉及差异文件 | 26 个 |

### 架构差异对比

| 维度 | master | hk |
|------|--------|----|
| 接口层 | Hessian RPC（主）+ 少量 deprecated REST | REST（50 个 Controller，~180 个接口） |
| Service 层 | 完整（19 个域，482 个方法） | 完整（16 个域） |
| DTO 来源 | `scene-client` jar | 迁到本地包 |
| pom parent | `spring-boot-starter-parent:2.2.5` | `rw-common-parent:6.0.0` |
| 独有功能 | aiot、gstack、simcard_sync_status | VPN、DataSyncAspect、PlaceRelation |
| Hessian 暴露 | SceneBaseService / SceneAccountService / PlaceDetailService / RobotDetailService | 无 |

### master 架构转型说明

master 已将接口层从 REST 迁移为 **Hessian RPC**，`DeprecatedApi.java` 注释：

> "scene服务已经改成rpc调用，原先的rest接口会陆陆续续都删掉。这里剩下的几个是由于部分老服务没有支持RPC，所以暂时保留，等迁移完了全部清理掉"

---

## Service 层兼容性分析

**结论：master Service 层几乎完整覆盖 hk 所有接口需求。**

| 分类 | 结果 |
|------|------|
| hk 用到的 Service 接口（25 个） | master 全部存在，含接口 + Impl ✓ |
| hk Controller 调用的方法（40 个 Controller） | 39/40 完全对齐 ✓ |
| 唯一缺失方法 | `PlaceService.getUser(long userId)` |

### 唯一缺口

`UserAccountApi` 调用 `placeService.getUser(userId)` 获取 `PlaceUser`，master 的 `PlaceService` 只有批量查询 `queryUsers(PlaceUserQuery)`，缺少单条查询。

**修复**：在 master `PlaceService` 接口补一个 `PlaceUser getUser(long userId)`，`PlaceServiceImpl` 实现调现有 Mapper 即可，5 分钟内完成。

---

## 推荐方案：以 master 为主干，REST + RPC 并存

### 核心思路

- master 保留 Hessian RPC，供国内调用方使用
- 将 hk 的 50 个 REST Controller 合入 master，供海外调用方使用
- 底层 `db/service/*` 共用同一套，不重复业务逻辑

```
统一后的 scene（master 为基础）
┌──────────────────────────────────────────────┐
│ 接口层                                        │
│ ├── Hessian RPC（SceneBaseService 等）        │ ← 国内调用方
│ ├── REST Controllers（从 hk 合入，50 个）     │ ← 海外调用方
│ └── HeartbeatApi                             │ ← 共用
├──────────────────────────────────────────────┤
│ Service 层（db/service/*）                   │ ← 共用，不区分环境
├──────────────────────────────────────────────┤
│ Mapper 层 + XML                              │
└──────────────────────────────────────────────┘
```

---

## 实施步骤

### 阶段一：代码合并

| 步骤 | 内容 | 工作量 |
|------|------|--------|
| 1 | hk 的 28 个提交 cherry-pick 到 master（主要是 VPN、DataSyncAspect、PlaceRelation 等） | 中 |
| 2 | 冲突处理：DTO 包路径（hk 本地 → master client jar 路径） | 中 |
| 3 | 将 hk 50 个 REST Controller 文件搬入 master | 低 |
| 4 | 改 4 个文件的 import 路径（见下方清单） | 低 |
| 5 | master `PlaceService` 补 `getUser(long userId)` 方法 | 低 |

#### 需要修改 import 的文件

| 文件 | 问题 | 修改 |
|------|------|------|
| `UserAccountApi.java` | 等待 `PlaceService.getUser` 补充 | 无需改 import |
| `PlaceApi.java` | `DataSyncer` 包路径不同 | `web/aspect` → `sync/aspect` |
| `deprecated/TaskApi.java` | DTO 是本地定义 | 改为引用 `scene-client` jar 的 `deprecated.*` |
| `deprecated/ThirdPartApi.java` | 同上 | 同上 |

### 阶段二：配置对齐

| 步骤 | 内容 |
|------|------|
| 6 | pom.xml 以 master 为准，补入 hk 需要的依赖（`rw-common-util:4.2.0` 等） |
| 7 | hk 数据库补加 `simcard_sync_status` 字段，或 mapper 做 nullable 容错 |

### 阶段三：环境差异配置化

| 步骤 | 内容 |
|------|------|
| 8 | 若 `DataSyncAspect` 仅海外生效，用 Spring Profile 隔离（`@Profile("hk")`） |
| 9 | 统一 CI/CD，同一镜像按环境变量区分部署行为 |

---

## 风险点

### 最高风险：DTO 包路径冲突

- hk 把 `Yuncang`、`YuncangInfo` 等 DTO 从 `scene-client` jar 迁到本地 `db/service/yuncang/`
- master 的 `SceneBaseServiceImpl`、mapper XML 均引用 `client.base.dto.*`
- **处理策略**：保留 master 的 client jar 路径，把 hk 本地 DTO 改回引用 client jar（client jar 是 RPC 接口契约，不能丢）

### 数据库 Schema 差异

- master 有 `simcard_sync_status` 字段，hk 没有
- 若直接用统一代码连 hk DB，insert/select 会报列不存在
- **处理策略**：hk DB 执行 `ALTER TABLE t_yuncang_info ADD COLUMN simcard_sync_status VARCHAR(32) NULL`

### REST 接口生命周期决策

合入后需明确 REST 的长期定位：

- **保留**：REST + RPC 并存，维护两套入口，改动最小
- **计划废弃**：合入后 REST 标记 `@Deprecated`，海外调用方排期改造，改造完删除

---

## 合并可行性评估

| 维度 | 评估 |
|------|------|
| 业务逻辑重写量 | **零**，Service 层全部共用 |
| 需改动文件数 | **4 个**（import 路径）+ 补 1 个方法 |
| 合并前置工作 | cherry-pick hk 的 28 个提交，解决 DTO 路径冲突 |
| 整体风险 | **低**，主要是机械性的路径修改，无逻辑变更 |

---

## 相关接口清单（hk REST）

hk 独有、需合入 master 的 REST Controller（共 44 个文件，约 180 个接口路径）：

- `account/UserAccountApi` — 用户账号查询（placeId / groupId / brandId / brokerId）
- `appsecret/AppSecretApi` — 应用密钥管理
- `archive/*Api` — 设备档案（CustomArchive / DeviceCode / DevicePassword / DeviceSubCode / IntelligentDevice）
- `broker/BrokerApi` — 运营商管理
- `config/DataConfigApi` — 数据配置
- `customer/CustomerDemandApi` — 客户需求
- `deprecated/TaskApi` / `ThirdPartApi` — 遗留任务接口
- `group/GroupApi` — 集团/品牌管理
- `lift/ThirdpartLiftApi` — 三方电梯
- `notify/SmsApi` / `ThirdpartNotifyApi` — 短信/三方通知
- `place/PhoneConfig*` / `PhoneMapping*` / `PhoneVoice*` — 电话配置
- `place/PlaceApi` / `PlaceDevice*` / `PlaceGate*` / `PlaceLift*` / `PlacePhone*` / `PlaceRobot*` / `PlaceUser*` / `PlaceYuncang*` — 场所全套管理
- `place/RegionApi` / `RouterApi` — 区域/路由
- `qa/*Api` — 问答（Default / Place / Content / Robot）
- `relation/RobotCustomerRelationApi` — 机器人客户关联
- `robot/RobotApi` / `YunfanRobotApi` — 机器人管理
- `sn/SnApi` / `SnYunfanApi` — SN 管理
- `vpn/VpnApi` — VPN（hk 特有功能）
- `yuncang/YuncangApi` — 云仓管理

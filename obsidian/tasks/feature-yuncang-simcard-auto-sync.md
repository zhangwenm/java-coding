---
tags:
  - yuncang
  - simcard
  - scheduled-task
  - manager
  - es-sync
date: 2026-05-26
project: manager
status: verified
scope: manager
generalized: false
confidence: 设计阶段未落地
---

按项目分组的条目列表
# 货柜流量卡自动同步方案

定时任务替代人工四步操作（刷新路由信息 → 查询货柜卡 → 查询路由卡 → 保存），同时修复 `/api/v4/yuncang/update` 只写 DB 不写 ES 的缺陷。

## 背景 / 问题

| 问题 | 说明 |
|------|------|
| 手动操作繁琐 | 每台机器需人工完成 4 步，无法规模化 |
| ES 不同步 | `/api/v4/yuncang/update` → `sceneBaseService.upsertYuncangInfo()` 只写 scene DB，ES 从未更新，搜索列表数据滞后 |
| B 类机器依赖设备在线 | `route_iccid` 在 DB 为空，必须先 MQTT 到物理设备才能拿到 |
| **B 类路由卡自动充值失效** | `datacenter/job-executor` 的 `simCardAutoRechargeJob` 从 `device.info.yuncang/doc` 读 `routeIccid`，ES 里没有时 `handleSimCardFromYuncangRoute` 首行直接 return，路由卡**永远不会被自动充值** |

### 机器分类

| 类型      | 判断条件                            | 处理策略                                                |
| ------- | ------------------------------- | --------------------------------------------------- |
| **A 类** | `iccid ≠ 空 AND route_iccid ≠ 空` | 离线批量，凌晨 3 点                                         |
| **B 类** | `iccid ≠ 空 AND route_iccid = 空` | 工作日 10 点，MQTT 拉 routeIccid                          |
| **C 类** | `iccid = 空 AND route_iccid = 空` | 工作日 10 点，与 B 类同批；MQTT 拉 iccid + routeIccid；iccid 不为空时调 `querySimcard(iccid)` 写主卡字段，routeIccid 不为空时另调 `querySimcard(routeIccid)` 写路由卡字段，两次查询独立 |
| 无法处理 | iccid 和 routeIccid 均为空且设备离线 | 记失败 List，7 天 TTL；更新 `simcard_sync_status = 2` |

### 统计 SQL（执行在 scene DB）

```sql
SELECT
  COUNT(*) AS total_deployed,
  SUM(CASE WHEN i.iccid != '' AND i.route_iccid != '' THEN 1 ELSE 0 END) AS type_a,
  SUM(CASE WHEN i.iccid != '' AND (i.route_iccid IS NULL OR i.route_iccid = '') THEN 1 ELSE 0 END) AS type_b,
  SUM(CASE WHEN i.iccid IS NULL OR i.iccid = '' THEN 1 ELSE 0 END) AS no_iccid
FROM t_yuncang_info i
INNER JOIN t_serial_number s ON s.product_id = i.id AND s.type = 'YUNCANG' AND s.status = 1
INNER JOIN t_place_yuncang p ON p.product_id = i.id AND p.deleted = 0
  AND p.place_id IS NOT NULL AND p.place_id != '';
```

## 决策记录

| 方案 | 结论 | 原因 |
|------|------|------|
| 在 common/webservice 新增批量 Hessian 接口 | ❌ | rw-backend 用 7.0.24，base/scene 用 7.0.25，当前版本 7.0.28，升级需评估 3 个版本 breaking change，风险不可控 |
| 现有 `listYuncangs(deployed=true)` 分页 + `queryYuncangInfo` N+1 | ✅ | 500 台 × 10ms/次 ≈ 5s，凌晨批次可接受；不修改 common/webservice 和 base/scene，零风险 |
| 在 base/scene 实现定时任务 | ❌ | base/scene 无 MQTT、无 simcard supplier 依赖，不可行 |
| 在 manager/rw-backend 实现定时任务 | ✅ | 所有依赖已就绪：sceneBaseService、SimcardSupplierService、yuncangMsgPublisher、IndexClient、RedisTemplate |

## 改动项目

| 项目                 | 路径                   | 改动内容                                                                                   |
| ------------------ | -------------------- | -------------------------------------------------------------------------------------- |
| manager/rw-backend | `manager/rw-backend` | 新建 `YuncangSimcardSyncService.java`；`KafkaConsumerConfig.java` 加 `@EnableScheduling`   |
| common/webservice | `common/webservice` | `dto/yuncang/YuncangInfo.java` 新增 `simcardSyncStatus` 字段（DTO 加字段，不新增 Hessian 方法，风险可控） |
| base/scene | `base/scene` | `YuncangMapper.xml` 的 upsert/update SQL 加 `simcard_sync_status`（`<if test>` 包裹保向后兼容） |
| scene DB           | 生产库 `t_yuncang_info` | `ALTER TABLE` 新增 `simcard_sync_status` 字段                                              |

## 实现方案

### 实现位置

`manager/rw-backend/src/main/java/ai/yunji/rw/backend/service/yuncang/YuncangSimcardSyncService.java`

### 方法结构

```
syncTypeA()                      @Scheduled A类入口，凌晨3点
syncTypeB()                      @Scheduled B类入口，工作日10点
syncMachine(id, iccid, routeIccid)  写 DB（含 simcard_sync_status=1）+ 写 ES + 更新去重缓存
fetchSimcardViaMqtt(id)          MQTT 5s 超时，返回 {iccid, routeIccid}，失败返回 null
                                 B类：只需 routeIccid；C类：两个字段都需要
querySimcard(iccid)              仅在 iccid 不为空时调用；主卡用 iccid 查，路由卡用 routeIccid 单独查，两次调用互不复用；内部轮询 5 家供应商 + ZCWG 截断重试
indexYuncang(...)                ES 写入主卡字段（`iccid`/`cardStatus`/`simCardSupplier`/`expireTime`/`trafficRemain`/`simCardPackage`/`operator`/`ts`/`brokerId`/`renewalEndtime`/`placeId`/`placeName`/`type`/`productId`）+ 路由卡6字段；字段须与 `SimCardAutoRechargeJobHandler` 所有 `record.get*()` 调用逐一对齐，重点：`trafficRemain` 用 `getFloat`（非 `getFloatValue`），缺失时 null 直接 return
appendFail(key, entry)           Redis List，7天TTL，供告警和人工补录
updateSyncStatus(id, status)     仅更新 simcard_sync_status 字段：1=已同步 2=设备离线待重试
clearDedup(iccid, id)            **写入 DB + ES 成功后**才 DEL pre_auto_simcard_cache:{iccid}:{productId}；不在写入前清除（避免写失败时 Kafka 去重保护窗口消失）
```

### B/C 类 MQTT 并发方案

B类 534台 + C类 696台 = **1230台**，5s 超时串行最坏耗时 102 分钟，必须并发。

参考 `SimCardDailyFlowJobHandler` 的做法，用固定线程池提交任务：

```java
// @PostConstruct 初始化
 executorService = new ThreadPoolExecutor(
     10, 20, 60, TimeUnit.SECONDS,
     new ArrayBlockingQueue<>(200),
     new ThreadPoolExecutor.CallerRunsPolicy());

// syncTypeB 主流程
CountDownLatch latch = new CountDownLatch(machines.size());
machines.forEach(m -> executorService.submit(() -> {
    try {
        Map<String, String> result = fetchSimcardViaMqtt(m.getId()); // 5s 超时
        if (result == null) {
            appendFail(...); updateSyncStatus(m.getId(), "2");
        } else {
            syncMachine(m.getId(), result.get("iccid"), result.get("routeIccid"));
        }
    } finally {
        latch.countDown();
    }
}));
latch.await(); // 等待全部完成后再返回，避免下次调度重叠
```

10 线程 × 5s 每批 × 123批 ≈ **62分钟**，并发后如全部离线最坏仇 62 分钟（实际大量在线设备响应远强于 5s，预期实际耐时更短）。

### 调度配置（Spring Cloud Config 外置）

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `yuncang.sync.cron.a` | `0 0 3 * * ?` | A类凌晨3点 |
| `yuncang.sync.cron.b` | `0 0 10 * * MON-FRI` | B类 + C类工作日10点（均需设备在线） |

### Redis 去重缓存处理

`KafkaMessageHandler` 用 `pre_auto_simcard_cache:{iccid}:{productId}` 1天TTL 去重，会拦截定时任务写入。
解决：写入 DB + ES **成功后**才 `DEL` 这两个 key，而非写入前清除。写入前清除的风险：供应商查询失败时缓存已删，Kafka 消费链路 1 天保护窗口消失，可能导致重复充值告警。

### ES 修复说明

**影响范围：不只是展示，是自动充值的前置依赖。**

`datacenter/job-executor` 充值链路：
```
simCardAutoRechargeJob
  └── handleAllSimCard(type=YUNCANG)
        └── handleSimCardFromDevice(record)        // 充主卡 iccid
        └── handleSimCardFromYuncangRoute(record)  // 充路由卡 routeIccid
              ← 读 ES device.info.yuncang/doc
              ← routeIccid 为空 → 首行 return，路由卡跳过
```

现有路径：`/api/v4/yuncang/update` → 只写 DB，ES 从未更新，导致 B 类路由卡无法被自动充值。

定时任务修复：写完 DB 后显式调用 `indexer.index("device.info.yuncang/doc", productId, map)`，将路由卡字段写入 ES，解锁充值 Job 对 B 类路由卡的处理。

**需写入 ES 的路由卡字段**（与 `SimCardAutoRechargeJobHandler` 读取字段严格对齐）：

| 字段 | 来源 |
|------|------|
| `routeIccid` | MQTT 拉取 / DB |
| `routeSimCardSupplier` | 供应商 API 查询结果 |
| `routeExpireTime` | 供应商 API 查询结果 |
| `routeTrafficRemain` | 供应商 API 查询结果 |
| `routeCardStatus` | 供应商 API 查询结果 |
| `routeSimCardPackage` | DB 已有字段 |
| `routeUseFlowWeek` | **不需要写** — 由 `simCardStatisticUseFlowJob` 定期回写 |
| `routeUseFlowMonth` | **不需要写** — 由 `simCardStatisticUseFlowJob` 定期回写 |

### 需修改的文件

| 文件 | 改动 |
|------|------|
| `KafkaConsumerConfig.java` | 加 `@EnableScheduling`（1行）|
| `service/yuncang/YuncangSimcardSyncService.java` | **新建**，约250行 |
| `common/webservice` `dto/yuncang/YuncangInfo.java` | 新增 `simcardSyncStatus` 字段 |
| `base/scene` `YuncangMapper.xml` | `upsertYuncangInfo` 和 `updateYuncangInfo` SQL 加 `simcard_sync_status`（`<if test>` 包裹） |

### 数据库变更（scene DB）

```sql
ALTER TABLE `t_yuncang_info`
  ADD COLUMN `simcard_sync_status` varchar(1) DEFAULT NULL
  COMMENT '流量卡同步状态：1=已同步 2=设备离线待重试'
  AFTER `route_sim_card_supplier`;
```

更新时机：

| 场景 | 值 |
|------|----|
| `syncMachine()` 同步成功 | `1` |
| MQTT 超时且 iccid 为空（设备离线） | `2` |
| 人工录入 iccid 后页面保存 | 清空（`NULL`），下次定时任务重新尝试 |

## 待办任务

- [x] #1 执行统计 SQL，确认 A/B 类机器数量
  结果：A类 3039台 / B类 534台 / C类 696台 / 共 4269台
- [x] #2 确认不新增 Hessian 接口，使用现有分页 N+1 方式获取机器列表
- [x] #3 实现 `fetchSimcardViaMqtt()`
- [x] #4 实现 `querySimcard()`：全部失败时抛异常由调用方标 status=2
- [x] #5 实现 `indexYuncang()`：写主卡7字段 + 路由卡7字段（含 `routeOperator`）；不写 `ts`；返回 bool
- [x] #6 实现 `appendFail()`：Redis List，7天TTL（已知双操作非原子低风险）
- [x] #7 实现 A 类同步主逻辑；catch 块补 `updateSyncStatus("2")`
- [x] #8 实现 B 类同步主逻辑：B类 iccid DB 值兜底；routeIccid 为空记失败
- [x] #13 实现 C 类同步主逻辑（合并入 syncTypeB）
- [x] #14 实现 `updateSyncStatus()`：`info.setSimcardSyncStatus(status)` 写 DB
- [x] #15 `common/webservice` YuncangInfo 加 `simcardSyncStatus`；`base/scene` YuncangMapper.xml 加 `simcard_sync_status`
- [x] #16 DDL `ALTER TABLE t_yuncang_info ADD COLUMN simcard_sync_status` —— 已确认执行
- [x] #9 `KafkaConsumerConfig` 加 `@EnableScheduling`，cron 配置外置
- [x] #10 `clearDedup()`：ES 写入成功后才执行
- [ ] #11 单元测试：A 类同步（供应商 API mock）
- [ ] #12 单元测试：B 类同步（MQTT 超时 + 设备离线场景）

## code-review 验收结论（2026-05-27）

`indexer.index(appname, id, obj)` 走 `/_update + doc_as_upsert`（partial update），`useFlowWeek`/`brokerId`/`type` 等设备档案字段不会被覆盖。

| 级别 | 问题 | 状态 |
|------|------|------|
| HIGH | ES 失败后仍清 dedup 缓存 | ✅ 已修复 |
| HIGH | syncTypeA/B catch 无 updateSyncStatus("2") | ✅ 已修复 |
| HIGH | querySimcard 失败静默跳过 | ✅ 已修复：抛异常 |
| HIGH | B 类 routeIccid=null 静默标成功 | ✅ 已修复 |
| LOW | ts 覆盖导致在线判断失效 | ✅ 已修复：不再写 ts |
| LOW | appendFail 双操作非原子 | 保留，低风险，无消费方 |
| LOW | querySimcard 吞所有异常 | 待后续优化 |

## 前置确认（动手前需用户回答）

1. ~~**统计 SQL**~~ ✅ 已确认：A类 3039台（N+1 供应商查询 −30s OK）；B+C类共 1230台（MQTT 串行最坏 102分钟，**必须并发**）
2. ~~**B 类 MQTT 超时**~~ ✅ 已确认：调整为 5s，B/C 类统一使用
3. ~~**路由卡 ES 字段命名**~~ ✅ 已确认：6 个字段名（`routeIccid`、`routeSimCardSupplier`、`routeExpireTime`、`routeTrafficRemain`、`routeCardStatus`、`routeSimCardPackage`）与 `datacenter/job-executor` `SimCardAutoRechargeJobHandler` 读取字段完全一致，无需额外确认

## 相关链接

- [[arch-yuncang-unified-dispatching]]

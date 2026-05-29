---
tags: [datacenter, data-push, 对接, 合作方, 解析, SC机器人, UP机器人, 底盘]
date: 2026-04-23
project: datacenter
status: done
scope: datacenter
generalized: false
retrieval_triggers: [push_type 518, SC机器人对接, UP机器人对接, TASKDETAIL_CHASSIS, 心跳解析, wt底盘]
---

# push_type=518 合作方数据解析指南（SC+UP+wt底盘）

push_type=518 = HEARTBEAT(2)+TASKSTATUS(4)+TASKDETAIL_CHASSIS(512)。同一个 appname 下可能同时绑定 SC 型和 UP 型机器人，两种机器人都会推 HEARTBEAT，需用 `type` 字段区分机器人种类，用 `position` 字段区分心跳子类型。

## 实际绑定的机器人类型

同一个 placeId 下，push_type=518 的 appname 可能同时绑定：

| 机器人类型 | productId 前缀 | chassisId 前缀 | 说明 |
|---|---|---|---|
| **SC上舱** | `SCYL` | `WTYS`（一体底盘） | 上舱+底盘固定配对 |
| **SC上舱** | `SCSK` | `WTHT`（分体底盘） | 底盘可分离，动态接驳不同上舱 |
| **UP** | `WTHT` | 无独立底盘 | UP型送货机器人（WTHT前缀） |

> ⚠️ WTHT 前缀有歧义：既用于分体底盘（有上舱），也用于 UP 型送货机器人。区分方式：分体底盘发布在 `robot/wt/WTHT.../topic/robot_status`，UP 机器人发布在 `robot/UP/WTHT.../topic/heartbeat`。

**底盘类型差异**：
- **一体底盘（WTYS）**：与 SCYL 上舱固定配对，chassisLiftState 通常为 0
- **分体底盘（WTHT）**：可动态抬载/放下上舱，`liftedId` 字段标记当前抬载的上舱 ID，`chassisLiftState=1` 表示正在抬载

## push_type=518 三类数据来源

| 位值 | DataType | 来源 Topic | 说明 |
|---|---|---|---|
| 2 | HEARTBEAT | `robot/SC/{productId}/topic/heartbeat` | SC本体心跳，type="SC" |
| 2 | HEARTBEAT | `robot/UP/{productId}/topic/heartbeat` | UP型心跳，type="UP" |
| 2 | HEARTBEAT | `place/.../position` | 位置心跳，type=**null** |
| 4 | TASKSTATUS | `robot/SC/{productId}/topic/task_status` 等 | 任务状态事件流（多条） |
| 512 | TASKDETAIL_CHASSIS | `robot/wt/{chassisId}/topic/task` | wt底盘任务完结详情（一条） |

**为什么用 512 不用 16**：SC机器人任务执行层在 wt 底盘，完结数据发到 `robot/wt/.../topic/task`，TASKDETAIL(16) 订阅的是 `flow` topic，收不到底盘数据。UP 型机器人无独立底盘，其任务数据走 TASKSTATUS(4)。

## 同一 SC 机器人的多条 HEARTBEAT 来源

一台 SC 机器人（如 SCYL00H05BYF03963）在同一个 placeId 下，合作方可能在短时间内收到**多条 dataType=2**，来自不同 MQTT topic，字段格式不同：

| 来源 Topic | 推送后 type | powerPercent 格式 | charging 字段 | 特有字段 |
|---|---|---|---|---|
| `robot/SC/SCYL.../topic/heartbeat` | SC | 0-1 float（如 1.0=100%） | ✅ 来自 chargeState | runningStatus/estop/idle/floor/chassisId |
| `robot/up/WTYS.../topic/heartbeat` | SC（RobotBindFilter转） | 0-100 integer（如 91） | ❌ 始终 false（isCharging丢失） | 无运动状态字段 |
| `robot/wt/WTYS.../topic/robot_status` | SC（RobotBindFilter转） | 0-1 float（如 0.94） | ✅ 来自 chargeState | runningStatus/moveStatus/floor/taskId |
| `robot/cabin/SCYL.../topic/heartbeat` | **CABIN**（非SC） | 0-100 integer（如 100） | ❌ 始终 false（isCharging丢失） | lockers舱门状态 |

**⚠️ 关键陷阱**：
- `charging` 字段只有来源 topic 含 `chargeState` 整数字段时才可信；底盘 up/heartbeat 和 cabin/heartbeat 用 `isCharging` boolean，FastJSON 无法映射，导致 `charging` 始终为 `false`
- `powerPercent` 格式不统一：SC 本体和 wt robot_status 发 0-1 小数，up/heartbeat 和 cabin/heartbeat 发 0-100 整数；合作方存储时需统一
- type="CABIN" 的心跳来自上舱独立心跳，不是 "SC"；合作方若只处理 type="SC" 会漏掉舱门状态数据

## 处理流程

### 第一步：验签

```java
if (!calcSign(body, appname, secret, ts).equals(sign)) {
    return 400;
}
```

sign 算法见 [[guide-data-push-partner-api]]。

### 第二步：按 dataType 分发

```java
switch (body.getIntValue("dataType")) {
    case 2   -> handleHeartbeat(body);
    case 4   -> handleTaskStatus(body);
    case 512 -> handleChassisTaskDetail(body);
    default  -> log.warn("未知 dataType: {}", dataType);
}
```

### 第三步：HEARTBEAT 解析（dataType=2）

两个维度需要判断：**子类型**（位置 vs 状态）和**机器人类型**（SC vs UP）。

```java
void handleHeartbeat(JSONObject body) {
    String productId = body.getString("productId");
    String type      = body.getString("type");
    // type="SC"  → SC型机器人，来自 robot/SC/.../topic/heartbeat
    // type="UP"  → UP型机器人，来自 robot/UP/.../topic/heartbeat
    // type=null  → 位置心跳，来自 place/.../position（SC和UP都可能产生）

    if (body.get("position") != null) {
        // ── 位置心跳：只有坐标，无电量/状态 ──
        // SC 和 UP 型机器人都会发，此时 type=null，用 productId 前缀区分
        double x     = body.getJSONObject("position").getDouble("x");
        double y     = body.getJSONObject("position").getDouble("y");
        int    floor = body.getIntValue("floor"); // 同 currentFloor
        // → 更新机器人位置

    } else if ("SC".equals(type)) {
        // ── SC型状态心跳 ──
        // 有完整状态：电量、急停、空闲、底盘ID
        Float   power         = body.getFloat("powerPercent");   // 同 power
        Boolean charging      = body.getBoolean("charging");
        Boolean estop         = body.getBoolean("estop");
        Boolean idle          = body.getBoolean("idle");
        String  runningStatus = body.getString("runningStatus"); // "idle"/"running"
        String  chassisId     = body.getString("chassisId");    // WTYS前缀底盘ID
        int     floor         = body.getIntValue("floor");
        // → 更新SC机器人状态

    } else if ("UP".equals(type)) {
        // ── UP型状态心跳 ──
        // 通常只有电量，无 estop/idle/chassisId
        Float   power    = body.getFloat("powerPercent");        // 同 power
        Boolean charging = body.getBoolean("charging");
        // → 更新UP机器人电量状态
    }
}
```

**SC 与 UP 心跳字段对比**：

| 字段 | SC型 | UP型 | 位置心跳 |
|---|---|---|---|
| type | "SC" | "UP" | null |
| powerPercent | ✅ | ✅ | ❌ |
| estop / idle | ✅ | ❌ | ❌ |
| runningStatus | ✅ | ❌ | ❌ |
| chassisId | ✅（WTYS前缀） | ❌ | ❌ |
| position / orientation | ❌ | ❌ | ✅ |
| floor | ✅ | ❌ | ✅ |

### 第四步：TASKSTATUS（dataType=4）

一个任务会收到多条，按 `eventType` 驱动状态机：

```java
void handleTaskStatus(JSONObject body) {
    String taskId    = body.getString("taskId");
    String eventType = body.getString("eventType"); // 驱动状态机的关键字段
    String taskType  = body.getString("taskType");
    String target    = body.getString("target");    // 空时已自动取 currentTarget
    long   occurTime = body.getLongValue("occurTime");
    String extraData = body.getString("extraData");
    String msgId     = body.getString("msgId");     // 幂等去重
}
```

### 第五步：TASKDETAIL_CHASSIS（dataType=512）

一个任务只推一条，任务完结后归档：

```java
void handleChassisTaskDetail(JSONObject body) {
    String  taskId       = body.getString("taskId");
    String  status       = body.getString("status");        // 任务结果
    long    startTime    = body.getLongValue("startTime");  // 毫秒时间戳
    long    endTime      = body.getLongValue("endTime");    // 毫秒时间戳
    Double  distance     = body.getDouble("distance");
    Integer retryTimes   = body.getInteger("retryTimes");
    Integer chargeResult = body.getInteger("chargeResult"); // 充电结果
    Float   estopRatio   = body.getFloat("estopRatio");    // 急停占比
    String  content      = body.getString("content");
    String  chassisId    = body.getString("chassisId");    // 底盘ID，WTYS前缀
}
```

## 易错点

| 问题 | 正确做法 |
|---|---|
| HEARTBEAT 子类型判断 | 先用 `position != null` 区分位置心跳，再用 `type` 区分 SC/UP/CABIN |
| type 字段为 null | 位置心跳来自 place topic，SC 和 UP 都可能产生，需用 productId 前缀区分 |
| UP 型心跳字段少 | UP 型无 estop/idle/chassisId，不要对这些字段做非空断言 |
| charging 字段不可信 | 底盘 up/heartbeat 和 cabin/heartbeat 的 `isCharging` 会丢失，`charging` 始终 false；只有 SC 本体心跳和 wt robot_status（dataType=128）的 `charging` 可信 |
| powerPercent 格式不统一 | SC本体/wt robot_status 发 0-1 小数；up/heartbeat、cabin/heartbeat 发 0-100 整数；存储前需判断格式统一处理 |
| type="CABIN" 被忽略 | 上舱独立心跳推出 type="CABIN"，含 lockers 舱门状态；只处理 type="SC" 会漏掉此数据 |
| 分体/一体底盘区分 | WTHT 前缀有歧义：分体底盘发 `robot/wt/WTHT.../topic/robot_status`，UP 机器人发 `robot/UP/WTHT.../topic/heartbeat`；用 topic 路径区分，不能只看 productId 前缀 |
| TASKSTATUS 多条去重 | 用 `msgId` 去重，用 `taskId+eventType` 驱动状态机 |
| TASKDETAIL_CHASSIS 时间 | `startTime`/`endTime` 是 **long 毫秒**，不是 Date |
| 接口幂等 | data-push 超时最多重试 5 次，必须用 `msgId` 去重 |
| 返回值 | HTTP 200 即可，不解析 response body |

## 相关链接

- [[guide-data-push-partner-api]] — 通用签名算法与字段说明
- [[arch-data-push]] — 整体架构与 push_type 位掩码

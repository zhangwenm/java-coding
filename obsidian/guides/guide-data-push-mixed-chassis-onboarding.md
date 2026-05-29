---
tags: [datacenter, data-push, 对接, 合作方, 一体机, 分体机, 配置, push_type]
date: 2026-04-23
project: datacenter
status: done
scope: datacenter
generalized: false
retrieval_triggers: [一体机分体机混部署, 合作方对接配置, powerPercent归一化, push_type配置, SCYL SCSK WTYS WTHT]
---

# 一体机+分体机混部署合作方对接配置指南

同一场地同时部署了一体机（SCYL+WTYS）和分体机（SCSK+WTHT）时，push_type=518 是标准配置。由于多个 topic 来源导致 `powerPercent` 格式不一致、`type` 字段有歧义，**推荐写自定义 Sender 做字段标准化**，否则合作方须自行处理兼容性。

## 一、DB 配置（push_config + app_robot）

### push_config 表

| 字段 | 值 | 说明 |
|------|-----|------|
| `appname` | `XXXX`（合作方标识） | 唯一标识，与 app_robot 关联 |
| `sender_name` | `XXXX`（自定义Bean名）或留空 | 留空=CommonRobotMsgSender 全量透传 |
| `push_type` | **518** | HEARTBEAT(2)+TASKSTATUS(4)+TASKDETAIL_CHASSIS(512) |
| `push_url` | 合作方 HTTP 接口地址 | — |
| `secret` | 签名密钥 | 用于 MD5 sign 计算 |

**push_type=518 的位含义**：
- `2`（HEARTBEAT）：SC/UP 机器人心跳 + 位置心跳
- `4`（TASKSTATUS）：任务事件流（多条，含 UP 型任务）
- `512`（TASKDETAIL_CHASSIS）：wt底盘任务完结详情（SC机器人执行层，一条）

> ⚠️ 不要加 `128`（HEARTBEAT_CHASSIS）：会导致底盘 robot_status 被重复推送，数据内容相同但 dataType=128，干扰合作方解析。

### app_robot 表

把所有上舱的 productId 绑定到此 appname：

| productId | 说明 |
|-----------|------|
| `SCYL00HXXXX` | 一体机上舱 |
| `SCSK00HXXXX` | 分体机上舱 |

底盘（WTYS/WTHT）不需要单独绑定，RobotBindFilter 会自动通过绑定关系把底盘心跳转换为上舱 productId 推送。

---

## 二、推送方案选择

### 方案 A：自定义 Sender（推荐）

在 `sender/impl/<name>/` 下新建 Sender，统一处理字段差异后再推送，合作方收到的数据已标准化。

```
sender/impl/xxxx/
  XxxxRobotMsgSender.java   ← 继承 AbstractRobotMsgSender，@Component("XXXX")
```

DB 中 `sender_name = XXXX`（与 `@Component` 值一致）。

适用于：合作方是内部或可协商的，不希望合作方自己做兼容层。

### 方案 B：CommonRobotMsgSender（全量透传）

`sender_name` 留空，数据按原格式推出，合作方自行处理 powerPercent 格式差异和 type 字段歧义。

适用于：合作方研发能力强、或已有对接过 518 数据的经验。

---

## 三、自定义 Sender 字段标准化逻辑（方案 A 实现要点）

### 3.1 powerPercent 归一化

不同 topic 来源的格式：

| 来源 Topic | powerPercent 格式 | 推送时 type |
|------------|-------------------|-------------|
| `robot/SC/.../topic/heartbeat` | 0-1 小数（如 0.85） | SC |
| `robot/wt/.../topic/robot_status` | 0-1 小数（如 0.94） | SC（RobotBindFilter转） |
| `robot/up/.../topic/heartbeat` | 0-100 整数（如 91） | SC（RobotBindFilter转） |
| `robot/cabin/.../topic/heartbeat` | 0-100 整数（如 100） | CABIN |

归一化规则（统一为 0-100 百分比整数，便于前端展示）：

```java
Float raw = body.getFloat("powerPercent");
int powerDisplay;
if (raw == null) {
    powerDisplay = -1;  // 无电量字段（位置心跳）
} else if (raw <= 1.0f) {
    powerDisplay = Math.round(raw * 100); // 0-1 → 0-100
} else {
    powerDisplay = Math.round(raw);       // 已是 0-100
}
```

### 3.2 type 字段归一化

| 原始 type | 含义 | 推荐标准化 |
|-----------|------|------------|
| `"SC"` | SC型机器人 | 保持 `"SC"` |
| `"UP"` | UP型送货机器人 | 保持 `"UP"` |
| `"CABIN"` | 上舱独立心跳（含舱门状态） | 映射为 `"SC"` |
| `null` | 位置心跳 | 根据 productId 前缀判断 |

```java
String type = body.getString("type");
if ("CABIN".equals(type)) {
    type = "SC";  // 合并为SC，lockers字段仍可透传
}
```

### 3.3 charging 字段说明

**仅当数据来自 SC 本体心跳或 wt robot_status 时，`charging` 字段可信。**

来自 `robot/up/` 和 `robot/cabin/` 的心跳，原始字段是 `isCharging`（boolean），FastJSON 无法映射到 `RobotHeartbeat.chargeState`（int），导致 `charging` 始终为 `false`。

合作方应以 `powerPercent` 变化趋势辅助判断充电状态，而非强依赖 `charging` 字段。

---

## 四、合作方数据解析（以 push_type=518 为例）

### 4.1 验签

```java
String sign = calcMd5(appname + ts + bodyJsonString);
if (!sign.equals(requestSign)) return 400;
```

详见 [[guide-data-push-partner-api]]。

### 4.2 按 dataType 分发

```java
switch (body.getIntValue("dataType")) {
    case 2   -> handleHeartbeat(body);
    case 4   -> handleTaskStatus(body);
    case 512 -> handleChassisTaskDetail(body);
}
```

### 4.3 HEARTBEAT（dataType=2）解析

```java
void handleHeartbeat(JSONObject body) {
    String productId = body.getString("productId");
    String type      = normalizeType(body.getString("type")); // CABIN→SC

    if (body.get("position") != null) {
        // 位置心跳：只有坐标，type=null
        double x = body.getJSONObject("position").getDouble("x");
        double y = body.getJSONObject("position").getDouble("y");
        int floor = body.getIntValue("floor");
        // → 更新坐标

    } else if ("SC".equals(type)) {
        // SC型（含一体机/分体机上舱/CABIN）
        int     power    = normalizePower(body.getFloat("powerPercent")); // 0-100
        Boolean charging = body.getBoolean("charging"); // 仅SC本体/wt robot_status可信
        Boolean estop    = body.getBoolean("estop");
        Boolean idle     = body.getBoolean("idle");
        String  chassisId = body.getString("chassisId"); // 一体WTYS/分体WTHT前缀
        // CABIN来源：body.getJSONArray("lockers") 含舱门状态
        // → 更新SC机器人状态

    } else if ("UP".equals(type)) {
        // UP型送货机器人
        int     power    = normalizePower(body.getFloat("powerPercent")); // 0-100
        // 无 estop/idle/chassisId 字段，不要做非空断言
        // → 更新UP机器人电量
    }
}
```

### 4.4 TASKSTATUS（dataType=4）

一个任务推多条，用 `msgId` 去重，用 `eventType` 驱动状态机：

```java
void handleTaskStatus(JSONObject body) {
    String taskId    = body.getString("taskId");
    String eventType = body.getString("eventType"); // CREATED / ASSIGNED / EXECUTING / COMPLETED ...
    String taskType  = body.getString("taskType");
    long   occurTime = body.getLongValue("occurTime");
    String msgId     = body.getString("msgId"); // 幂等去重
    // data-push 超时重试最多5次，msgId去重必须实现
}
```

### 4.5 TASKDETAIL_CHASSIS（dataType=512）

每个任务只推一条，任务完结后归档：

```java
void handleChassisTaskDetail(JSONObject body) {
    String  taskId       = body.getString("taskId");
    String  status       = body.getString("status");       // 任务结果
    long    startTime    = body.getLongValue("startTime"); // 毫秒时间戳，非Date
    long    endTime      = body.getLongValue("endTime");
    String  chassisId    = body.getString("chassisId");   // 底盘ID
    Double  distance     = body.getDouble("distance");
    Integer retryTimes   = body.getInteger("retryTimes");
    Float   estopRatio   = body.getFloat("estopRatio");
}
```

UP 型机器人无独立底盘，其任务数据通过 TASKSTATUS(4) 传递，不产生 TASKDETAIL_CHASSIS(512)。

---

## 五、设备 ID 对照

| 机器人类型 | 上舱 productId 前缀 | 底盘 chassisId 前缀 | 特征 |
|-----------|--------------------|--------------------|------|
| 一体机 | `SCYL` | `WTYS` | 固定配对，不分离 |
| 分体机 | `SCSK` | `WTHT` | 底盘可动态抬载不同上舱，`liftedId` 标记当前上舱 |
| UP送货机 | `WTHT` | 无独立底盘 | 注意 WTHT 前缀与分体底盘歧义 |

**WTHT 前缀歧义区分**：
- 分体底盘：MQTT topic = `robot/wt/WTHT.../topic/robot_status`
- UP机器人：MQTT topic = `robot/UP/WTHT.../topic/heartbeat`
- data-push 推出的数据：分体底盘经 RobotBindFilter 后 type="SC"；UP机器人 type="UP"

---

## 六、易错点速查

| 问题 | 正确做法 |
|------|---------|
| powerPercent 格式不一 | 用 `if (raw <= 1.0f)` 判断格式统一为 0-100 整数 |
| type="CABIN" 被忽略 | 含舱门状态（lockers），合并为"SC"处理时保留 lockers 字段 |
| charging 始终 false | 仅 SC 本体/wt robot_status 来源可信；辅以电量趋势判断充电 |
| UP型心跳字段少 | 无 estop/idle/chassisId，不做非空断言 |
| TASKSTATUS 重复推 | 必须用 msgId 去重（最多重试5次） |
| TASKDETAIL_CHASSIS 时间戳 | startTime/endTime 是 long 毫秒，不是 Date 对象 |
| 加了 push_type=128 | 导致底盘 robot_status 被推两次（dataType=2 和 128），合作方需过滤 |

## 相关链接

- [[guide-data-push-518-parsing]] — 完整字段解析参考（SC/UP/wt底盘三类数据）
- [[arch-data-push]] — 整体架构与 Receiver 全景
- [[guide-data-push-partner-api]] — 签名算法与通用字段说明

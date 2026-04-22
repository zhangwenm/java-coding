---
tags: [架构, lot, open-robot-call-api, 接口文档]
date: 2026-04-22
project: lot
status: done
---

# open-robot-call-api 机器人控制接口文档

> 项目路径：`~/appstore/project/lot/open-robot-call-api`
> 更新日期：2026-04-22

## 概述

对外开放的机器人任务调度服务，提供统一的 HTTP API 控制多品牌机器人（歌歌/GEGE、女娲/NVWA、启雷/KF、DELI、Up底盘/WT、SC医疗舱）执行任务。

鉴权方式：
- **appname + appSecret**：对外第三方调用（`permissionService.hasRobotPermission`）
- **user token**：H5 页面用户直接调用（`userHasRobotPermission`）

机器人类型识别规则（`BaseControlApi.getRobotController`）：

| productId 前缀 | 控制器类型 |
|---|---|
| HOTGG / QL / GEGE / GG | GEGE |
| HOT | NVWA（女娲） |
| KF | KF |
| DELI / DL | DELI |
| WATER / WT / OCEAN | CHASSIS（底盘） |
| SC | SC（医疗舱） |

---

## 接口列表

### 一、V5 核心控制接口（RobotV5ControlApi）

#### 1. 召唤机器人

```
POST /openapi/v5/robot/call
```

发送移动任务，支持多种 `taskType`，是最核心的控制接口。

**请求体（MoveRequest）：**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| appname | String | 是 | 应用标识，用于鉴权 |
| productId | String | 是 | 机器人编号 |
| taskType | String | 是 | 任务类型（见下表） |
| itemParams | List | 否 | 配送目标列表，每项含目标点、语音等 |
| origin | String | 否 | 出发点 |
| start | String | 否 | `call_to_transport` 的取货起始点 |
| callbackUrl | String | 否 | 任务事件回调地址 |
| callbackEventList | String | 否 | 关注的回调事件（逗号分隔） |
| autoBack | boolean | 否 | 任务完成后是否自动返回（默认 true） |
| async | boolean | 否 | 是否异步（默认 false） |
| extraData | String | 否 | 扩展 JSON，含 `rcs_task_id` 可触发多语音 |
| doorAutoCloseTimeout | int | 否 | 舱门自动关闭超时秒数（默认 30） |
| backToCaller | boolean | 否 | 完成后是否返回呼叫点 |
| forceCloseDoor | boolean | 否 | 强制关门 |
| fetchedAutoBack | boolean | 否 | 取件成功后自动返回（默认 true） |
| notFetchedAutoBack | boolean | 否 | 取件失败后自动返回（默认 true） |
| breakFlow | boolean | 否 | 中断当前任务流 |
| resumeFlow | boolean | 否 | 恢复任务流 |

**taskType 取值：**

| taskType | 说明 |
|---|---|
| `delivery` | 标准配送：机器人前往目标点派送物品 |
| `call` | 召唤：机器人到指定点等待用户 |
| `call_to_transport` | 召唤并运输：先到取货点取货，再送往多目标 |
| `call_to_getitem` | 召唤并取件：前往取件点，用户取物后返回 |
| `build_transport` | 自定义运输流程 |
| `cruise` | 巡游模式（`cruiseType`、`cruiseName`、`targets` 等参数生效） |

**可监听的回调事件（callbackEventList）：**

| 事件 | 说明 |
|---|---|
| `goto_target` | 机器人前往目标点 |
| `exit_lift` | 机器人离开电梯 |
| `arrive_target` | 机器人到达目标点 |
| `goods_fetched` | 物品已取出 |
| `start_go_back` | 开始返回 |
| `battery_charging` | 开始充电 |
| `goods_not_fetched` | 物品未取 |
| `task_canceled` | 任务已取消 |

---

#### 2. 开舱门

```
POST /openapi/v5/robot/door/open
```

| 字段 | 类型 | 必填 |
|---|---|---|
| appname | String | 是 |
| productId | String | 是 |
| callbackUrl | String | 否 |

---

#### 3. 关舱门

```
POST /openapi/v5/robot/door/close
```

参数同上。

---

#### 4. 机器人回充

```
POST /openapi/v5/robot/back
```

命令机器人返回充电桩。

| 字段 | 类型 | 必填 |
|---|---|---|
| appname | String | 是 |
| productId | String | 是 |
| callbackUrl | String | 否 |

---

#### 5. 取消任务

```
POST /openapi/v5/robot/task/cancel
```

| 字段 | 类型 | 必填 |
|---|---|---|
| appname | String | 是 |
| productId | String | 是 |
| requestId | String | 否 | 指定取消的 requestId，为空则取消当前任务 |

---

#### 6. 重新派送

```
POST /openapi/v5/robot/redeliver
```

任务失败后重新触发派送（如用户未取件后再次派送）。

---

#### 7. 机器人排班调度

```
POST /openapi/v5/robot/schedule
```

按场地（placeId）提交调度请求，由系统自动分配空闲机器人执行。

| 字段 | 类型 | 必填 |
|---|---|---|
| appname | String | 是 |
| placeId | String | 是 |
| taskType | String | 是 |
| 其余同 call | - | - |

> 鉴权为场地级别（`hasPlacePermission`），而非机器人级别。

---

#### 8. 查询任务状态

```
GET /openapi/v5/robot/task/status?appname=&productId=
```

返回机器人当前运行状态（通过 `RobotController.getRunningState`）。

---

#### 9. App 通知推送

```
POST /openapi/v5/robot/app/notify
```

向机器人的 App 推送自定义通知（如屏幕消息提示）。

---

### 二、V5 Flow 接口（UpRobotV5ControlApi）

用于底盘 + 上仓一体机（Up 系列）的流程化任务控制。

#### 10. 提交任务流

```
POST /openapi/v5/robot/flow/submit
```

提交完整 Flow 任务，`executors` 为 JSON 数组，每个元素是一个执行动作（移动、等待、舱控制等）。

| 字段 | 类型 | 必填 |
|---|---|---|
| appname | String | 是 |
| chassisId | String | 是（底盘ID） |
| upperId | String | 否（上仓ID） |
| taskId | String | 是 |
| missionKey | String | 否 |
| executors | String | 是（JSON 数组） |
| timeoutCallback | String | 否 |

---

#### 11. 继续任务流

```
POST /openapi/v5/robot/flow/continue
```

在已有任务流上继续追加步骤（需 `chassisId` + `upperId`）。

---

#### 12. 续租任务流

```
POST /openapi/v5/robot/flow/relet
```

延长任务流的超时时间（需 `chassisId` + `taskId` + `missionKey`）。

---

#### 13. 取消任务流

```
POST /openapi/v5/robot/flow/cancel
```

取消指定 `taskId` 的任务流（需 `chassisId` + `taskId`）。

---

#### 14. 底盘直控（升降等）

```
POST /openapi/v5/robot/chassis/control
```

控制底盘执行特定机械动作（如升降舱）。

| 字段 | 类型 | 必填 |
|---|---|---|
| appname | String | 是 |
| chassisId | String | 是 |
| action | String | 是 | 如 `"up"`、`"down"` |

内部构建 `simple_lift_control` executor 并调用 `createFlow`。

---

#### 任务流 Executor 类型全览

`executors` 是一个 JSON 数组，每个元素结构为：
```json
{"optionId": "1001", "executionId": "<类型>", "params": {...}}
```

**移动类**

| executionId | 功能 | params 字段 |
|---|---|---|
| `move` | 移动到指定标记点 | `marker`（目标点）、`maxSpeedLinear`（最大线速度，可选） |
| `go_back` | 返回充电桩或待命点 | `marker`（充电桩名）、`type`（`""` = 充电，`"standby"` = 待命点） |
| `self_decision_go_back` | 自主决策返回（WTHT 系列专用） | `marker`（目标点） |
| `move_to_container` | 移动到货柜位置 | `lockerIds`（柜门 ID 数组） |

**举升 / 舱控类**

| executionId | 功能 | params 字段 |
|---|---|---|
| `simple_lift_control` | 底盘举升/下降上仓 | `action`：`"up"` 举起，`"down"` 放下 |
| `lift_cabin_control` | 上舱举升/下降（带舱编号） | `action`（`"up"`/`"down"`）、`cabinKey`（上舱编号）、`cabinType`（上舱类型，可选） |
| `docking_cabin` | 底盘与上舱对接 | `cabinKey`（上舱编号）、`marker`（对接标记点）、`hcp`（对接高度补偿） |

**配送 / 交互类**

| executionId | 功能 | params 字段 |
|---|---|---|
| `delivery` | UP 系列配送（移动+等待取件） | `target`（目标点）、`action`（交互动作）、`lockers`（柜格列表）、`waitOperatingTimerMilliseconds`（超时，默认 240000ms） |
| `delivery_with_screen_cabin` | 带屏舱配送 | `cabinKey`、`target`、`action`、`autoIdentificationDoor`（自动识别门）、`lockers`（柜格列表） |
| `customer_flow` | UP300 客户交互流程（巡游宣传等） | `scheduleType`（`"once"`）、`speechType`（`"border"`）、`executorMarkers`（标记点+语音列表） |

**语音 / 辅助类**

| executionId | 功能 | params 字段 |
|---|---|---|
| `speech` | 语音播报 | `type`（`"tts"` = 文字转语音）、`resource`（播报内容） |
| `wait` | 等待指定时间 | `timerMilliseconds`（等待毫秒数） |
| `self_decision_go_back` | 自主决策返回 | 无额外参数 |

**清扫类**

| executionId | 功能 | params 字段 |
|---|---|---|
| `sweep` | 按区域清扫（扫地机器人） | `zones`（区域数组，每项含 `zoneName`）、`hcp`（补偿值，默认 2） |

**工厂调度类**

| executionId | 功能 | params 字段 |
|---|---|---|
| `factory` | 工厂 AGV 调度任务 | 由 `ScheduleApi` 自动构建，外部不需要手动拼 |

> **来源文件**：
> - `ExecutorAction.java`（`open-robot-call-api/devicectrl/`）
> - `JinjiangRequestUtils.java`（`open-robot-call-api/web/api/jinjiang/`）
> - `WtDeviceLowBatteryJobHandler.java`（`open-api/config/`）
> - `UpRobotV5ControlApi.java:156`（`simple_lift_control` 拼装位置）

---

#### 15. 模板化提交任务流

```
POST /openapi/v5/robot/flow/submit/template
```

通过预定义模板（`FlowTemplateRequest`）提交任务，由 `RobotFlowService.parseFlowRequest` 解析为 FlowRequest。

---

#### 16. 通用一体机任务提交（内部接口）

```
POST /openapi/v5/common/robot/task/submit
```

appname 鉴权，适配底盘（WATER/TH/TB/TC）和医疗一体机（上仓绑底盘）两种场景。

---

#### 17. 医疗机器人 H5 召唤接口

```
POST /openapi/v5/robot/task/submit
```

用户 token 鉴权（H5 页面使用）。支持不指定 `productId` 时系统自动分配底盘和上仓。

| 字段 | 类型 | 说明 |
|---|---|---|
| token | String | 用户登录 token |
| productId | String | 可为空，空则自动分配 |
| taskType | String | `call` 或 `call_to_transport` |
| target | String | 目标点 |
| from | String | 取货点（`call_to_transport`） |
| targets | List | 多目标点 |
| liftUp | boolean | 是否需要抬升上仓 |
| liftDown | boolean | 完成后是否降下上仓 |
| itemParams | List | 每个配送目标（boxId + target） |

---

#### 18. 获取机器人详情

```
GET /openapi/v5/robot/info?appname=&productId=
```

从 Redis 读取机器人实时状态（relevance、missionKey 等字段）。

---

#### 19. 获取机器人当前任务 Key

```
GET /openapi/v5/robot/missionKey?appname=&productId=
```

返回当前 `missionKey`，未知时返回 `"UNKNOWN"`。

---

### 三、V1 辅助接口（RobotV1ControlApi）

#### 20. 自定义 App 视图

```
POST /openapi/v1/robot/appview/customize
POST /openapi/v5/robot/appview/customize  （别名）
```

控制机器人屏幕是否显示自定义 URL（H5 界面）。

| 参数 | 类型 | 必填 |
|---|---|---|
| appname | String | 否 |
| productId | String | 是 |
| customize | boolean | 是 | true = 显示自定义 URL |
| url | String | 否 |

---

#### 21. 创建任务流（底层接口）

```
POST /openapi/v1/chassis/task/flow
POST /openapi/v5/chassis/task/flow  （别名）
```

直接调用 `RobotController.creatTaskFlow`，构建灵活任务流。

---

### 四、V4 旧版接口（RobotV4ControlApi）—— 已废弃

> `@Deprecated`，不建议新业务使用，功能与 V5 相同但走旧命令链。

| 接口 | 路径 | 说明 |
|---|---|---|
| 召唤 | `POST /openapi/v4/robot/call` | 旧版 CallRequest（字段较少） |
| 开门 | `POST /openapi/v4/robot/door/open` | 同 V5 |
| 关门 | `POST /openapi/v4/robot/door/close` | 同 V5 |
| 回充 | `POST /openapi/v4/robot/back` | 同 V5 |
| 取消 | `POST /openapi/v4/robot/task/cancel` | 同 V5 |
| 查询任务事件 | `GET /openapi/v4/robot/task/event/query?requestId=` | 查询任务各阶段事件是否已触发 |

---

### 五、AGV 工厂调度接口（ScheduleApi）

#### 22. AGV 任务调度

```
POST /openapi/v5/agv/schedule/task/submit
POST /openapi/v5/agv/schedule/task/callback  （别名）
```

适用于工厂 AGV 场景。自动从 Redis 或分配器获取可用 WT 底盘，构建 `factory` executor 调用外部工厂调度系统。

| 字段 | 类型 | 说明 |
|---|---|---|
| placeId | String | 场地 ID |
| taskId | String | 任务 ID（幂等 key，24h 内同 taskId 复用同一底盘） |
| taskType | String | 工厂任务类型 |
| start | String | 起始点 |
| end | String | 终点 |

---

### 六、锦江专用接口（JinjiangIpassApi）

#### 23. 锦江 iPass 送货

```
POST /openapi/v5/jinjiang/ipass/goods
```

专为锦江集团酒店对接，通过 iPass 系统随机挑选空闲机器人执行配送。

| 字段 | 类型 | 说明 |
|---|---|---|
| thirdpartId | String | 格式 `xxx:syspipe:hotelId` |
| from | String | 取货点 |
| target | String | 目标点 |
| callbackUrl | String | 回调 URL |
| outTaskId | String | 外部任务 ID |
| password | String | 舱门密码 |
| goodsInfo | Object | 货物信息 |

---

## 设计决策

| 决策 | 结论 | 原因 |
|------|------|------|
| 鉴权为何两套（appname+appSecret vs user token） | 第三方系统走 appname+appSecret（服务端到服务端），H5 页面走 user token（浏览器到服务端）；两者安全模型不同，不能混用 |
| executors 为何用 JSON 字符串而非对象数组 | 对接方语言多样，JSON 字符串最通用；服务端按 executionId 动态路由，无需强类型反序列化 |
| V5 Flow 为何引入 missionKey | taskId 创建时固定，missionKey 在 relet/update 时可刷新，二者解耦避免旧 missionKey 失效后无法续租 |
| schedule 接口鉴权为何是场地级而非机器人级 | 调度时不指定具体机器人，由系统分配；只要有场地权限就够，无需提前知道哪台机器人执行 |
| AGV taskId 24h 内复用同一底盘 | 工厂流水线同一任务可能被多次提交，幂等 key 防止重复分配底盘浪费资源 |

## 通用回调机制

当请求携带 `callbackUrl` + `callbackEventList` 时，任务执行期间每触发一个事件，系统会 POST 回调到指定地址。

**回调体字段：**

| 字段 | 说明 |
|---|---|
| requestId | 对应请求的唯一 ID |
| productId | 机器人编号 |
| eventType | 事件类型（见上方事件表 + `task_execute_success` / `task_execute_fail`） |
| message | 事件说明信息 |

---

## 架构流程（简化）

```
API (权限校验)
  → RequestHandleService (幂等 + MongoDB 记录)
    → RobotTaskListener (MQTT 监听注册)
      → DeviceControlService (构建命令链)
        → TaskCommand 链 (MoveCommand → WaitTaskEventCommand → ...)
          → RobotController SDK / MQTT
            → CallbackExecutor (回调通知调用方)
```

调度任务流：
```
/robot/schedule → Redis 队列 → RobotScheduler (每10s) → RobotAllocator → RequestHandleService
```

---
tags: [架构, datacenter, data-push, mqtt, 推送]
date: 2026-04-23
project: datacenter
status: done
scope: datacenter
generalized: false
retrieval_triggers: [数据推送, data-push, mqtt推送, 合作方对接, 心跳推送, 任务推送]
---

# data-push 合作方对接文档

新增合作方只需实现一个 Sender 类 + DB 配置两张表，无需重启，`/config/reload` 热加载即可生效。

## 数据流

```
MQTT Broker（内/外部）
  └─ MqttMsgReceiver（每种 DataType 独立；心跳双 Broker 共享同一个 Queue）
       └─ MsgQueue（CircularQueue，队满丢弃最旧消息防内存溢出）
            └─ RobotMsgDispatcher（解析后调用 QueueManager.addRobotMsg）
                 └─ QueueManager（productId → appname 路由）
                      └─ RobotMsgSender（线程池轮询，超时补偿重试 5 次）
                           └─ 合作方 HTTP 接口
```

> Kafka 在配置中标注为"备用"，当前实际接入层为 MQTT。

## Receiver 全景（msg-bean.xml）

每种 DataType 独立一套 Receiver→Queue→Dispatcher 三元组。心跳/任务等核心数据有 old（外部 broker）+ new（内部 broker）双 Receiver 共享同一 Queue，实现冗余容灾而非重复消费。

| Receiver | 订阅 Topic | DataType | RobotBindFilter | Broker |
|---|---|---|---|---|
| RobotBaseinfoReceiver | `robot/+/+/topic/base_info` | BASE_INFO(1) | ❌ | 外部 |
| Old/RobotHeartbeatReceiver | `robot/+/+/topic/heartbeat` `robot/+/+/topic/robot_status` `robot/nvwa/+/topic/position` `place/+/+/+/+/position` | HEARTBEAT(2) | ✅ | 外部+**内部** |
| Old/new/RobotTaskStatusReceiver | `robot/+/topic/task_status` `robot/+/+/topic/task_status` `robot/+/+/topic/task/status` `robot/+/+/topic/task/+/events` | TASKSTATUS(4) | ✅ | 外部+内部+task专用 |
| Old/RobotExceptionReceiver | `robot/+/+/topic/exception` | EXCEPTION(8) | ❌ | 外部+内部 |
| Old/RobotTaskDetailReceiver | `robot/+/+/topic/flow` | TASKDETAIL(16) | ❌ | 外部+内部 |
| Old/RobotMapStateReceiver | `robot/+/topic/robot_maps` `robot/+/+/topic/robot_maps` | MAP_STATE(32) | ❌ | 外部+内部 |
| RobotCommentReceiver | `robot/+/topic/robot_comment` | COMMENT(64) | ❌ | 外部 |
| **ChassisHeartbeatReceiver** | `robot/water\|ocean\|wt/+/topic/robot_status` | HEARTBEAT_CHASSIS(128) | ❌ | **内部** |
| RobotPowerReceiver | `robot/water\|ocean\|wt/+/topic/power_status` | POWER_STATUS(256) | ❌ | 内部 |
| **ChassisTaskReceiver** | `robot/water\|ocean\|wt/+/topic/task` | TASKDETAIL_CHASSIS(512) | ❌ | **内部** |
| RobotDiagnosisReceiver | `robot/+/+/topic/diagnosis` | DIAGNOSIS | ❌ | 内部 |
| RobotNotificationReceiver | `robot/+/+/topic/notification` | NOTIFICATION | ❌ | 内部 |
| Old/RobotBootInfoReceiver | `robot_bootup/shutdown/poweroff_info` | BOOT_INFO | ✅ | 外部+内部 |
| MaidianInfoReceiver | Kafka `hdos-data-sync` | MAIDIAN_INFO | ❌ | **Kafka（唯一）** |
| RobotDoorStatusReceiver | `robot/+/+/topic/door_status` | DOOR_STATUS | ❌ | 内部 |
| MqttConnectionReceiver | `$SYS/.../connected\|disconnected` | MQTT_CONNECTION | ❌ | 内部 |

**RobotBindFilter 只挂在 4 类 Consumer**：HEARTBEAT、TASKSTATUS、BOOT_INFO、（TASKSTATUS 三路）。底盘心跳（HEARTBEAT_CHASSIS）和底盘任务（TASKDETAIL_CHASSIS）**不经 Filter**，用底盘原始 productId 路由。

## ⚠️ 易错：ChassisHeartbeatReceiver 与 RobotHeartbeatReceiver 存在 topic 重叠

RobotHeartbeatReceiver 订阅 `robot/+/+/topic/robot_status`（通配符），ChassisHeartbeatReceiver 订阅 `robot/wt/+/topic/robot_status`（精确前缀）。

两者使用**相同 share group 名但不同 topic pattern**，在 EMQX 中属于两个独立订阅组，同一条 `robot/wt/WTYS.../topic/robot_status` 消息会**各投递一次**：

```
robot/wt/WTYS.../topic/robot_status
  ├─ → RobotHeartbeatReceiver  → robotHeartbeatQueue  → DataType=HEARTBEAT(2)
  └─ → ChassisHeartbeatReceiver → chassisHeartbeatQueue → DataType=HEARTBEAT_CHASSIS(128)
```

| 合作方 push_type 含哪些位 | 收到几次底盘 robot_status |
|---|---|
| 只含 HEARTBEAT(2) | 1次 |
| 只含 HEARTBEAT_CHASSIS(128) | 1次 |
| 同时含 2+128 | **2次，内容相同，dataType 不同** |

push_type=518 只含 HEARTBEAT(2)，不含 128，不受此影响。

## MQTT Topic 与 DataType 对应（简览）

| Topic 模式 | DataType | 说明 |
|---|---|---|
| `robot/+/+/topic/base_info` | BASE_INFO(1) | 机器人基础信息 |
| `robot/+/+/topic/heartbeat` | HEARTBEAT(2) | 通用心跳，含 SC/UP/cabin/up 子类型 |
| `place/+/+/+/+/position` | HEARTBEAT(2) | 定位心跳，仅含坐标和楼层 |
| `robot/+/+/topic/task_status` 等 | TASKSTATUS(4) | 任务状态事件流 |
| `robot/+/+/topic/exception` | EXCEPTION(8) | 异常上报 |
| `robot/+/+/topic/flow` | TASKDETAIL(16) | 任务完结详情（flow topic） |
| `robot/wt\|water\|ocean/+/topic/robot_status` | HEARTBEAT_CHASSIS(128) | 底盘精细状态 |
| `robot/wt\|water\|ocean/+/topic/task` | TASKDETAIL_CHASSIS(512) | 底盘任务完结 |

**topic → type/productId 提取**（`AbstractRobotMsgParser`）：
- 新格式 `robot/{type}/{productId}/topic/...`：正则 group(1)=type（大写），group(2)=productId
- 旧格式 `robot/{productId}/topic/...`：group(1)=productId，type 固定为 "NVWA"

`PushConfig.pushType` 用位掩码叠加：`pushType & DataType.XXX > 0` 判断是否推该类型。例如同时推心跳和任务 = 2+4 = 6。

## 消息结构

### HEARTBEAT（RobotHeartbeat）

完整状态心跳字段：
- 基础：`productId`, `type`, `floor`, `ts`
- 电量：`powerPercent`, `chargeState`（true=充电中）
- 状态：`estop`/`estopState`, `idle`, `runningStatus`
- 定位：`position`（x,y,z 地图坐标系，非经纬度），`orientation`（四元数 w,x,y,z）
- 扩展：`lockers`（舱门信息，结构因机器人型号而异，见下方）

**位置心跳**（position topic）只含 position/orientation/floor，无电量和运行状态字段。

### TASKDETAIL（RobotTaskDetail）

`taskId`, `taskType`, `result`, `startTime`, `endTime`, `distance`, `stages`（JSONArray）, `retryTimes`, `extraData`

## Sender 两种推送模式

### 全量透传（CommonRobotMsgSender 风格）
消息对象直接序列化为 JSON，Header 或 Body 追加鉴权字段：

```
sign = MD5(appname + ts + body_json_string)   // 32位小写十六进制
```

appname/ts 放 HTTP Header，sign 放 Body JSON。代表：saite、rujia、wanda 等。

### 字段重建（HuazhuRobotMsgSender 风格）
只取部分字段，按合作方协议重组报文（如 `productState: "online"/"offline"`）。代表：huazhu、meituan、shangou 等。

## 新增合作方：两种方式

### 方式一：不写代码（纯 DB 配置）

`push_config.sender_name` 留空 → 工厂自动使用 `CommonRobotMsgSender`（全量透传 + MD5 sign）。

适用于能接受标准格式的合作方，只需配：
- `appname` / `sender_name`（留空）/ `push_type` / `push_url` / `secret`
- `app_robot` 表绑定 `productId`

### 方式二：自定义 Sender（代码 + DB）

合作方需要字段裁剪、协议重组、特殊鉴权时，写代码：

1. `sender/impl/<name>/` 下继承 `AbstractRobotMsgSender`，实现 `doSendMsg()`
2. 类上加 `@Component("YOUR_SENDER_NAME")`（Bean 名即 DB 中 `sender_name` 的值）
3. DB `push_config.sender_name` 填写与 `@Component` 相同的名称

**工厂机制**：`RobotMsgSenderFactoryImpl` 通过 `@Autowired Map<String, RobotMsgSender>` 自动收集所有 Sender Bean；查 `sender_name` 命中则 clone，未命中则 fallback 到 `CommonRobotMsgSender`。

已注册的 Bean 名（sender_name 参考值）：`HUAZHU` / `MEITUAN` / `RUJIA` / `SHANGOU` / `MINSHAN` / `WANDA` / `LIANTONG` / `ROCOS` / `YADUO` / `HAIQUE` / `DONGCHENG` / `CMCC` / `XIAOZHI` / `CHUANGZE`

配置变更后调用 `GET /config/reload` 触发热加载，无需重启。

## 已对接合作方（sender/impl/）

| 目录 | 合作方 | 模式 |
|---|---|---|
| `huazhu/` | 华住 | 字段重建 |
| `meituan/` | 美团 | 字段重建 |
| `rujia/` | 如家 | 全量透传 |
| `shangou/` / `shangou2/` | 闪购 | 字段重建 |
| `minshan/` | 岷山 | 字段重建 |
| `liantong/` | 联通 | — |
| `rocos/` | Rocos | — |
| `yaduo/` | 雅朵 | — |
| `haique/` | 海雀 | — |
| `xiaozhi/` | 小知 | — |
| `huazhuiot/` | 华住IoT | — |
| `saite/` | 赛特 | 全量透传 |
| `cmcc/` | 中国移动 | — |

## GEGE（格格）机器人特殊处理

GEGE 舱门结构为 `Map<String, LockerInfo>`（键为舱门编号），与润用等的 `List<LockerInfo>` 不同，需用 `RobotDoorStatusParser.getGegeLockers()` 解析。

| 合作方 | 特殊逻辑 |
|---|---|
| meituan | 心跳过滤 `estopState==null` 的消息不推送 |
| shangou | 2 个舱门逐个单独推送（不合并） |
| rujia | GEGE 机器人设置 `warehouse=2` |
| minshan | 多舱未取物逐个推工单 |

## 配置热加载

- `ConfigManager` 每 2 小时定时从 DB 重载 `PushConfig` 和 `AppRobot`
- `QueueManager`、`SenderManager` 实现 `ConfigListener`，收到变更通知后动态增删队列和 Sender 实例
- 手动触发：`GET /config/reload`（ConfigApi）

## 关键约定

- `hotelId` 只在 BASE_INFO 消息时从消息体更新，心跳/任务消息复用上次缓存值
- `placeId` 在 QueueManager 中双向补全（productId ↔ placeId 互查）
- MQTT topic 中 `@env@` 由 Maven profile filtering 替换为实际环境标识（test/prod）
- 心跳配置了内/外部两个 MqttReceiver，共享同一个 Queue，防止重复消费

## Web API

| 接口 | 说明 |
|---|---|
| `GET /config/reload` | 手动触发配置重载 |
| `GET /sender/list` | 查看当前运行中的 Sender 列表 |
| `GET /queue/status` | 查看队列状态 |

## 关键文件索引

| 职责 | 文件 |
|---|---|
| MQTT 接入配置 | `resources/bean/msg-bean.xml` |
| topic 解析 | `msg/parser/AbstractRobotMsgParser.java` |
| 消息分发 | `msg/consumer/RobotMsgDispatcher.java` |
| 路由 & 队列 | `sender/QueueManager.java` |
| 发送器工厂 | `sender/RobotMsgSenderFactory.java` |
| 发送器管理 | `sender/SenderManager.java` |
| 配置加载 | `sender/config/ConfigManager.java` |
| 舱门解析 | `msg/parser/RobotDoorStatusParser.java` |

> 所有路径相对于 `datacenter/data-push/src/main/java/ai/yunji/rw/data/push/`

## 相关链接

- [[pitfalls-open-yuncang-api-sign]]
- [[arch-iot-platform]]

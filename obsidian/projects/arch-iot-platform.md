---
tags:
  - 架构
  - iot
  - 设计方案
date: 2026-04-20
project: iot-platform
status: draft
confidence: 设计阶段未落地，待验证
---

# IoT 通用设备接入平台 —— 整体架构设计

## 项目定位

新建独立平台，屏蔽底层设备差异（协议、验签、接入方式），向上层业务系统暴露统一的设备能力调用接口。

**典型场景**：咖啡机制作咖啡、煮面机器人煮面等商业 IoT 设备能力的统一接入与调度。

---

## 技术栈

| 组件 | 选型 | 用途 |
|------|------|------|
| 核心框架 | Spring Boot 3.x | 各服务统一框架 |
| 关系数据库 | MySQL 8 | 设备注册、厂商配置、任务终态持久化 |
| 缓存 | Redis | 活跃任务状态、设备在线状态 |
| 消息队列 | Kafka | 平台核心 ↔ Adapter 解耦、事件流 |
| MQTT Broker | EMQX（自建集群）| 设备直连 MQTT 接入 |
| 历史事件 | ClickHouse | 时序事件存储，报表分析 |
| Adapter 写入链路 | Kafka → ClickHouse Kafka 引擎 | 秒级延迟批量写入 |

---

## 系统分层架构

```
┌─────────────────────────────────────────────────────┐
│                   业务系统（调用方）                    │
│         REST API / Webhook 回调 / MQ 消费              │
└─────────────────────┬───────────────────────────────┘
                      │ 北向接口
┌─────────────────────▼───────────────────────────────┐
│                  平台核心（Core）                       │
│  设备管理  │  能力路由  │  任务管理  │  事件分发          │
└──────┬──────────────────────────────┬───────────────┘
       │ Kafka 指令下发                │ Kafka 事件上报
┌──────▼──────┐              ┌────────▼──────────────┐
│  Adapter A  │              │      Adapter B         │
│（厂商/品类）  │              │    （厂商/品类）         │
└──────┬──────┘              └────────┬───────────────┘
       │ HTTP / MQTT                  │ HTTP / MQTT
  厂商云 / 设备                    厂商云 / 设备
```

---

## 核心模块设计

### 1. 设备接入层

**支持协议**
- HTTP/HTTPS（轮询 & Webhook 回调）
- MQTT（QoS 0/1/2，TLS，via EMQX）

**接入方式**

| 方式 | 描述 |
|------|------|
| 云云对接 | 平台调用厂商云 API；厂商云通过 Webhook 推事件到平台 |
| 设备直连 HTTP | 设备直接与平台 HTTP 通信 |
| 设备直连 MQTT | 设备通过 EMQX 与平台通信 |

**验签策略（可插拔，Strategy 模式）**
- HMAC-SHA256 消息签名
- API Key + Secret
- OAuth2 Client Credentials
- 自定义扩展（实现 `SignStrategy` 接口）

---

### 2. 物模型（Thing Model）

每个**品类**定义标准物模型，屏蔽厂商差异：

```
品类（DeviceCategory）
├── 属性（Property）：设备状态，如 status / temperature
│     └── 读写类型：只读 / 可写
├── 动作（Action）：可调用的能力，如 makeCoffee / cookNoodle
│     ├── 入参定义（JSON Schema）
│     ├── 出参定义（JSON Schema）
│     └── 执行模式：同步 / 异步
└── 事件（Event）：设备主动上报，如 orderCompleted / errorOccurred
      └── 数据字段定义
```

**执行流程**
```
业务调用 makeCoffee(deviceId, params)
  → 平台路由 → 找到设备对应的 Adapter
  → Adapter 组装厂商请求 + 签名
  → HTTP/MQTT 发送给厂商云或设备
  → 异步：返回 taskId，设备完成后回调平台
  → 同步：等待设备响应后返回
```

---

### 3. Adapter（适配器）

**部署方式**：独立 Spring Boot 微服务，每个厂商/品类一个 Adapter。

**标准接口规范**

下行（平台 → Adapter）：
```
Kafka Topic: iot.adapter.{adapterId}.command
Payload:
{
  "taskId": "xxx",
  "deviceId": "xxx",
  "actionName": "makeCoffee",
  "params": { ... },
  "callbackTopic": "iot.platform.callback"
}
```

上行 - 任务回调（Adapter → 平台）：
```
Kafka Topic: iot.platform.callback
Payload:
{
  "taskId": "xxx",
  "status": "COMPLETED|FAILED",
  "result": { ... },
  "rawResponse": "..."
}
```

上行 - 设备事件（Adapter → 平台）：
```
Kafka Topic: iot.platform.events
Payload:
{
  "adapterId": "xxx",
  "deviceId": "xxx",
  "eventName": "orderCompleted",
  "payload": { ... },
  "timestamp": 1713600000000
}
```

**新增厂商只需**：实现 Adapter 服务，消费/发布上述 Topic，平台核心不改动。

---

### 4. 任务生命周期管理

**状态机**
```
PENDING → SENT → PROCESSING → COMPLETED
                             → FAILED
                             → TIMEOUT
```

**存储策略**（双写）

| 状态 | 存储 | 说明 |
|------|------|------|
| 执行中（PENDING/SENT/PROCESSING）| Redis | TTL = 超时时间 + buffer，快速读写 |
| 终态（COMPLETED/FAILED/TIMEOUT）| MySQL | 异步落库，供审计和历史查询 |

**好处**：Redis 只存活跃任务，MySQL 只写一次终态，两者压力都小。

---

### 5. 北向接口（业务使用层）

**REST API**
```
POST   /api/devices/{deviceId}/actions/{actionName}   # 执行动作
GET    /api/devices/{deviceId}/properties             # 查询属性
GET    /api/devices/{deviceId}/tasks/{taskId}         # 查询任务状态
GET    /api/devices                                   # 设备列表（按 locationId 过滤）
```

**事件推送**（设备事件 → 业务）
- Webhook：业务注册回调 URL，平台主动推送
- MQ：平台发布到 Kafka，业务自行消费
- 轮询接口（降级方案）

**鉴权**：API Key（按调用方颁发）

---

### 6. 历史事件存储（ClickHouse）

**写入链路**
```
Adapter 上报事件 → Kafka(iot.platform.events) → ClickHouse Kafka 引擎（批量消费）
```

**表设计思路**
- 按品类建表，字段对应物模型中的事件定义
- 分区键：`toYYYYMM(timestamp)`（按月分区）
- 主索引：`(device_id, timestamp)`

**查询场景**
- 指定设备 + 时间范围的事件列表
- 按品类 / 时间窗口的聚合统计（报表）
- 事件异常分布分析

---

## 数据模型（MySQL）

```
DeviceCategory（品类）
  id, name, description
  └── ThingModel JSON（属性/动作/事件的 Schema 定义）

Vendor（厂商）
  id, name

VendorConfig（厂商接入配置）
  vendor_id, adapter_id
  auth_type（HMAC/API_KEY/OAUTH2/CUSTOM）
  config_json（加密存储：AppKey/Secret/API地址等）

Device（设备实例）
  id, sn, category_id, vendor_id, location_id
  device_config_json（MQTT Topic 规则、设备级参数等）
  status（ONLINE/OFFLINE/ERROR）

ActionTask（动作任务 - 终态）
  id, device_id, action_name
  status, params_json, result_json
  raw_request, raw_response
  created_at, completed_at

Subscription（业务订阅）
  app_id, device_id / category_id（订阅粒度）
  callback_url / kafka_topic
  event_filter（订阅哪些事件类型）
```

---

## 多租户处理

设备归属由**外部系统**管理，平台仅感知不管理：
- Device 表存 `location_id` 字段，值来自外部系统
- 业务调用北向 API 时带 `locationId`，平台做过滤
- 外部系统设备归属变更时，通过 API 同步更新平台 `location_id`

---

## 非功能要求

| 项目 | 要求 |
|------|------|
| 扩展性 | 新品类/厂商只需新建 Adapter 服务，平台核心不改 |
| 可靠性 | Kafka 保证消息至少一次投递；任务超时自动标 TIMEOUT |
| 安全性 | 厂商密钥 AES 加密存储；北向 API Key 鉴权；租户数据过滤 |
| 可观测性 | traceId 贯穿业务调用→平台→Adapter→设备全链路 |
| 幂等性 | 指令下发去重（taskId）；设备事件去重（事件 ID + 设备 ID）|

---

## 落地阶段规划

### Phase 1 —— 核心骨架
- 平台核心服务：设备管理、任务管理、北向 REST API
- 首个 Adapter：选一个接入最简单的厂商验证流程
- MySQL + Redis + Kafka 基础设施搭建

### Phase 2 —— 能力完善
- EMQX 接入，支持设备直连 MQTT
- 事件推送：Webhook + MQ 双通道
- ClickHouse 接入，历史事件写入与查询 API

### Phase 3 —— 生产加固
- 链路追踪（SkyWalking / OpenTelemetry）
- 设备状态大盘、任务成功率告警
- 多 Adapter 压测，调整 Kafka 分区策略

---

## 待决策 / 遗留问题

- [ ] 同步动作的超时时间怎么配置（全局 vs 品类级 vs 动作级）
- [ ] EMQX 集群节点数和 Topic 规范（按 deviceId 还是按 category 分 Topic）
- [ ] ClickHouse 集群规模初始评估（单机还是 3 节点）
- [ ] Adapter 服务的部署方式（K8s / 独立 VM）
- [ ] 北向 API 是否需要 SDK（Java/Python 客户端）

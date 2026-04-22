---
tags:
  - 架构
  - datacenter
  - data-push
date: 2026-04-18
project: datacenter
status: done
---

# data-push 整体架构

## 项目定位

机器人数据推送服务——从 Kafka 接收机器人实时消息，按配置规则分发到各合作方系统。

## 技术栈

Spring Boot 2.x · MyBatis · PostgreSQL · Redis/Redisson · Kafka · Hessian RPC · HTTP Client

## 分层架构

```
┌─────────────────────────────────────────────────────┐
│                    API 层 (web/api)                   │
│    SenderApi  ·  ConfigApi  ·  QueueApi              │
│    （查询发送器状态、配置管理、队列监控）              │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│               消息接入层 (receiver)                    │
│               KafkaMsgReceiver                        │
│            （从 Kafka Topic 拉取原始消息）             │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│            消息解析与分发层 (msg)                      │
│                                                       │
│  ┌─────────────────────┐  ┌──────────────────────┐   │
│  │ AbstractRobotMsg     │  │ DefaultRobotMsg      │   │
│  │ Parser               │→ │ Parser               │   │
│  └─────────┬───────────┘  └──────────────────────┘   │
│            │                                          │
│  ┌─────────▼───────────────────────────┐              │
│  │ CommonMsgDispatcher                  │              │
│  │ RobotMsgDispatcher                   │              │
│  │ （按数据类型路由到不同队列）         │              │
│  └─────────┬───────────────────────────┘              │
│            │  RobotBindService（机器人绑定关系）       │
└────────────┼─────────────────────────────────────────┘
             │
┌────────────▼─────────────────────────────────────────┐
│              队列层 (sender/queue)                     │
│    QueueManager  ·  CircularQueue                     │
│    （按 appname 分队列，环形缓冲）                    │
└────────────┬─────────────────────────────────────────┘
             │
┌────────────▼─────────────────────────────────────────┐
│            发送器工厂层 (sender)                       │
│                                                       │
│  SenderManager → RobotMsgSenderFactory                │
│    （按 PushConfig 动态创建/启停发送器实例）          │
│                                                       │
│  RobotMsgSender（接口）                               │
│    └─ AbstractRobotMsgSender（抽象基类，含重试）      │
│         ├─ HuazhuRobotMsgSender     华住              │
│         ├─ MeituanRobotMsgSender    美团              │
│         ├─ RuJiaRobotMsgSender      如家              │
│         ├─ ShangouRobotMsgSender    闪购              │
│         ├─ MinshanRobotMsgSender    岷山              │
│         ├─ LianTongRobotMsgSender   联通              │
│         ├─ DaliHospitalRobotMsg...  大理医院          │
│         └─ DongChengMqttPublisher   东城 MQTT         │
└──────────────────────────────────────────────────────┘
             │ HTTP / MQTT
             ▼
      各合作方外部系统
```

## 核心数据流

```
Kafka → KafkaMsgReceiver → RobotMsgDispatcher → QueueManager → SenderManager → 各合作方 Sender → 外部系统
                                          ↓
                                    RobotBindService（查询机器人绑定关系）
```

## 配置驱动

| 组件 | 职责 |
|---|---|
| `ConfigManager` | @Scheduled 定时从 DB 加载 PushConfig，驱动发送器创建 |
| `ConfigServiceImpl` | 查询 PushConfig、AppRobot 配置 |
| `ConfigMapper.xml` | MyBatis 映射，多数据源 |
| `RoutingDataSource` | AOP 切面动态切换数据源（DW_XZ、SCENE 等） |

## 已对接合作方（sender/impl/）

| 目录 | 合作方 | 推送方式 |
|---|---|---|
| `huazhu/` | 华住 | HTTP REST |
| `meituan/` | 美团 | HTTP REST |
| `rujia/` | 如家 | HTTP REST |
| `shangou/` | 闪购 | HTTP REST |
| `minshan/` | 岷山 | HTTP REST |
| `liantong/` | 联通 | HTTP REST |
| `dalihospital/` | 大理医院 | HTTP REST |
| `dongcheng/` | 东城 | MQTT |

## 新增合作方流程

1. 在 `sender/impl/` 下新建目录，继承 `AbstractRobotMsgSender`
2. 在 `RobotMsgSenderFactory` 注册新 appname
3. 数据库 `PushConfig` 表新增配置记录
4. `ConfigManager` 定时刷新后自动生效

## 关键文件索引

| 层级 | 文件 | 路径 |
|---|---|---|
| 入口 | KafkaMsgReceiver | `receiver/KafkaMsgReceiver.java` |
| 分发 | RobotMsgDispatcher | `msg/consumer/RobotMsgDispatcher.java` |
| 分发 | CommonMsgDispatcher | `msg/consumer/CommonMsgDispatcher.java` |
| 解析 | AbstractRobotMsgParser | `msg/parser/AbstractRobotMsgParser.java` |
| 队列 | QueueManager | `sender/QueueManager.java` |
| 队列 | CircularQueue | `sender/queue/CircularQueue.java` |
| 工厂 | RobotMsgSenderFactory | `sender/RobotMsgSenderFactory.java` |
| 管理 | SenderManager | `sender/SenderManager.java` |
| 配置 | ConfigManager | `sender/config/ConfigManager.java` |
| 数据源 | RoutingDataSource | `aspect/datasource/RoutingDataSource.java` |
| API | SenderApi / ConfigApi / QueueApi | `web/api/` |

> 所有路径相对于 `datacenter/data-push/src/main/java/ai/yunji/rw/data/push/`

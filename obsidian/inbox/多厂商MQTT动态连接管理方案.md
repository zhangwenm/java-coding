---
tags:
  - 架构设计
  - MQTT
  - IoT
date: 2026-05-07
status: draft
retrieval_triggers:
  - 多厂商MQTT接入
  - 动态连接管理
  - MQTT指令下发
  - 设备连接池
---

# 多厂商 MQTT 动态连接管理方案

> 场景：每家厂商有独立的 MQTT broker，需要可配置、动态建连、支持双向消息（上行监听 + 下行指令）。

## 1. 配置存储（DB 表）

```
mqtt_vendor_config
├── id
├── vendor_code              # 厂商唯一标识，如 "vendor_a"
├── broker_url               # tcp://host:1883
├── username
├── password                 # 加密存储，连接前解密
├── client_id_prefix         # 拼上随机后缀避免冲突
├── subscribe_topic_template # 上行 topic 模板，如 "/{vendorCode}/device/{deviceId}/report"
├── publish_topic_template   # 下发 topic 模板，如 "/{vendorCode}/device/{deviceId}/cmd"
├── topics                   # JSON 数组，订阅的 topic 列表
├── qos
├── publish_qos              # 下发默认 QoS
├── publish_retain           # 是否 retain
├── cmd_timeout_ms           # 等待 ACK 超时时间
├── enabled                  # 开关，false 时不建立连接
├── reconnect_interval       # 断线重连间隔（秒）
└── extra_options            # JSON，扩展字段（keepalive、ssl 证书路径等）
```

## 2. 连接管理器

```
MqttConnectionManager
├── Map<vendorCode, MqttConnection>   # 运行中的连接池
├── loadAll()         # 启动时读全部 enabled 配置，批量建连
├── connect(config)   # 建立单条连接，订阅 topics，注册 messageHandler
├── disconnect(vendorCode)     # 优雅关闭
├── reload(vendorCode)         # 先断后连（配置变更时调用）
└── healthCheck()              # 定时检测，断线自动重连
```

每个 `MqttConnection` 实例双向复用（上行 subscribe + 下行 publish），不分开维护。

## 3. 指令下发（MqttCommandSender）

```
MqttCommandSender
├── send(vendorCode, deviceId, payload)
│     → 从连接池拿到 MqttClient
│     → 用 publish_topic_template 渲染实际 topic
│     → publish(topic, payload, qos, retain)
│
├── sendAndWait(vendorCode, deviceId, payload, timeout)
│     → 发送后挂起等待 ACK/响应（correlationId 对应）
│
└── broadcast(vendorCode, payload)
      → 下发到该厂商所有设备
```

业务层只传 `vendorCode + deviceId + payload`，不感知 topic 格式和连接细节。

## 4. ACK 追踪

```
PendingCommandRegistry
├── Map<correlationId, CompletableFuture>
├── register(correlationId)           # 下发前注册
├── complete(correlationId, response) # 上行回调时触发
└── timeout 自动清理
```

下发 payload 携带 `correlationId`，设备响应原样返回，上行 handler 根据 id 找到 Future 并 complete。

## 5. 消息路由（上行）

收到消息 → 从 MqttClient 标识拿到 vendorCode → 路由到对应 `VendorMessageHandler` → 各厂商自己实现 `handle(topic, payload)`

用策略模式：`Map<vendorCode, VendorMessageHandler>`，按厂商注入不同处理逻辑。

## 6. 配置变更触发重连

| 方式 | 说明 |
|------|------|
| REST 管理接口（主动推） | 即时生效，运维调用后立即 reload |
| 定时轮询 DB（兜底） | 每 30s 检查，用于保活和补偿 |

推荐两者结合：接口触发 + 轮询兜底。

### 管理接口

```
POST   /mqtt/vendors               # 新增配置并建连
PUT    /mqtt/vendors/{code}        # 更新配置并重连
DELETE /mqtt/vendors/{code}        # 断连并删配置
POST   /mqtt/vendors/{code}/reload # 手动重连
GET    /mqtt/vendors/status        # 查看各厂商连接状态
```

## 7. 整体架构

```
                ┌─────────────────────────────────────┐
                │         MqttConnectionManager        │
                │  Map<vendorCode, MqttConnection>     │
                └────────────┬────────────┬────────────┘
                             │            │
                ┌────────────▼──┐    ┌────▼───────────────────┐
                │  上行 Subscribe │    │    下行 Publish         │
                │               │    │                        │
                │ VendorMessage │    │ MqttCommandSender      │
                │ Handler       │    │ + PendingCommandRegistry│
                │ （策略模式）  │    │   （ACK 追踪）          │
                └───────────────┘    └────────────────────────┘
                       ↑                        ↑
                设备上报消息                业务服务调用下发
```

## 8. 关键边界处理

- **clientId 冲突**：`{prefix}_{vendorCode}_{UUID前8位}`
- **SSL/TLS**：`extra_options` 存证书路径，连接时动态加载
- **密码安全**：DB 存加密值，连接前解密，不明文落库
- **启动顺序**：`@PostConstruct` 触发 `loadAll()`，单条失败不阻塞整体启动
- **优雅停机**：`@PreDestroy` 断开所有连接
- **断线重连**：subscribe 重新注册，publish 直接用无需重注册

## 待确认

- [ ] MQTT 客户端库选型（Eclipse Paho / HiveMQ SDK / Spring Integration MQTT）
- [ ] 指令下发是否需要设备 ACK 确认
- [ ] topic 模板变量维度（除 deviceId 外是否有 roomId、floorId 等）
- [ ] 配置变更触发方式（REST 主动推 / 定时轮询 / 两者）

---
tags: [datacenter, data-collect, 数据库, DDL, IoT, 健康监测, 上线检查]
date: 2026-04-29
project: datacenter
status: draft
scope: datacenter
generalized: false
retrieval_triggers: [data-collect上线, datacenter数据库变更, IoT设备表结构, 健康监测DDL]
---

# data-collect 健康监测+告警功能 —— DB 变更清单

**上线需执行 2 张新表建表 DDL + 1 张已有表加 2 列 + 若干配置数据 INSERT。**

---

## 决策记录

| 方案 | 结论 | 原因 |
|------|------|------|
| 健康数据独立存 MongoDB（原 HealthMsgConsumer 设计） | ❌ 否决 | 与 job-executor 已有 `t_aiot_property_info` 割裂，查询需跨库 |
| 统一走 `t_aiot_property_info` + `AiotPropertyMsgConsumer` | ✅ 采纳 | 复用 job-executor 成熟的 upsert + SpEL 映射机制，零代码改动接入新设备 |
| 告警数据独立建表 | ✅ 采纳 | 告警（camera + health）语义与属性快照不同，需独立存储和查询 |
| 告警数据并入 `t_aiot_property_info` | ❌ 否决 | 属性表是 upsert 最新值，告警表是 append 事件流水，语义冲突 |

---

## 一、新增表

### 1. `t_device_alarm_event` — 统一设备告警事件表

```sql
CREATE TABLE t_device_alarm_event (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code    VARCHAR(64)  COMMENT '内部设备编码（t_aiot_device.code）',
    third_party_no VARCHAR(128) COMMENT '第三方设备号（Tuya devId / 健康设备 id）',
    source         INT          COMMENT '告警来源：1=摄像头(Tuya)  2=健康设备(Aliyun AMQP)',
    alarm_type     VARCHAR(64)  COMMENT '告警类型。摄像头: IPC_CAR/IPC_PERSON; 健康: breath_rate/heart_rate/leave_bed',
    alarm_time     BIGINT       COMMENT '告警时间戳（ms）',
    title          VARCHAR(256) COMMENT '告警标题',
    content        TEXT         COMMENT '告警内容描述',
    extra          TEXT         COMMENT '扩展字段 JSON。摄像头: {bucket, files}; 健康: 预留',
    is_deleted     INT DEFAULT 0,
    created_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_device_code (device_code),
    INDEX idx_source (source),
    INDEX idx_alarm_time (alarm_time)
);
```

写入方：`DeviceAlarmMsgConsumer`
- `source=1`：涂鸦摄像头 Pulsar 告警（`alarm_message` base64 解码后提取 bucket/cmd/files）
- `source=2`：健康设备阿里云 AMQP warning（type=0/1/2 写库，type=3/4 上下线跳过）

### 2. `t_aiot_property_info` — 设备属性快照表

```sql
CREATE TABLE t_aiot_property_info (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    device_code       VARCHAR(64)   COMMENT '设备编码（t_aiot_device.code）',
    online_status     INT           COMMENT '在线状态：0离线 1在线',
    heartbeat         DATETIME      COMMENT '心跳时间',
    device_status     VARCHAR(128)  COMMENT '设备状态',
    task_status       VARCHAR(128)  COMMENT '任务状态',
    custom_properties TEXT          COMMENT '自定义属性（JSON）。未匹配 AiotPropertyInfo 字段的指标自动归入此处',
    deleted           INT DEFAULT 0,
    created_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_device_code (device_code)
);
```

写入方：`AiotPropertyMsgConsumer`（upsert 模式：存在则 update，不存在则 insert）。
与 job-executor 共用此表，data-collect 写、job-executor 读。

---

## 二、已有表新增列

### `t_data_config`

```sql
ALTER TABLE t_data_config ADD COLUMN receiver_type   VARCHAR(32)   COMMENT '消息源类型：mqtt / pulsar / aliyun_iot_amqp';
ALTER TABLE t_data_config ADD COLUMN receiver_config LONGTEXT      COMMENT 'receiver 的 JSON 配置（连接参数、鉴权等）';
```

`MsgProcessorFactory` 根据 `receiver_type` 路由到对应处理器：
- `mqtt` → 原有 `MsgProcessorImpl`（不改）
- `pulsar` → `PulsarMsgProcessor`
- `aliyun_iot_amqp` → `AliyunIotAmqpMsgProcessor`

---

## 三、已有表无 DDL 变更，但新增 Mapper 查询

| 表名 | 新增 Mapper | 用途 |
|------|------------|------|
| `t_aiot_device` | `selectByThirdPartyNo` | `DeviceAlarmMsgConsumer` 按第三方设备号反查 `device_code` |
| `t_aiot_device_model_map` | `listByProductIdAndType` | `AiotPropertyMsgConsumer` 按 product + type 查 SpEL resultMap |
| `t_aiot_device_model_map` | `listByProductIdAndLinkType` | 同上，健康设备专用（`link_type='health_iot_amqp'`）|

---

## 四、配置数据（INSERT，非 DDL）

上线后需在对应环境执行：

**`t_data_config` — 健康设备 AMQP 处理器：**
```sql
INSERT INTO t_data_config
    (cluster_name, name, receiver_type, receiver_config, queue_size, parser, topics, filters, consumers, threads)
VALUES (
    'GLASS',
    'health_iot_amqp_processor',
    'aliyun_iot_amqp',
    '{"accessKey":"<替换>","accessSecret":"<替换>","consumerGroupId":"<替换>","iotInstanceId":"<替换>","clientId":"data-collect-prod","host":"<替换>.amqp.iothub.aliyuncs.com","connectionCount":4}',
    50000,
    '{"class":"ai.yunji.rw.data.msg.parser.impl.health.HealthIotAmqpMsgParser"}',
    NULL,
    '{"class":"ai.yunji.rw.data.msg.filter.HealthDeviceFilter","paramClass":"ai.yunji.rw.data.msg.filter.HealthDeviceFilter$Param","allowedProductKeys":["i0m7S3JQXku"]};{"class":"ai.yunji.rw.data.msg.filter.HealthAbnormalFilter"}',
    '{"class":"ai.yunji.rw.data.msg.consumer.impl.AiotPropertyMsgConsumer","paramClass":"ai.yunji.rw.data.msg.consumer.impl.AiotPropertyMsgConsumer$Param","linkType":"health_iot_amqp","topic":"health-iot-property"}',
    4
);
```

**`t_aiot_device_model_map` — 健康设备 SpEL resultMap：**
```sql
INSERT INTO t_aiot_device_model_map
    (code, product_id, name, link_type, params, is_deleted)
VALUES (
    'health_iot_amqp',
    NULL,
    '健康监测设备 AMQP 属性上报',
    'health_iot_amqp',
    '{"resultMap":{"heartRate":"#{\\#payload[''heartRate'']}","respiratoryRate":"#{\\#payload[''respiratoryRate'']}","sleepStage":"#{\\#payload[''sleepStage'']}","apneaDuration":"#{\\#payload[''apneaDuration'']}","peoplePresent":"#{\\#payload[''peoplePresent'']}","moving":"#{\\#payload[''moving'']}","signalAmplitude":"#{\\#payload[''signalAmplitude'']}","rssi":"#{\\#payload[''rssi'']}","measureTime":"#{\\#payload[''measureTime'']}","productKey":"#{\\#payload[''productKey'']}"}}',
    0
);
```

**`t_aiot_device` — 每台健康设备维护 code ↔ product_id 映射**（运行前置条件，否则 Consumer 查不到 resultMap 会跳过消息）：
```sql
INSERT INTO t_aiot_device (code, third_party_no, product_id, is_deleted)
VALUES ('<设备8位ID>', '<DeviceName>', <product_id>, 0);
```

---

## 五、上线检查清单

- [ ] `t_device_alarm_event` 表已建
- [ ] `t_aiot_property_info` 表已建
- [ ] `t_data_config` 已加 `receiver_type` + `receiver_config` 列
- [ ] `t_data_config` 健康设备行已插入，`accessSecret` 已替换为真实值
- [ ] `t_aiot_device_model_map` health resultMap 已插入
- [ ] 各健康设备在 `t_aiot_device` 中有对应记录（code=deviceId, product_id 正确）
- [ ] `receiver_config.accessSecret` 生产环境走配置中心注入，**禁止硬编码提交**

---

## 相关链接

- 详细设计文档：`datacenter/data-collect/doc/`（AIOT 通用接入方案、健康监测文档、睡眠设备接入指南）
- [[arch-iot-platform]] — IoT 通用平台架构（data-collect 的配置驱动设备接入模式与此对齐）

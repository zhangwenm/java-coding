---
tags: [datacenter, data-push, 对接, 签名, api, 合作方]
date: 2026-04-23
project: datacenter
status: done
scope: datacenter
generalized: false
retrieval_triggers: [data-push对接, 合作方接收, 验签, CommonRobotMsgSender, 推送格式]
---

# data-push 合作方接收端对接文档（无自定义 Sender）

不新增 Sender 时，推送格式固定为：HTTP POST + JSON Body 全量透传，Body 内追加 `appname`/`ts`/`sign` 三字段，合作方验签后按 `dataType` 分发处理即可。

## 推送格式

- 协议：`HTTP POST`，`Content-Type: application/json`
- 无额外 Header，鉴权字段全在 Body

Body = 消息业务字段 + 三个追加字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| appname | string | 合作方标识（由 data-push 侧配置） |
| ts | **string** | 推送时刻毫秒时间戳（注意是 string，不是 number） |
| sign | string | MD5 签名，32位小写十六进制 |

## 签名算法

```
1. 取 Body 中所有字段，过滤掉：
   - value 为 null 的字段
   - value 为空字符串的字段
   - key 为 appname / secret / ts / sign 的字段
2. 每个字段拼为 key:value
   - primitive 或 string：String.valueOf(value)
   - object / array：JSON 序列化，key 字母排序
3. 将结果按字母升序排序
4. 末尾追加（不参与排序）：
   appname:<appname>
   secret:<secret>     ← 不出现在 Body，但参与签名
   ts:<ts>
5. 用 | 拼接，MD5(UTF-8) → 32位小写十六进制
```

验签参考（Java）：
```java
List<String> kvs = new ArrayList<>();
for (var entry : params.entrySet()) {
    if (entry.getValue() == null) continue;
    String key = entry.getKey().trim();
    String value = /* primitive: String.valueOf; object: JSON.toJSONString(MapSortField) */;
    if (Set.of("appname","secret","ts","sign").contains(key.toLowerCase())
            || value.isBlank()) continue;
    kvs.add(key + ":" + value.trim());
}
Collections.sort(kvs);
kvs.add("appname:" + appname);
kvs.add("secret:" + secret);
kvs.add("ts:" + ts);
String sign = DigestUtils.md5DigestAsHex(String.join("|", kvs).getBytes(UTF_8));
```

## 公共字段（所有消息类型）

| 字段 | 类型 | 说明 |
|---|---|---|
| dataType | int | 消息类型，见枚举表 |
| msgId | string | UUID，可用于幂等去重 |
| productId | string | 机器人设备ID |
| type | string | 机器人类型（SC/UP/NVWA等） |
| placeId | string | 场地ID |
| hotelId | string | 项目ID，仅 BASE_INFO 消息时更新，其余复用缓存值，可能为空 |
| chassisId / placeName / address / monitorUrl / appVersion | string | 可能为 null |

## dataType 枚举

| 值 | 名称 | 说明 |
|---|---|---|
| 1 | BASE_INFO | 机器人基础信息 |
| 2 | HEARTBEAT | 心跳/位置 |
| 4 | TASKSTATUS | 任务状态事件流 |
| 8 | EXCEPTION | 异常 |
| 16 | TASKDETAIL | 任务完结详情 |
| 32 | MAP_STATE | 地图状态 |
| 64 | COMMENT | 评价 |
| 128 | HEARTBEAT_CHASSIS | 底盘心跳 |
| 256 | POWER_STATUS | 电源状态 |
| 512 | TASKDETAIL_CHASSIS | 底盘任务详情 |

`push_type` 为位掩码叠加，如心跳+任务状态 = 2+4 = 6。

## HEARTBEAT 字段（dataType=2）

| 字段 | 类型 | 说明 |
|---|---|---|
| floor / currentFloor | int | 当前楼层（同一字段两个 getter，值相同） |
| ts | long | 机器人上报时间戳（毫秒），与 Body 外层 ts 不同 |
| powerPercent / power | float | 电量百分比（同一字段两个 getter） |
| chargeState | int | 1=充电中 |
| charging | boolean | chargeState==1 的派生字段 |
| estop | boolean | 急停；原始值为 null 时降级取 estopState |
| estopState | boolean | 急停（新格式） |
| idle | boolean | 空闲；原始值为 null 时按 runningStatus=="idle" 推导 |
| runningStatus | string | "idle" / "running" 等 |
| velocity | float | 速度，可能为 null |
| position | object | 地图坐标 `{x, y, z}`，**非经纬度** |
| orientation | object | 姿态四元数 `{w, x, y, z}` |
| deviceInfoChangeFlag | boolean | 设备信息是否变化，默认 false |
| lockers | array | `[{lockerId, status}]`，UP型机器人舱门状态 |

> position topic 来的心跳只含 position/orientation/floor，其余状态字段为 null。

## TASKSTATUS 字段（dataType=4）

`taskId` / `flowId` / `parentTaskId` / `taskType` / `eventType` / `target`（空时取 currentTarget）/ `currentTarget` / `boxId` / `occurTime`(ms) / `notifyTime`(ms) / `extraData` / `outTaskId`

## TASKDETAIL 字段（dataType=16）

`taskId` / `taskType` / `result` / `startTime` / `endTime` / `distance` / `stages`(JSONArray) / `retryTimes` / `extraData`

## EXCEPTION 字段（dataType=8）

`exceptionCode` / `exceptionMsg` / `taskId` / `target` / `floor` / `occurTime`(ms) / `notifyTime`(ms) / `extraData`

## 合作方接口要求

- 返回 HTTP 200 即视为成功，**不解析 response body**
- 超时（3000ms）触发重试，最多 **5 次**；接口必须幂等，建议用 `msgId` 去重
- 无需返回特定格式

## 易错点

- `ts` 是 **string** 类型，验签时不能当 number 处理
- `secret` 不在 Body 中，但参与签名，需运维提供给合作方
- `hotelId` 为空不代表无绑定，只是还未收到 BASE_INFO 消息
- 心跳消息中有两个 `ts`：外层是推送时间，内层是机器人上报时间，含义不同

## 相关链接

- [[arch-data-push]]

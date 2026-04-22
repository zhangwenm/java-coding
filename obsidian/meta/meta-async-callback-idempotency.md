---
tags: [元知识, 异步, 回调, 幂等]
date: 2026-04-22
scope: cross-domain
status: done
---

# 异步回调必须假设重复投递

**结论**：任何异步回调/消息消费逻辑，必须设计为幂等——同一消息被处理两次，结果与处理一次相同。

## 适用场景

- Kafka 消息消费
- 机器人任务状态回调（云迹、自研机器人）
- 云仓下货结果回调
- 第三方支付回调
- 定时任务（分布式锁失效时可能重复执行）

## 为什么一定会重复

| 场景 | 重复原因 |
|------|---------|
| Kafka at-least-once | 消费者 offset 提交前崩溃，重启后重消费 |
| 机器人回调 | 网络抖动导致回调方重试，接收方收到多次 |
| 定时任务 | 分布式锁过期或未加锁，多节点同时执行 |
| 业务重试 | 上游超时后重发，下游已处理但 ACK 未到达 |

## 幂等实现模式

**模式 A：唯一键去重（推荐）**
```sql
INSERT INTO task_result (request_id, status, ...) 
VALUES (?, ?) 
ON DUPLICATE KEY UPDATE status = VALUES(status)
```

**模式 B：状态机前置校验**
```java
if (!task.canTransitionTo(newStatus)) {
    log.warn("状态流转忽略：{} -> {}", task.getStatus(), newStatus);
    return;
}
```

**模式 C：Redis 去重（短窗口）**
```java
if (!redisTemplate.opsForValue().setIfAbsent("processed:" + requestId, "1", 5, TimeUnit.MINUTES)) {
    return; // 已处理
}
```

## 踩坑提醒

- 云迹机器人的 `ITEMS_SERVED_ON` 事件**会触发两次**（到达时 + 用户取货后），需用 `target` 字段区分，不能简单去重
- 幂等不等于忽略重复：需要区分"真重复"和"同 requestId 不同状态"的合法更新

## 相关链接

- [[projects/arch-data-push]] — Kafka 消费幂等设计

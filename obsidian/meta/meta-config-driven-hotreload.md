---
tags: [元知识, 配置, 热加载, 并发]
date: 2026-04-22
scope: cross-domain
status: done
---

# 配置驱动的动态组件热加载有并发写风险

**结论**：用定时任务从 DB 拉配置并动态创建/销毁组件时，必须加分布式锁或 CAS，否则多节点并发重建同一组件导致状态撕裂。

## 适用场景

- 定时从 DB 加载推送配置，动态启停 Sender（data-push 模式）
- 动态线程池参数调整
- 规则引擎动态加载规则
- 功能开关（Feature Flag）热更新

## 风险点

```
节点A: 读配置 → 发现变更 → 停旧Sender → 建新Sender（未完成）
节点B: 读配置 → 发现变更 → 停旧Sender → 建新Sender ← 此时A和B都在操作同一个组件
```

结果：两个节点各建了一个新 Sender，消息被发送两次；或一个节点把另一个刚建好的 Sender 又停掉。

## 防护方案

**方案 A：分布式锁（推荐）**
```java
RLock lock = redisson.getLock("config-reload-lock");
if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
    try {
        reloadConfig();
    } finally {
        lock.unlock();
    }
}
```

**方案 B：版本号 CAS**
```java
// 只有当前版本 == 期望版本时才执行重建
if (currentVersion.compareAndSet(expectedVersion, newVersion)) {
    rebuild();
}
```

**方案 C：单节点执行（XXL-Job 路由策略）**
将配置刷新任务路由到固定节点（FIRST / CONSISTENT_HASH），避免多节点竞争。

## 决策记录

| 方案 | 适用条件 | 缺点 |
|------|---------|------|
| 分布式锁 | 多节点部署，刷新频率低 | 锁过期时间难以估算 |
| 版本号 CAS | 组件有明确版本概念 | 需要维护版本状态 |
| 单节点执行 | 有 XXL-Job 等调度框架 | 单点故障风险 |

## 相关链接

- [[projects/arch-data-push]] — ConfigManager 定时刷新 PushConfig 的实现

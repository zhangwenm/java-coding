---
tags: [元知识, 多数据源, 数据库]
date: 2026-04-22
scope: cross-domain
status: done
---

# 多数据源切换在 AOP 边界上失效

**结论**：`@DataSourceKey` 注解依赖 AOP 拦截，在 AOP 不生效的场景（内部方法调用、非 Spring 管理的对象）下静默走默认数据源，不报错，数据写错库。

## 失效场景

| 场景 | 原因 | 现象 |
|------|------|------|
| 同类内部方法调用 `this.xxx()` | AOP 代理不拦截 self-invocation | 走默认数据源，不报错 |
| `@Async` 线程池中调用 | 新线程无 AOP 上下文 | 走默认数据源 |
| 构造函数/初始化中调用 | Bean 未完全初始化 | NPE 或走默认数据源 |
| 事务方法内切换数据源 | 事务已绑定连接，切换无效 | 数据源不切换 |

## 正确使用方式

```java
// ✅ 在 Service 方法上加注解（由外部调用才有效）
@DataSourceKey("DW_XZ")
public List<Xxx> queryFromDwXz() { ... }

// ❌ 内部调用，AOP 不生效
public void process() {
    this.queryFromDwXz(); // 走默认数据源
}
```

## 验证方法

怀疑数据源切换失效时：
```java
log.info("当前数据源: {}", DataSourceContextHolder.getDataSourceKey());
```

## 跨库事务禁止使用

多数据源不支持跨库事务。需要跨库操作时，用补偿事务（TCC）或最终一致性方案，不能用 `@Transactional`。

## 相关链接

- Resolver 规则：消息含 `DataSourceKey` 时自动注入此提醒

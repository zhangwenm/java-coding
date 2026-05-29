---
tags: [安全, 验签, openapi, 接口设计]
date: 2026-04-23
project: cross-domain
status: done
scope: cross-domain
generalized: true
retrieval_triggers: [验签, sign参数, 接口安全, 签名校验, 鉴权缺失]
---

# 接口接收了 sign 参数但从未校验——比无鉴权更危险的假安全

接口方法签名里有 `sign` 参数、注释里提到"待完善验签"，很容易被误认为"这个接口是安全的"。实际上它与完全裸露的接口等价，且更难被安全审计发现。

## 典型代码特征

```java
// 看起来有安全意识，实际上 sign 从未被使用
@PostMapping("/api/v2/xxx")
public Response doSomething(
    @RequestParam String appname,
    @RequestParam long ts,
    @RequestParam String sign,   // ← 接收了，但下面的逻辑完全没用到
    @RequestBody Payload payload) {
    // 直接处理业务，无任何 sign 校验
}
```

## 为什么比裸接口更危险

1. **审计盲区**：代码审查时看到 sign 参数，容易默认"这个接口已做鉴权"而跳过
2. **虚假文档**：调用方看到 sign 字段会认为需要签名，实际不传也能通
3. **历史惯性**：接口一旦上线，后补验签逻辑会破坏已有调用方，阻力大

## 防范措施

- 新接口上线前，在 CI 或代码审查中显式检查：接收了 sign 参数的接口是否调用了验签方法
- 验签逻辑抽成 Filter 或 AOP，避免每个 Controller 方法各自实现（也避免各自忘记）
- 如果暂时不做验签，直接删掉 sign 参数，不要留"占位符"

## 来源

- [[pitfalls-open-yuncang-api-sign]] — lot/open-yuncang-api 三个接口接收 sign 但未校验

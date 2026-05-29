---
tags: [lot, yuncang, 安全, 验签, openapi]
date: 2026-04-23
project: lot
status: done
scope: lot
generalized: true
retrieval_triggers: [open-yuncang-api, 验签, 接口安全, 签名校验, jinjiang]
---

# open-yuncang-api 验签覆盖不完整——部分接口接收签名参数但从不校验

`open-yuncang-api` 只有 jinjiang 的一个接口做了真正的 RSA 验签，其余三个接口虽然接收 `appname/ts/sign` 参数，但代码中完全没有校验，等同于无保护。

## 接口验签现状

| 接口 | 验签 | 说明 |
|---|---|---|
| `POST /openapi/jinjiang/v1/vm/send-goods` | ✅ | RSA + 时间戳双重校验 |
| `POST /api/v3/yuncang/isAvailable` | ❌ | 接收 sign 参数，未使用 |
| `POST /api/v2/yuncang/order/unifiedDispatching` | ❌ | 无任何鉴权 |
| `POST /api/v2/manager/yuncang/task/putActionInfo` | ❌ | 接收 sign 参数，未使用 |

## jinjiang 验签逻辑（`validateRequest()`）

三道校验，顺序执行：

1. **时间戳防重放**：`|System.currentTimeMillis() - ts| > 5min` → 直接拒绝
2. **签名非空**：`sign` 为空 → 拒绝
3. **RSA 验签**（`JinjangSignUtils.verifyJsonSign`）：
   - 取 JSON 全部字段，排除 `sign` 及空值
   - 每个字段拼为 `key:value`，**按字母排序**后用 `|` 连接
   - 用 RSA 公钥（配置项 `${jinjiang.rsa.public.key}`）做 `SHA256withRSA` 验证

关键文件：
- `OpenYuncangApi.java` — `validateRequest()` / `sendGoods()`
- `JinjangSignUtils.java` — `verifySign()` / `verifyJsonSign()`

## 风险点

`unifiedDispatching` 是下货核心接口，任何人知道地址即可调用，无鉴权。
`putActionInfo` 是货柜回调接口，同样裸露。

历史上 `isAvailable` 的 sign 参数看起来是最初规划了验签但未落地的痕迹。

## 相关链接

- [[arch-lot-robot-api-overview]]
- [[arch-lot-open-robot-call-api]]

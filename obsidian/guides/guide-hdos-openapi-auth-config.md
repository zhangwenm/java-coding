---
tags: [yuncang, hdos, 验签, openapi, 接口安全, 配置]
date: 2026-04-24
project: yuncang
status: done
scope: yuncang
generalized: false
retrieval_triggers: [open-api-server, 验签配置, headerAuthList, systemUrlAuthList, 接口鉴权, sign]
---

# hdos open-api-server 新增验签接口只需改 application.yml

三套验签机制完全由配置驱动，不需要改代码——把 URL 加入对应的 list 即可生效。核心文件：`ApiAuthConfig.java` + `InterceptorConfig.java`。

## 三套机制一览

| 机制 | 配置项 | 拦截器 | 适用场景 |
|---|---|---|---|
| Header 验签 | `api-auth.headerAuthList` | `AuthHeaderInterceptor` | 对外开放 API |
| 系统间验签 | `api-auth.systemUrlAuthList` | `SystemAuthUrlInterceptor` | 内部系统互调 |
| 饿了么验签 | `api-auth.eleme-auth.urlAuthList` | `ElemeAuthInterceptor` | 饿了么专用 |

## 机制一：Header 验签（最常用）

新增对外接口，加入 `headerAuthList`：

```yaml
api-auth:
  headerAuthList:
    - /v3/your-new-api/**
```

调用方需在 Header 携带（两种方式二选一）：

**方式 A — Header 签名**（推荐）：

| Header | 说明 |
|---|---|
| `token` | 开发者 token |
| `accessKeyId` | 开发者 accessKeyId |
| `signatureNonce` | 随机字符串（防重放） |
| `timestamp` | 时间戳 |

**方式 B — URL 参数**（兼容老接口）：`?accessToken=xxx`

两种方式在拦截器中是 `||` 关系，有一个通过即可。

## 机制二：系统间验签

内部系统互调，加入 `systemUrlAuthList`：

```yaml
api-auth:
  systemUrlAuthList:
    - /v3/your-internal-api
```

**签名算法**（MD5）：
1. 收集所有业务参数，排除 `appname`/`secret`/`ts`/`sign` 和空值
2. 每个参数拼为 `key:value`，**按字母排序**
3. 末尾追加 `appname:xxx|secret:xxx|ts:xxx`
4. MD5 整体字符串 → 即为 `sign`

请求参数：`appname`、`ts`（毫秒时间戳）、`sign`，支持 GET query 或 POST JSON body。时间窗口：**10 分钟**。

## 互斥规则（关键）

`systemUrlAuthList` 中的 URL 会被自动从 `headerAuthList` 的覆盖范围中排除（`excludePathPatterns`）。同一个接口不能同时在两个 list 里——否则只有 systemAuth 生效。

## 白名单（跳过所有验签）

需要接口完全不验签（如回调、内部工具），加入白名单：

```yaml
api-auth:
  urlAuthWhitelist:
    - /v3/your-callback/**
```

## 本地/测试环境

`application.yml` 中 `server.testing: true` 时，所有验签全部跳过（`InterceptorConfig` 直接 return）。生产环境必须确认此值为 `false`。

## 注意

- **新接口默认不校验**：URL 不在任何 list 里时，不会被拦截。必须主动加入 `headerAuthList` 才能生效。
- URL 支持 Ant 风格通配符（`/**`、`/*`）。

## 相关文件

- `InterceptorConfig.java` — 拦截器注册，决定哪个拦截器覆盖哪些 URL
- `ApiAuthConfig.java` — `@ConfigurationProperties(prefix = "api-auth")` 映射配置
- `AuthHeaderInterceptor.java` — Header 验签逻辑
- `SystemAuthUrlInterceptor.java` — 系统间验签 + MD5 算法实现
- `application.yml` — 所有 list 的实际配置位置

## 相关链接

- [[pitfalls-open-yuncang-api-sign]]

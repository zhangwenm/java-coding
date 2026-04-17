---
name: hdos-get-points
description: 当用户查询点位、获取点位列表、或表达送水/喝水需求时触发（如"查点位"、"获取点位"、"点位信息"、"我喝了"、"我想喝水"、"帮我送水"），调用门店点位接口获取可用点位列表，供后续派单使用。
trigger: 用户说出点位查询意图或饮水相关意图，如"查点位"、"获取点位"、"点位信息"、"我喝了"、"我要喝水"、"帮我送水"
api_base_url: https://dev01-open-api.yunjiai.cn
api_method: GET
api_path: /v3/stores/{storeId}/points
items_path: data
desc_param:
fixed_params:
---

## 接口配置

- 固定使用测试环境：`https://dev01-open-api.yunjiai.cn`

## 任务目标

用户表达了喝水需求，你需要：
1. 从当前对话上下文中提取 `storeId` 和 `accessToken`
2. 调用点位接口获取门店所有点位
3. 将点位列表整理后呈现给用户，供其选择送达位置

## 第一步：提取参数

**storeId** 按以下优先级获取：
1. `/hdos-get-points <storeId>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的 `storeId`

如果无法获取 storeId，告知用户提供门店 ID，不要猜测或伪造。

## 第二步：调用点位接口

```bash
curl -s "https://dev01-open-api.yunjiai.cn/v3/stores/{storeId}/points"
```

## 第三步：处理响应

**成功（code=0）时**，过滤出 `type=PT` 的客房点位，按楼层分组展示：

```
已获取门店点位，请选择送达位置：

7楼：101、102、103
8楼：201、202、203
...

请问送到哪里？
```

若点位名称与 id 相同（无别名），直接展示 id。

**失败时**：
- `code=10001`（token 失效）：告知用户重新登录获取新 token
- `code=10002`（参数缺失）：展示具体缺少的参数名
- 其他错误：展示 `message` 字段原始内容

## 约束

- 不要在没有真实参数的情况下发起请求
- 不要自动帮用户选择点位，等待用户回复后再继续
- 不要自动推进到下单步骤

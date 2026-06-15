---
name: hdos-order-list
description: 分页查询某用户的订单列表。当用户说"查所有订单"、"历史订单"、"订单列表"时触发。
trigger: 用户查询订单列表、历史订单、全部订单
---

## 接口配置

- 统一接口地址：`http://192.168.1.105:3100/l4/v1/skills/execute`

## 任务目标

1. 从上下文获取 `userId`、`storeId`
2. 调用统一接口查询订单列表
3. 分页展示订单摘要

## 第一步：提取参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| userId | 用户ID（路径参数） | 必填 |
| storeId | 门店ID | 必填 |
| pageNumber | 页码 | 1 |
| pageSize | 每页条数 | 10 |

**userId / storeId** 按以下优先级获取：
1. `/hdos-order-list <userId>` 命令行参数 `$ARGUMENTS`
2. 对话上下文

无法获取时告知用户提供。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://192.168.1.105:3100/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "hdos-order-list",
    "skillParameters": {
      "userId": "{userId}",
      "storeId": "{storeId}",
      "pageNumber": 1,
      "pageSize": 10
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**，展示订单列表：

```
订单列表（共 5 条，第 1/1 页）：

1. 订单号：2026031712345678
   状态：已支付 | 待发货
   金额：¥199.98
   时间：2026-03-17 12:34:56

2. 订单号：2026031600001234
   状态：已完成
   金额：¥59.90
   时间：2026-03-16 09:20:11
...
```

主要响应字段：
- `data.records[]`：订单列表
- `data.total`：总条数
- `data.current`：当前页
- `data.pages`：总页数
- `records[].sn`：订单号
- `records[].orderStatus`：订单状态
- `records[].payStatus`：支付状态
- `records[].flowPrice`：订单金额
- `records[].createTime`：下单时间

**有下一页时**询问用户是否继续翻页，不要自动加载全部。

**失败时**：展示 `message` 字段内容。

## 约束

- 不要在没有 userId 和 storeId 的情况下发起请求
- 默认只加载第一页，翻页由用户主动触发

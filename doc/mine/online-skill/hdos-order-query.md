---
name: hdos-order-query
description: 根据订单号查询单个订单详情。当用户说"查订单"、"订单状态"、"我的订单"并提供订单号时触发。
trigger: 用户查询特定订单状态或详情，提供了 orderSn
---

## 接口配置

- 统一接口地址：`http://192.168.1.105:3100/l4/v1/skills/execute`

## 任务目标

1. 从上下文获取 `orderSn`
2. 调用统一接口查询订单详情
3. 展示订单状态和商品信息

## 第一步：提取参数

**orderSn** 按以下优先级获取：
1. `/hdos-order-query <orderSn>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的订单号

无法获取时告知用户提供订单号。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://192.168.1.105:3100/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "hdos-order-query",
    "skillParameters": {
      "orderSn": "{orderSn}"
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**，展示订单详情：

```
订单详情：
订单号：2026031712345678
门店：测试店铺
订单状态：待发货（UNDELIVERED）
支付状态：已支付（PAID）
支付方式：微信支付
下单时间：2026-03-17 12:34:56

商品：
  - 测试商品（红色-M）× 2  ¥99.99

收货信息：张三 13800138000 北京市朝阳区建国路88号
```

主要响应字段：
- `data.sn`：订单号
- `data.orderStatus`：订单状态（UNPAID/UNDELIVERED/DELIVERED/COMPLETED/CANCELLED）
- `data.payStatus`：支付状态（UNPAID/PAID）
- `data.orderItems[]`：商品列表
- `data.address`：收货地址

**失败时**：展示 `message` 字段内容。

## 约束

- 不要在没有 orderSn 的情况下发起请求

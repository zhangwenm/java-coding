---
name: up-deck-locker-task
description: UP 机器人接力送-创建上舱 locker 任务接口。给上舱下发接力配送任务（取物/送物）时触发。
trigger: UP 机器人接力送，给上舱创建 locker 配送任务
---

## 接口配置

- 统一接口地址：`http://localhost:9090/api/skill/invoke`
- 对应原始接口：`POST /openapi/v5/deck/locker/task`

## 任务目标

1. 从上下文提取上舱 SN 和配送信息
2. 调用统一接口向上舱创建接力送任务
3. 展示任务创建结果

## 第一步：提取参数

按以下优先级获取：
1. `/up-deck-locker-task <productId> <lockerId> <target>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

**必填参数：**

| 参数 | 说明 |
|------|------|
| productId | 上舱 SN |
| lockers[].lockerId | 舱位 ID |
| lockers[].target（deliveryMarker） | 送物目标点位 |
| lockers[].goods[].name | 商品名称 |
| lockers[].goods[].quantity | 商品数量 |

**选填参数：**

| 参数 | 说明 |
|------|------|
| taskId | 任务 ID，为空时自动生成 |
| lockers[].orderId | 订单 ID，为空时取 lockerId |
| lockers[].stage（taskType） | 任务阶段：1=普通点位取物，2=货柜取物，3=仅送物，4=取物并配送 |
| lockers[].preTarget（fetchMarker） | 取物点位 |

缺少 productId、lockerId、target、goods 时告知用户补充。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://localhost:9090/api/skill/invoke" \
  -H "Content-Type: application/json" \
  -d '{
    "skillName": "up-deck-locker-task",
    "params": {
      "productId": "{productId}",
      "taskId": "{taskId}",
      "lockers": [
        {
          "lockerId": "{lockerId}",
          "orderId": "{orderId}",
          "stage": {stage},
          "preTarget": "{fetchMarker}",
          "target": "{deliveryMarker}",
          "goods": [
            {
              "name": "{goodsName}",
              "quantity": {quantity}
            }
          ]
        }
      ]
    }
  }'
```

## 第三步：处理响应

**成功时**（外层 errcode=0 且 result.code=0）：

```
上舱接力送任务创建成功！
上舱：{productId}
舱位：{lockerId}
送达点：{target}
```

**失败时**：
- 外层 `errcode≠0`：展示 `errmsg`
- `result.code≠0`：优先展示 `result.customerErrorMessage`，无则展示 `result.message`

## 约束

- 不要在缺少 productId、lockerId、target、goods 的情况下发起请求
- lockers 至少包含 1 个元素，goods 至少包含 1 个商品

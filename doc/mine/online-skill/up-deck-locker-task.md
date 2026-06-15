---
name: up-deck-locker-task
description: UP 机器人接力送-创建上舱 locker 任务接口。给上舱下发接力配送任务（取物/送物）时触发。
trigger: UP 机器人接力送，给上舱创建 locker 配送任务
---

## 接口配置

- 统一接口地址：`http://101.42.172.33:3430/l4/v1/skills/execute`

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
| lockers[].target | 送物目标点位 marker |
| lockers[].goods[].name | 商品名称 |
| lockers[].goods[].quantity | 商品数量 |

**顶层选填参数：**

| 参数 | 说明 |
|------|------|
| appname | 调用方应用名称 |
| taskId | 顶层任务 ID，为空时自动生成 |
| clientToken | 客户端唯一标识，用于幂等或状态查询 |
| attach | 附加信息（透传字符串） |

**lockers[] 选填参数：**

| 参数 | 说明 |
|------|------|
| taskId | 单舱位任务 ID（locker 级别，独立于顶层 taskId） |
| orderId | 订单 ID，为空时取 lockerId |
| taskType | 任务阶段：1=普通点位取物，2=货柜取物，3=仅送物，4=取物并配送 |
| preTarget | 取物点位 marker |
| preAction | 取物前动作 |
| createdAt | 任务创建时间戳（毫秒） |
| notification.value | 通知内容 |
| notification.type | 通知类型 |
| speech.put_item_in.type | 放物语音类型 |
| speech.put_item_in.resource | 放物语音资源 |

**goods[] 选填参数：**

| 参数 | 说明 |
|------|------|
| imgUrl | 商品图片 URL |
| id | 商品 ID |

缺少 productId、lockerId、target、goods 时告知用户补充。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://101.42.172.33:3430/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "up-deck-locker-task",
    "skillParameters": {
      "productId": "{productId}",
      "taskId": "{taskId}",
      "clientToken": "{clientToken}",
      "appname": "{appname}",
      "attach": "{attach}",
      "lockers": [
        {
          "lockerId": "{lockerId}",
          "taskId": "{lockerTaskId}",
          "orderId": "{orderId}",
          "taskType": {taskType},
          "preTarget": "{preTarget}",
          "preAction": "{preAction}",
          "target": "{target}",
          "createdAt": {createdAt},
          "goods": [
            {
              "name": "{goodsName}",
              "quantity": {quantity},
              "imgUrl": "{imgUrl}",
              "id": "{goodsId}"
            }
          ],
          "notification": {
            "value": "{notificationValue}",
            "type": "{notificationType}"
          },
          "speech": {
            "put_item_in": {
              "type": "{speechType}",
              "resource": "{speechResource}"
            }
          }
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

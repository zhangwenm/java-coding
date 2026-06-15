---
name: up-chassis-task-flow
description: UP 机器人底盘任务流接口。给 UP 机器人下发送物、清扫、召唤、对接货柜、巡游等任务时触发。
trigger: 给 UP 机器人创建底盘任务，如送物、清扫、召唤、对接货柜、上舱巡游
---

## 接口配置

- 统一接口地址：`http://localhost:9090/api/skill/invoke`
- 对应原始接口：`POST /openapi/v5/chassis/task/flow`

## 任务目标

1. 从上下文提取底盘 SN 和业务参数
2. 调用统一接口向 UP 机器人下发底盘任务
3. 展示任务下发结果

## 第一步：提取参数

按以下优先级获取：
1. `/up-chassis-task-flow <productId> <businessType> [target]` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| productId | 是 | 底盘机器人 SN |
| businessType | 是 | 业务类型，见下方枚举 |
| target | 视场景 | 目标点位 marker（送物/召唤场景必填） |
| cabinKey | 视场景 | 上舱 key（含上舱的场景必填） |
| taskType | 否 | 任务类型值，优先从 businessType 自动推断 |

**businessType 枚举（对应 UpBusinessServiceImpl 方法）：**

| businessType | 说明 | taskType |
|--------------|------|----------|
| `delivery_pickup` | 取物/放物配送 | 1 |
| `cleaning` | 清扫 | 42 |
| `sweep` | 清扫维护 | 42 |
| `docking` | 对接货柜 | 82 |
| `summon` | 召唤机器人 | 53 |
| `move_to_container` | 移动到货柜 | 2 |
| `summon_delivery` | 召唤上舱送物 | 1 |
| `no_screen_delivery` | 无屏幕上舱送物 | 1 |
| `cabin_cruise` | 上舱巡游（安防/消杀） | 53 |
| `move_to_cabin` | 移动到上舱位置 | 82 |

缺少必填参数时告知用户补充，不要猜测或填默认值。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://localhost:9090/api/skill/invoke" \
  -H "Content-Type: application/json" \
  -d '{
    "skillName": "up-chassis-task-flow",
    "params": {
      "productId": "{productId}",
      "businessType": "{businessType}",
      "target": "{target}",
      "cabinKey": "{cabinKey}",
      "taskType": {taskType}
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**：

```
底盘任务下发成功！
机器人：{productId}
业务类型：{businessType}
任务 ID：{taskId}
```

**失败时**：
- `errcode≠0`：展示 `errmsg` 内容
- `result.code≠0`：展示 `result.message` 内容

## 约束

- 不要在没有 productId 和 businessType 的情况下发起请求
- target 和 cabinKey 缺失时先询问用户，不要自动补空字符串

---
name: lot-mechanical-arm-dispatch
description: 洗衣机械臂出单接口。当用户说"机械臂出单"、"触发机械臂"、"让机械臂取衣服"、"洗衣出单"时触发，向洗衣机器人下发机械臂出单指令。
trigger: 用户表达洗衣机械臂出单意图，如"机械臂出单"、"触发机械臂"、"洗衣出单"、"让机械臂取衣服"
---

## 接口配置

- 统一接口地址：`http://localhost:3430/l4/v1/skills/execute`

## 任务目标

调用统一接口，触发洗衣机械臂出单动作，展示执行结果。

## 第一步：提取参数

按以下优先级获取：
1. `/lot-mechanical-arm-dispatch <orderId> <storeId> <roomNo>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| orderId | 是 | 订单 ID |
| workorderId | 是 | 工单 ID（时间戳毫秒，自动生成） |
| storeId | 是 | 门店 ID |
| roomNo | 是 | 房间号 |

`workorderId` 由调用方生成（当前时间戳毫秒），其余参数无法获取时告知用户补充，不要猜测。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://localhost:3430/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "lot-mechanical-arm-dispatch",
    "skillParameters": {
      "orderId": "{orderId}",
      "workorderId": "{ts_ms}",
      "storeId": "{storeId}",
      "roomNo": "{roomNo}",
      "taskType": "laundry_wash_dry",
      "items": []
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**：

```
✅ 洗衣机械臂出单成功

响应数据：{data}
```

**失败时**：

| 失败原因 | 特征 | 处理 |
|---------|------|------|
| 设备繁忙 | message 含"设备繁忙"、"taskId" | 展示当前执行中的 taskId，提示稍后重试 |
| 机器人未找到 | message 含"robot not found" | 提示洗衣机器人离线或未注册 |
| 机械臂无响应 | message 含"响应为空" | 提示机械臂服务未响应，检查连接 |
| 其他 | `errcode≠0` | 展示 `errmsg` 原始内容 |

## 约束

- 仅适用于 Demo 环境，不可在生产调用
- 此 skill 固定走洗衣路由，便利购出单不在此处理

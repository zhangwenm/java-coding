---
name: up-deck-cancel-locker-task
description: UP 机器人取消上舱接力送 locker 任务接口。撤销已下发的接力配送任务时触发。
trigger: 取消 UP 上舱接力送任务、撤销 locker 配送任务
---

## 接口配置

- 统一接口地址：`http://localhost:9090/api/skill/invoke`
- 对应原始接口：`POST /openapi/v5/deck/cancel/locker/task`

## 任务目标

1. 从上下文提取上舱 SN
2. 调用统一接口取消接力送 locker 任务
3. 展示取消结果

## 第一步：提取参数

按以下优先级获取：
1. `/up-deck-cancel-locker-task <productId>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的 `productId`（上舱 SN）

缺少 productId 时告知用户提供上舱 SN，不要猜测。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://localhost:9090/api/skill/invoke" \
  -H "Content-Type: application/json" \
  -d '{
    "skillName": "up-deck-cancel-locker-task",
    "params": {
      "productId": "{productId}"
    }
  }'
```

## 第三步：处理响应

**成功时**（errCode=0 且 result.success=true）：

```
接力送任务取消成功！
上舱：{productId}
```

**失败时**：
- `errCode≠0`：展示 `errMsg` 内容
- `result.success=false`：展示 `result.errorMessage` 内容

## 约束

- 此接口取消的是**接力送 locker 任务**（由 `up-deck-locker-task` 创建）
- 取消上舱**调度任务流**请用 `up-deck-executor-command`（cmd=cancel_task）
- 取消**底盘任务**请用 `up-chassis-update-task-flow`（action=cancel）

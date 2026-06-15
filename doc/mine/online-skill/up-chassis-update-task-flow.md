---
name: up-chassis-update-task-flow
description: UP 机器人更新底盘任务流接口。取消底盘任务、推进任务节点、暂停任务时触发。
trigger: 取消 UP 底盘任务、跳过任务节点、暂停任务
---

## 接口配置

- 统一接口地址：`http://192.168.1.105:3100/l4/v1/skills/execute`

## 任务目标

1. 从上下文提取底盘 SN 和任务 ID
2. 调用统一接口更新或取消底盘任务流
3. 展示操作结果

## 第一步：提取参数

按以下优先级获取：
1. `/up-chassis-update-task-flow <productId> <taskId> <action>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| productId | 是 | 底盘机器人 SN |
| taskId | 是 | 当前任务 ID |
| action | 是 | 操作类型：`cancel` / `update_task_flow` / `task_paused` |
| optionId | 是 | 要操作的节点 ID |
| cancelInfo | 否 | 取消原因（action=cancel 时建议填写） |
| versionNumber | 否 | 版本号，默认 1 |

缺少 productId、taskId、action 时告知用户补充。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://192.168.1.105:3100/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "up-chassis-update-task-flow",
    "skillParameters": {
      "productId": "{productId}",
      "taskId": "{taskId}",
      "action": "{action}",
      "optionId": "{optionId}",
      "cancelInfo": "{cancelInfo}",
      "versionNumber": 1
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**：

```
操作成功！
机器人：{productId}
任务 ID：{taskId}
操作：{action}
```

**失败时**：展示 `errmsg` 或 `result.message` 内容。

## 约束

- 取消底盘任务走此接口（action=cancel），不要用 chassis/executor/command
- action=cancel 时 cancelInfo 建议填写，便于日志追踪

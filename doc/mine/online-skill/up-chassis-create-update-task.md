---
name: up-chassis-create-update-task
description: UP 机器人底盘创建/更新任务接口。上舱直接发起任务（不含 docking_cabin 节点）或修改已有任务时触发。
trigger: UP 机器人创建不含 docking_cabin 的新任务，或更新/替换已有底盘任务
---

## 接口配置

- 统一接口地址：`http://101.42.172.33:3400/l4/v1/skills/execute`

## 任务目标

1. 从上下文提取底盘 SN、任务参数
2. 调用统一接口创建或更新底盘任务
3. 展示任务创建结果

## 第一步：提取参数

按以下优先级获取：
1. `/up-chassis-create-update-task <productId> <taskType>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| productId | 是 | 底盘机器人 SN |
| executors | 是 | 执行器节点 JSON 字符串（序列化后） |
| taskType | 是 | 任务类型值（Integer，见 DeliveryTypeEnum） |
| taskId | 否 | 当前任务 ID；新建时留空自动生成，更新时填旧任务 ID |
| newTaskId | 否 | 更新任务时的新任务 ID |
| missionKey | 否 | 当前任务 missionKey；更新任务时需传旧值 |
| newMissionKey | 否 | 更新任务时的新 missionKey |
| appname | 否 | 调用方应用名称（部分场景需鉴权） |
| clientToken | 否 | 客户端唯一标识，用于幂等或状态查询 |
| reason | 否 | 任务原因描述 |
| timeoutCallback | 否 | 超时回调配置，JSON 对象：`{"timeout": 秒数, "action": "动作"}` |
| versionNumber | 否 | 版本号，默认 1 |
| timestamp | 自动 | 接口层自动设置 UTC 时间，无需传入 |

缺少 productId、executors、taskType 时告知用户补充。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://101.42.172.33:3400/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "up-chassis-create-update-task",
    "skillParameters": {
      "productId": "{productId}",
      "appname": "{appname}",
      "taskId": "{taskId}",
      "newTaskId": "{newTaskId}",
      "missionKey": "{missionKey}",
      "newMissionKey": "{newMissionKey}",
      "executors": "{executors}",
      "taskType": {taskType},
      "versionNumber": "1",
      "clientToken": "{clientToken}",
      "reason": "{reason}",
      "timeoutCallback": "{\"timeout\": 60, \"action\": \"{action}\"}"
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**：

```
底盘任务创建成功！
机器人：{productId}
任务 ID：{taskId}
```

**失败时**：展示 `errmsg` 或 `result.message` 内容。

## 约束

- 更新任务时 `taskId`（旧）和 `newTaskId`（新）都必须填，否则旧 missionKey 失效后无法再控制机器人
- 同理，若使用 missionKey 控制，更新时需同时传 `missionKey`（旧）和 `newMissionKey`（新）
- `executors` 必须是序列化后的 JSON 字符串，不能传对象
- `timeoutCallback` 也需序列化为 JSON 字符串传入，字段含义：`timeout` 为超时秒数，`action` 为超时触发动作

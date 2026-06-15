---
name: up-chassis-create-update-task
description: UP 机器人底盘创建/更新任务接口。上舱直接发起任务（不含 docking_cabin 节点）或修改已有任务时触发。
trigger: UP 机器人创建不含 docking_cabin 的新任务，或更新/替换已有底盘任务
---

## 接口配置

- 统一接口地址：`http://localhost:9090/api/skill/invoke`
- 对应原始接口：`POST /openapi/v5/chassis/create/update/task`

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
| executors | 是 | 执行器节点 JSON 字符串 |
| taskType | 是 | 任务类型值（见 DeliveryTypeEnum） |
| taskId | 否 | 当前任务 ID；新建时留空自动生成，更新时填旧任务 ID |
| newTaskId | 否 | 更新任务时的新任务 ID |
| versionNumber | 否 | 版本号，默认 1 |

缺少 productId、executors、taskType 时告知用户补充。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://localhost:9090/api/skill/invoke" \
  -H "Content-Type: application/json" \
  -d '{
    "skillName": "up-chassis-create-update-task",
    "params": {
      "productId": "{productId}",
      "taskId": "{taskId}",
      "newTaskId": "{newTaskId}",
      "executors": "{executors}",
      "taskType": {taskType},
      "versionNumber": 1
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
- `executors` 必须是序列化后的 JSON 字符串，不能传对象

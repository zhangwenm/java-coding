---
name: up-deck-L4-executor-command
description: UP 机器人上舱 L4 执行器命令接口。L4 内部调用版本，无 appname 权限校验，控制上舱舱门状态、锁仓等待货柜、取消上舱调度任务时触发。
trigger: L4 内部控制上舱舱门开关、锁仓等待货柜、取消上舱任务调度
---

## 接口配置

- 统一接口地址：`http://localhost:3430/l4/v1/skills/execute`
- 对应后端端点：`POST /openapi/v5/L4/deck/executor/command`
  - 源码：`OpenScControlApi.java:38` — `deckL4ExecutorCommand`

## 与非 L4 版本的区别

| 维度 | 非 L4（`up-deck-executor-command`） | L4（本 skill） |
|------|--------------------------------------|----------------|
| 端点 | `/openapi/v5/deck/executor/command` | `/openapi/v5/L4/deck/executor/command` |
| 权限校验 | 需要 appname，校验机器人归属 | **无权限校验**，L4 内部直调 |
| appname 字段 | 必填 | 不需要 |

## 任务目标

1. 从上下文提取上舱 SN 和命令类型
2. 调用统一接口向上舱发送执行器命令
3. 展示命令执行结果

## 第一步：提取参数

按以下优先级获取：
1. `/up-deck-L4-executor-command <productId> <cmd>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| productId | 是 | 上舱 SN |
| cmd | 是 | 命令类型，见下方枚举 |
| clientToken | 否 | 幂等 token |
| data | 视 cmd | 命令参数 JSON 字符串，无额外参数传 `{}` |

**cmd 枚举：**

| cmd | 说明 | data 说明 |
|-----|------|-----------|
| `executor_customer_needs` | 自定义客需舱操作 | 操作参数 JSON |
| `lock_wait_container` | 锁仓，等待货柜就位 | 锁仓参数 JSON |
| `cancel_task` | 取消上舱当前调度任务 | `{}` |
| `operator_locker` | 控制指定舱门服务状态 | `{"lockerId":"{doorId}","serviceStatus":{status}}` |

**operator_locker 的 serviceStatus 值：** `10` = 可用

缺少 productId 或 cmd 时告知用户补充。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://localhost:3100/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "up-deck-L4-executor-command",
    "skillParameters": {
      "productId": "{productId}",
      "cmd": "{cmd}",
      "data": "{data}"
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**：

```
上舱命令发送成功！
上舱：{productId}
命令：{cmd}
```

**失败时**：展示 `errmsg` 或 `result.message` 内容。

## 约束

- 取消上舱**调度任务流**用此接口（cmd=cancel_task）；取消**接力送 locker 任务**用 `up-deck-cancel-locker-task`，两者不同
- data 无额外参数时传 `{}`，不能为空
- 此接口跳过 appname 权限校验，仅用于 L4 内部服务调用；外部调用请用 `up-deck-executor-command`

---
name: up-deck-executor-command
description: UP 机器人上舱执行器命令接口。控制上舱舱门状态、锁仓等待货柜、取消上舱调度任务时触发。
trigger: 控制上舱舱门开关、锁仓等待货柜、取消上舱任务调度
---

## 接口配置

- 统一接口地址：`http://192.168.1.105:3100/l4/v1/skills/execute`

## 任务目标

1. 从上下文提取上舱 SN 和命令类型
2. 调用统一接口向上舱发送执行器命令
3. 展示命令执行结果

## 第一步：提取参数

按以下优先级获取：
1. `/up-deck-executor-command <productId> <cmd>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| productId | 是 | 上舱 SN |
| cmd | 是 | 命令类型，见下方枚举 |
| data | 视 cmd | 命令参数 JSON 字符串 |

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
curl -s -X POST "http://192.168.1.105:3100/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "up-deck-executor-command",
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

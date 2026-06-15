---
name: up-chassis-executor-command
description: UP 机器人底盘执行器命令接口。用户操作完成后通知底盘提前结束等待节点时触发（如用户已取物/放物，通知机器人继续行进）。
trigger: 用户操作完成，通知 UP 底盘结束等待、继续执行下一步
---

## 接口配置

- 统一接口地址：`http://localhost:9090/api/skill/invoke`
- 对应原始接口：`POST /openapi/v5/chassis/executor/command`

## 任务目标

1. 从上下文提取底盘 SN 和任务标识
2. 调用统一接口发送执行器命令，提前结束当前等待节点
3. 展示命令发送结果

## 第一步：提取参数

按以下优先级获取：
1. `/up-chassis-executor-command <productId> <cmd>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| productId | 是 | 底盘机器人 SN |
| cmd | 是 | 命令名称，目前支持：`finish_user_operating` |
| data | 否 | 命令参数 JSON 字符串，无额外参数时传 `{}` |

**cmd 说明：**

| cmd | 说明 | 适用节点 |
|-----|------|---------|
| `finish_user_operating` | 提前结束当前节点，进入下一步 | `wait`、`move_sweep_control`、`transport_without_screen_cabin` |

缺少 productId 或 cmd 时告知用户补充。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://localhost:9090/api/skill/invoke" \
  -H "Content-Type: application/json" \
  -d '{
    "skillName": "up-chassis-executor-command",
    "params": {
      "productId": "{productId}",
      "cmd": "{cmd}",
      "data": "{data}"
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**：

```
命令发送成功！
机器人：{productId}
命令：{cmd}
```

**失败时**：展示 `errmsg` 或 `result.message` 内容。

## 约束

- data 缺省时传 `{}`，不能传 null 或空字符串
- 此命令只能结束**当前正在执行**的节点，节点已完成时调用无效

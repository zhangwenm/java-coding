---
name: up-chassis-lift-control
description: UP 机器人底盘举升/下降上仓接口。需要单独控制底盘举起或放下上仓时触发，如对接/分离上仓、手动调整舱位。
trigger: 用户说"举升上仓"、"放下上仓"、"底盘举起"、"底盘降下"，或需要单独控制 UP 底盘举升/下降动作时
---

## 接口配置

- 统一接口地址：`http://192.168.1.105:3100/l4/v1/skills/execute`

## 任务目标

1. 从上下文提取底盘 SN 和动作方向
2. 调用统一接口向底盘下发 `simple_lift_control` 举升/下降指令
3. 展示执行结果

## 第一步：提取参数

按以下优先级获取：
1. `/up-chassis-lift-control <productId> <action>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| productId | 是 | 底盘机器人 SN（前缀 WT / WATER / OCEAN） |
| action | 是 | 动作方向：`up`（举升）或 `down`（下降） |

**action 语义推断：**

| 用户表达 | action |
|---------|--------|
| "举升"、"举起"、"抬起"、"升起"、"up" | `up` |
| "下降"、"放下"、"降下"、"落下"、"down" | `down` |

两个参数均为必填。`productId` 无法从上下文获取时告知用户补充，不要猜测 SN。

## 第二步：调用统一接口

内部会构建如下 executor 并通过 `createFlow` 发给底盘：
```json
[{"optionId":"1001","executionId":"simple_lift_control","params":{"action":"up"}}]
```

调用示例：

```bash
curl -s -X POST "http://192.168.1.105:3100/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "up-chassis-lift-control",
    "skillParameters": {
      "productId": "{productId}",
      "action": "{action}"
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**：

```
举升/下降指令已下发！
机器人：{productId}
动作：{up → 举升上仓 / down → 下降上仓}
```

**失败时**：
- `errcode≠0`：展示 `errmsg` 内容
- `result.code≠0`：展示 `result.message` 内容

## 约束

- 仅适用于 UP 底盘系列（WT / WATER / OCEAN 前缀），其他品牌不支持此接口
- 此接口只控制举升/下降动作本身，不包含移动到对接点；若需完整对接流程，使用 `up-chassis-task-flow`（businessType=`docking`）
- `action` 只接受 `up` / `down`，不能传其他值

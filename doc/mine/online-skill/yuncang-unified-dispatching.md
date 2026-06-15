---
name: yuncang-unified-dispatching
description: 云仓/Ubox 统一下货接口。接收商城订单，根据货柜 SN 前缀自动路由：UBOX 前缀走 Ubox 下货流程，其他走云仓下货流程。
trigger: 下货通知、发送下货指令、云仓下货、Ubox 下货、统一下货
---

## 接口配置

- 统一接口地址：`http://101.42.172.33:3430/l4/v1/skills/execute`
- 实际接口路径：`POST /api/v2/yuncang/order/unifiedDispatching`（open-yuncang-api 服务）

## 任务目标

1. 从上下文提取下货所需参数
2. 调用统一接口发送下货指令
3. 展示下货结果及 requestId

## 第一步：提取参数

按以下优先级获取：
1. `/yuncang-unified-dispatching <containerSN> <taskId>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| containerSN | 是 | 货柜 SN；`UBOX` 前缀走 Ubox 流程，其他走云仓流程 |
| taskId | 是（云仓）| 销售任务 ID；Ubox 流程可留空 |
| orderId | 否 | 订单 ID |
| robotSN | 否 | 机器人 SN |
| taskType | 否 | 任务类型，默认 0 |
| callbackUrl | 否 | 回调地址，云仓流程完成后通知业务方 |
| orderItem | 是 | 订单项列表，至少一条 |
| orderItem[].row | 是 | 货道行号（整数） |
| orderItem[].column | 是 | 货道列号（整数） |
| orderItem[].count | 是 | 出货数量 |
| orderItem[].goodsName | 否 | 商品名称 |

**参数缺失规则：**
- `containerSN` 缺失：告知用户补充，不要猜测
- 云仓流程下 `taskId` 或 `orderItem` 为空：告知用户补充，不要提交
- Ubox 流程下 `taskId` 非必填，可不传

## 第二步：调用统一接口

```bash
curl -s -X POST "http://101.42.172.33:3430/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "yuncang-unified-dispatching",
    "skillParameters": {
      "containerSN": "{containerSN}",
      "taskId": "{taskId}",
      "orderId": "{orderId}",
      "robotSN": "{robotSN}",
      "taskType": {taskType},
      "callbackUrl": "{callbackUrl}",
      "orderItem": [
        {
          "row": {row},
          "column": {column},
          "count": {count},
          "goodsName": "{goodsName}"
        }
      ]
    }
  }'
```

## 第三步：处理响应

响应结构（`DispatchingResult`）：

| 字段 | 说明 |
|------|------|
| `errorCode` | 0=成功，404=参数缺失，500=处理异常 |
| `errorMsg` | 错误描述 |
| `requestId` | 成功时返回，云仓流程为 UUID，Ubox 流程为 appTranId |

**成功（errorCode=0）时**：

```
下货指令发送成功！
货柜：{containerSN}
requestId：{requestId}
```

**失败时**：

| errorCode | 含义 | 处理方式 |
|-----------|------|---------|
| 404 | 必填参数为空 | 展示 `errorMsg`，提示用户补充缺失字段 |
| 500 | 服务端异常 | 展示 `errorMsg`，提示用户检查货柜状态或重试 |

**errorMsg 常见值说明：**

| errorMsg | 说明 |
|----------|------|
| `Insufficient stock` | Ubox 库存不足，检查货柜实际库存 |
| `unknown mode` | Ubox containerMode 未知，排查货柜配置 |
| `handle delivery order exception!` | 通用下货异常，查服务端日志 |

## 路由逻辑说明

| containerSN 前缀 | 流程 | 说明 |
|-----------------|------|------|
| `UBOX` | Ubox 流程 | 按 containerMode（1/2/3/4）决定下单方式；mode=1 需检查库存 |
| 其他 | 云仓流程 | 保存货物记录和任务后发送 MQ 消息；taskId、orderItem 必填 |

## 约束

- 同一 taskId 不要重复下发，云仓不做幂等保护
- Ubox mode=1 开启库存校验时，库存不足直接返回失败，不重试
- `orderItem` 的 row/column 必须是整数，货道号由服务端计算为 `rowrow` + `colcol` 格式（如行 1 列 2 → `0102`）

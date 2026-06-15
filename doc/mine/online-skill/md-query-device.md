---
name: md-query-device
description: 查询门店下的设备列表（Device/机器人资源），支持分页。需要 storeId 时触发。
trigger: 查询某个门店的设备列表、获取设备信息、列出机器人资源
---

## 接口配置

- 统一接口地址：`http://192.168.100.106:3430/l4/v1/skills/execute`
- skillId：`20260416160436877_MD_query_device_test`

## 任务目标

1. 从上下文提取 storeId 和分页参数
2. 调用统一接口查询设备列表
3. 展示设备名称、SN、可用状态、标签等关键信息

## 第一步：提取参数

按以下优先级获取：
1. `/md-query-device <storeId>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的相关字段

| 参数 | 必填 | 说明 |
|------|------|------|
| storeId | 是 | 门店 ID，格式 `space_xxx` |
| type | 否 | 资源类型，固定传 `Device`，默认即此值 |
| pageNo | 否 | 页码，默认 1 |
| pageSize | 否 | 每页数量，默认 100 |

缺少 storeId 时告知用户补充。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://192.168.100.106:3430/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "20260416160436877_MD_query_device_test",
    "skillParameters": {
      "storeId": "{storeId}",
      "type": "Device",
      "pageNo": 1,
      "pageSize": 100
    }
  }'
```

## 第三步：处理响应

**成功（code=0，status=SUCCESS）时**，从 `data.result.output` 中提取关键字段展示：

```
共 {total} 台设备（第 {pageNo} 页）

[1] 名称：{name}
    SN：{robotProfile.productId}
    IP：{robotProfile.robotIp}
    类型：{robotProfile.robotType}/{robotProfile.robotSubType}
    子类型名称：{resourceSubtype.name}
    状态：{availability}
    标签：{tags}
    设备编码：{attributesJson.deviceCode}
```

**关键字段说明：**

| 字段路径 | 说明 |
|---------|------|
| `name` | 设备显示名称 |
| `robotProfile.productId` | 机器人 SN（唯一标识） |
| `robotProfile.robotIp` | 机器人 IP |
| `robotProfile.robotType` | 机器人类型（如 WT） |
| `robotProfile.robotSubType` | 机器人子类型（如 HK） |
| `availability` | `online` / `offline` |
| `tags` | 业务标签数组（如 ["客需舱"]） |
| `attributesJson.deviceCode` | 设备编码 |
| `resourceSubtype.name` | 子类型名称（如 上舱） |
| `resourceSubtype.skillIds` | 该子类型支持的 skillId 列表 |
| `resourceId` | 资源全局唯一 ID（`res_dev_xxx`格式） |

**失败时**：展示 `message` 字段内容，或 `data.result` 中的错误信息。

## 约束

- type 固定传 `Device`，不要修改
- pageSize 建议不超过 100，数据量大时需分页循环获取
- `robotProfile` 可能为 null（纯设备无机器人档案时），展示前需判空
- `resourceSubtype` 可能为 null（未分配子类型的设备），展示时跳过该字段

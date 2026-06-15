---
name: hdos-list-goods
description: 查询 HDOS 门店商品列表。当用户说"查商品"、"查询商品"、"商品列表"、"有哪些商品"、"门店商品"时触发，调用接口返回商品列表供用户浏览或选择。
trigger: 用户说出商品查询意图，如"查商品"、"商品列表"、"门店有哪些商品"、"查询商品"
---

## 接口配置

- 环境：`https://dev01-open-api.yunjiai.cn`
- 路径：`GET /v3/stores/{storeId}/goods/list`

## 第一步：提取参数

**storeId** 按以下优先级获取：
1. `/hdos-list-goods <storeId>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的 `storeId`

如果无法获取 storeId，直接告知用户提供门店 ID，不要猜测。

## 第二步：确认过滤条件

调用前先问用户：**"需要按名称搜索或按类型过滤吗？直接说关键词即可，如"矿泉水"或"客需品""**

支持以下条件（均可选）：

| 参数 | 说明 |
|------|------|
| `types` | 商品类型，多个用英文逗号分隔，不填返回全部 |
| `name` | 商品名称关键词（模糊匹配） |
| `groupId` | 商品分组 ID |
| `current` | 页码（默认 1） |
| `pageSize` | 每页条数（默认 20，最大 9999） |

**types 枚举值：**

| 值 | 说明 |
|----|------|
| `ENTITY` | 实物商品 |
| `VIRTUAL` | 虚拟商品 |
| `HOTEL` | 酒店商品 |
| `MEALS` | 餐食 |
| `ROOMITEMS` | 客需品 |
| `SENT_UP` | 送上来 |
| `CARD_COUPON` | 卡券商品 |
| `LAUNDRY` | 洗衣商品 |
| `SAVENDAY_COFFEE` | 咖啡机商品 |
| `SAVENDAY_CONTAINER` | 货柜商品 |

如果用户没有明确提到过滤条件，直接使用默认值（不传 types/name/groupId）查全部。

## 第三步：发起请求

```bash
curl -s -G "https://dev01-open-api.yunjiai.cn/v3/stores/{storeId}/goods/list" \
  --data-urlencode "current=1" \
  --data-urlencode "pageSize=20" \
  [--data-urlencode "types=ROOMITEMS"] \
  [--data-urlencode "name=水"] \
  [--data-urlencode "groupId=xxx"]
```

将 `{storeId}` 替换为真实值，可选参数按实际情况加上。

## 第四步：处理响应

**成功（code=0）**，展示商品列表：

```
共 XX 件商品（第 1 页，共 X 页）：

1. 矿泉水（id: xxx，类型: ROOMITEMS，价格: 5.00）
2. 毛巾（id: xxx，类型: ROOMITEMS，价格: 0.00）
...

是否需要查看下一页，或按名称/类型筛选？
```

展示字段：序号、商品名称、商品 ID、类型、价格（如有）。
若返回为空，提示用户该门店暂无匹配商品。

**失败时**：直接展示 `message` 字段原始内容。

## 约束

- 不要自动帮用户选择商品或发起下单
- 分页信息（total/current/pageSize）必须展示，方便用户判断是否翻页

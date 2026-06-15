---
name: hdos-goods-menu
description: 查询门店商品菜单，获取分类及商品列表（最多三级分类）。当用户说"查商品"、"商品列表"、"有什么商品"时触发。
trigger: 用户查询商品、获取菜单、浏览商品分类
---

## 接口配置

- 统一接口地址：`http://101.42.172.33:3430/l4/v1/skills/execute`

## 任务目标

1. 从上下文提取 `storeId`
2. 调用统一接口获取商品菜单
3. 按分类整理展示商品列表

## 第一步：提取参数

**storeId** 按以下优先级获取：
1. `/hdos-goods-menu <storeId>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的 `storeId`

**goodsName**：从对话上下文中提取，用于后续精准匹配。

无法获取 storeId 时告知用户提供门店 ID，不要猜测。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://101.42.172.33:3430/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "20260415160436877_MD_hdos-goods-menu",
    "skillParameters": {
      "storeId": "{storeId}",
      "current": 1,
      "type": "Amenities",
      "pageSize": 100
    }
  }'
```

## 第三步：处理响应

**成功（code=200）时**，取每个商品的 `name` 和 `goodsId` 展示：

```
门店商品菜单：

【Amenities】
  - 矿泉水（goodsId: 2036067531807485954）
  - 可乐（goodsId: 2036066275965759490）
```

字段说明：
- `list[].name`：商品名称
- `list[].attributesJson.mallGoodsSnapshot.goodsId`：商品 ID

其余字段（价格、skuId、promotionPrice 等）忽略不展示。

**失败时**：展示 `message` 字段内容。

## 第四步：goodsName 精准匹配（上下文有 goodsName 时执行）

从 `data.result.output.list` 中，找到 `name` 字段与上下文 `goodsName` **完全一致**的条目，返回：

```
匹配结果：
- goodsId: {attributesJson.mallGoodsSnapshot.goodsId}
- goodsName: {name}
```

未匹配到时提示："未找到与「{goodsName}」完全匹配的商品"，不做模糊推断。

## 约束

- 不要在没有 storeId 的情况下发起请求
- 商品列表较长时，按分类折叠展示，等用户选择后再展开

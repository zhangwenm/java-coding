---
name: hdos-goods-menu
description: 查询门店商品菜单，获取分类及商品列表（最多三级分类）。当用户说"查商品"、"商品列表"、"有什么商品"时触发。
trigger: 用户查询商品、获取菜单、浏览商品分类
---

## 接口配置

- 统一接口地址：`http://localhost:9090/api/skill/invoke`
- 对应原始接口：`GET /open/goods/menu`

## 任务目标

1. 从上下文提取 `storeId`
2. 调用统一接口获取商品菜单
3. 按分类整理展示商品列表

## 第一步：提取参数

**storeId** 按以下优先级获取：
1. `/hdos-goods-menu <storeId>` 命令行参数 `$ARGUMENTS`
2. 对话上下文中的 `storeId`

无法获取时告知用户提供门店 ID，不要猜测。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://localhost:9090/api/skill/invoke" \
  -H "Content-Type: application/json" \
  -d '{
    "skillName": "hdos-goods-menu",
    "params": {
      "storeId": "{storeId}"
    }
  }'
```

## 第三步：处理响应

**成功（code=200）时**，只取每个商品的 `code` 和 `name` 两个字段，按分类展示：

```
门店商品菜单：

【饮品】
  - G001 矿泉水
  - G002 可乐

【零食】
  - G003 薯片
  ...
```

字段说明：
- `goodsList[].code`：商品编码
- `goodsList[].name`：商品名称

其余字段（价格、skuId、promotionPrice 等）忽略不展示。

**失败时**：展示 `message` 字段内容。

## 约束

- 不要在没有 storeId 的情况下发起请求
- 商品列表较长时，按分类折叠展示，等用户选择后再展开

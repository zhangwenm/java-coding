---
name: hdos-order-create
description: 创建订单接口。当用户确认商品和收货信息后，调用此接口下单。
trigger: 用户确认下单、创建订单、提交订单
---

## 接口配置

- 统一接口地址：`http://192.168.1.105:3100/l4/v1/skills/execute`

## 任务目标

1. 收集下单所需全部信息（商品、地址、支付方式等）
2. 调用统一接口创建订单
3. 返回订单号给用户

## 第一步：收集参数

下单前必须确认以下信息已就绪：

| 参数 | 说明 | 必填 |
|------|------|------|
| storeId | 门店ID | 是 |
| mobile | 下单手机号 | 是 |
| addressInfo.consigneeName | 收货人姓名 | 是 |
| addressInfo.consigneeMobile | 收货人手机 | 是 |
| addressInfo.consigneeDetail | 收货地址详情 | 是 |
| orderItems[].goodsId | 商品ID | 是 |
| orderItems[].skuId | SKU ID | 是 |
| orderItems[].quantity | 数量 | 是 |
| orderItems[].price | 单价 | 是 |
| orderItems[].goodsName | 商品名称 | 是 |
| orderItems[].skuSpec | 规格描述（如"颜色:红色,尺寸:M"） | 否 |
| orderItems[].image | 商品图片URL | 否 |
| payType | 支付方式（1=微信等） | 是 |
| remark | 备注 | 否 |
| nickname | 用户昵称 | 否 |

任何必填项缺失时，先向用户确认，不要补默认值。

## 第二步：调用统一接口

```bash
curl -s -X POST "http://192.168.1.105:3100/l4/v1/skills/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "skillId": "hdos-order-create",
    "skillParameters": {
      "storeId": "{storeId}",
      "mobile": "{mobile}",
      "addressInfo": {
        "consigneeName": "{consigneeName}",
        "consigneeMobile": "{consigneeMobile}",
        "consigneeDetail": "{consigneeDetail}"
      },
      "orderItems": [
        {
          "goodsId": "{goodsId}",
          "skuId": "{skuId}",
          "quantity": {quantity},
          "price": {price},
          "goodsName": "{goodsName}",
          "skuSpec": "{skuSpec}",
          "image": "{image}"
        }
      ],
      "payType": {payType},
      "remark": "{remark}",
      "nickname": "{nickname}"
    }
  }'
```

## 第三步：处理响应

**成功（code=0）时**：

```
订单创建成功！
订单号：2026031712345678
外部订单ID：hdos_1234567890

请完成支付。
```

响应字段：
- `data.orderSn`：订单序号，后续查询/支付使用
- `data.outOrderId`：外部订单ID

**失败时**：展示 `message` 字段内容，不要重试。

## 约束

- 下单前必须向用户二次确认商品和地址信息
- 不要自动重复下单

# apify-ecommerce

从 Amazon、Walmart、eBay、IKEA 和 50+ 电商平台抓取电商数据，包括定价智能、客户评论和卖家发现。

## 功能描述

用于监控价格、跟踪竞品、分析评论、研究产品或寻找卖家。

## 工作流程选择

| 用户需求 | 工作流 | 最佳用途 |
|----------|--------|----------|
| 跟踪价格、比较产品 | Workflow 1: Products & Pricing | 价格监控、MAP 合规、竞品分析 |
| 分析评论（情感或质量） | Workflow 2: Reviews | 品牌感知、客户情感、质量问题 |
| 跨商店寻找卖家 | Workflow 3: Sellers | 未授权卖家发现、供应商发现 |

## Workflow 1: 产品与定价

**用途**: 提取产品数据、价格和库存状态。跟踪竞品价格、检测 MAP 违规、基准产品或研究市场。

### 输入选项

| 输入类型 | 字段 | 描述 |
|----------|------|------|
| 产品 URL | `detailsUrls` | 直接产品页面 URL |
| 类别 URL | `listingUrls` | 类别/搜索结果页面 URL |
| 关键词搜索 | `keyword` + `marketplaces` | 跨选定市场的搜索词 |

### 示例 - 产品 URL

```json
{
  "detailsUrls": [
    {"url": "https://www.amazon.com/dp/B09V3KXJPB"},
    {"url": "https://www.walmart.com/ip/123456789"}
  ],
  "additionalProperties": true
}
```

### 示例 - 关键词搜索

```json
{
  "keyword": "Samsung Galaxy S24",
  "marketplaces": ["www.amazon.com", "www.walmart.com"],
  "additionalProperties": true,
  "maxProductResults": 50
}
```

## Workflow 2: 客户评论

**用途**: 提取评论用于情感分析、品牌感知监控或质量问题检测。

### 输入选项

| 输入类型 | 字段 | 描述 |
|----------|------|------|
| 产品 URL | `reviewListingUrls` | 要提取评论的产品页面 |
| 关键词搜索 | `keywordReviews` + `marketplacesReviews` | 按关键词搜索产品评论 |

### 排序选项

- `Most recent` - 最新评论
- `Most relevant` - 平台默认相关性
- `Most helpful` - 最高投票评论
- `Highest rated` - 5星评论优先
- `Lowest rated` - 1星评论优先

## Workflow 3: 卖家情报

**用途**: 跨商店寻找卖家、发现未授权卖家、评估供应商选项。

```json
{
  "googleShoppingSearchKeyword": "Nike Air Max 90",
  "scrapeSellersFromGoogleShopping": true,
  "countryCode": "us",
  "maxGoogleShoppingSellersPerProduct": 20,
  "maxGoogleShoppingResults": 100
}
```

## 支持的市场

### Amazon (20+ 区域)
`www.amazon.com`, `www.amazon.co.uk`, `www.amazon.de`, `www.amazon.fr`, `www.amazon.it`, `www.amazon.es`, `www.amazon.ca`, `www.amazon.com.au`, `www.amazon.co.jp`, `www.amazon.in` 等

### 主要美国零售商
`www.walmart.com`, `www.costco.com`, `www.homedepot.com`

### 欧洲零售商
`allegro.pl`, `www.alza.cz`, `www.kaufland.de` 等

### IKEA
支持 40+ 国家/语言组合

## 运行提取

```bash
# 快速答案
node --env-file=~/.claude/.env ~/.claude/skills/apify-ecommerce/reference/scripts/run_actor.js \
  --actor "apify/e-commerce-scraping-tool" --input 'JSON_INPUT'

# CSV 导出
node --env-file=~/.claude/.env ~/.claude/skills/apify-ecommerce/reference/scripts/run_actor.js \
  --actor "apify/e-commerce-scraping-tool" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.csv --format csv
```

## 错误处理

| 错误 | 解决方案 |
|------|----------|
| `APIFY_TOKEN not found` | 确保 `~/.claude/.env` 包含 `APIFY_TOKEN` |
| `Actor not found` | 验证 Actor ID: `apify/e-commerce-scraping-tool` |
| `Run FAILED` | 查看错误输出中的 Apify console 链接 |
| `Timeout` | 减小 `maxProductResults` 或增加 `--timeout` |

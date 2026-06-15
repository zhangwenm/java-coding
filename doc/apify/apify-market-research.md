# apify-market-research

分析 Google Maps、Facebook、Instagram、Booking.com 和 TripAdvisor 上的市场条件、地理机会、定价、消费者行为和产品验证。

## 功能描述

使用 Apify Actors 从多个平台提取数据进行市场调研。

## 工作流程

```
Task Progress:
- [ ] Step 1: 确定市场调研类型（选择 Actor）
- [ ] Step 2: 通过 mcpc 获取 Actor schema
- [ ] Step 3: 询问用户偏好（格式、文件名）
- [ ] Step 4: 运行分析脚本
- [ ] Step 5: 总结发现
```

## Step 1: 选择合适的 Actor

| 用户需求 | Actor ID | 最佳用途 |
|----------|----------|----------|
| 市场密度 | `compass/crawler-google-places` | 位置分析 |
| 地理空间分析 | `compass/google-maps-extractor` | 商业地图 |
| 区域兴趣 | `apify/google-trends-scraper` | 趋势数据 |
| 定价和需求 | `apify/facebook-marketplace-scraper` | 市场定价 |
| 活动市场 | `apify/facebook-events-scraper` | 活动分析 |
| 消费者需求 | `apify/facebook-groups-scraper` | 群组调研 |
| 市场格局 | `apify/facebook-pages-scraper` | 商业页面 |
| 文化洞察 | `apify/facebook-photos-scraper` | 视觉调研 |
| 利基定位 | `apify/instagram-hashtag-scraper` | 话题标签研究 |
| 市场活动 | `apify/instagram-reel-scraper` | 活动分析 |
| 酒店市场 | `voyager/booking-scraper` | 酒店数据 |
| 旅游洞察 | `maxcopell/tripadvisor-reviews` | 评论分析 |

## Step 2-4: 运行脚本

```bash
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.csv --format csv
```

## Step 5: 总结发现

完成后报告：
- 找到的结果数量
- 关键市场洞察
- 建议的后续步骤（更深入分析、验证）

# apify-brand-reputation-monitoring

跟踪 Google Maps、Booking.com、TripAdvisor、Facebook、Instagram、YouTube 和 TikTok 上的评论、评分、情感和品牌提及。

## 功能描述

使用 Apify Actors 从多个平台抓取评论、评分和品牌提及。

## 工作流程

```
Task Progress:
- [ ] Step 1: 确定数据源（选择 Actor）
- [ ] Step 2: 通过 mcpc 获取 Actor schema
- [ ] Step 3: 询问用户偏好（格式、文件名）
- [ ] Step 4: 运行监控脚本
- [ ] Step 5: 总结结果
```

## Step 1: 选择合适的 Actor

| 用户需求 | Actor ID | 最佳用途 |
|----------|----------|----------|
| Google Maps 评论 | `compass/crawler-google-places` | 商家评论、评分 |
| Google Maps 评论导出 | `compass/Google-Maps-Reviews-Scraper` | 专用评论抓取 |
| Booking.com 酒店 | `voyager/booking-scraper` | 酒店数据、评分 |
| Booking.com 评论 | `voyager/booking-reviews-scraper` | 详细酒店评论 |
| TripAdvisor 评论 | `maxcopell/tripadvisor-reviews` | 景点/餐厅评论 |
| Facebook 评论 | `apify/facebook-reviews-scraper` | 页面评论 |
| Facebook 页面指标 | `apify/facebook-pages-scraper` | 页面评分概览 |
| Instagram 评论 | `apify/instagram-comment-scraper` | 评论情感 |
| Instagram 话题标签 | `apify/instagram-hashtag-scraper` | 品牌话题监控 |
| Instagram 综合数据 | `apify/instagram-scraper` | 完整 Instagram 监控 |
| YouTube 评论 | `streamers/youtube-comments-scraper` | 视频评论情感 |
| TikTok 评论 | `clockworks/tiktok-comments-scraper` | TikTok 情感 |

## Step 2: 获取 Actor Schema

```bash
export $(grep APIFY_TOKEN .env | xargs) && mcpc --json mcp.apify.com \
  --header "Authorization: Bearer $APIFY_TOKEN" \
  tools-call fetch-actor-details actor:="ACTOR_ID" | jq -r ".content"
```

## Step 3: 询问用户偏好

运行前询问：
1. **输出格式**:
   - **快速答案** - 在聊天中显示结果
   - **CSV** - 完整导出
   - **JSON** - JSON 格式导出
2. **结果数量**

## Step 4: 运行脚本

```bash
# 快速答案
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT'

# CSV
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.csv --format csv

# JSON
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.json --format json
```

## Step 5: 总结结果

完成后报告：
- 找到的评论/提及数量
- 文件位置和名称
- 可用关键字段
- 建议的后续步骤（情感分析、过滤）

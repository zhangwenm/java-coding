# apify-ultimate-scraper

通用 AI 驱动的网页抓取工具，可从 55+ Actors 跨所有主要平台抓取数据。

## 功能描述

这是一个全能型抓取工具，能自动为任务选择最佳 Actor。适用于线索生成、品牌监控、竞品分析、红人发现、趋势研究、内容分析、受众分析或任何数据提取任务。

## 前提条件

- `.env` 文件中的 `APIFY_TOKEN`
- Node.js 20.6+
- `mcpc` CLI: `npm install -g @apify/mcpc`

## 平台覆盖

### Instagram (12 Actors)

| Actor ID | 最佳用途 |
|----------|----------|
| `apify/instagram-profile-scraper` | 档案数据、粉丝数、简介信息 |
| `apify/instagram-post-scraper` | 单个帖子详情、参与指标 |
| `apify/instagram-comment-scraper` | 评论提取、情感分析 |
| `apify/instagram-hashtag-scraper` | 话题标签内容、热门话题 |
| `apify/instagram-hashtag-stats` | 话题标签表现指标 |
| `apify/instagram-reel-scraper` | Reels 内容和指标 |
| `apify/instagram-search-scraper` | 搜索用户、地点、话题标签 |
| `apify/instagram-scraper` | 综合 Instagram 数据 |
| `apify/instagram-api-scraper` | API 访问 |

### Facebook (14 Actors)

| Actor ID | 最佳用途 |
|----------|----------|
| `apify/facebook-pages-scraper` | 页面数据、指标、联系信息 |
| `apify/facebook-posts-scraper` | 帖子内容和参与度 |
| `apify/facebook-comments-scraper` | 评论提取 |
| `apify/facebook-groups-scraper` | 群组内容和成员 |
| `apify/facebook-ads-scraper` | 广告创意和定向 |
| `apify/facebook-reviews-scraper` | 页面评论 |

### TikTok (14 Actors)

| Actor ID | 最佳用途 |
|----------|----------|
| `clockworks/tiktok-scraper` | 综合 TikTok 数据 |
| `clockworks/tiktok-profile-scraper` | 档案数据 |
| `clockworks/tiktok-video-scraper` | 视频详情和指标 |
| `clockworks/tiktok-comments-scraper` | 评论提取 |
| `clockworks/tiktok-hashtag-scraper` | 话题标签内容 |
| `clockworks/tiktok-sound-scraper` | 热门声音 |
| `clockworks/tiktok-trends-scraper` | 病毒内容 |

### YouTube (5 Actors)

| Actor ID | 最佳用途 |
|----------|----------|
| `streamers/youtube-scraper` | 视频数据和指标 |
| `streamers/youtube-channel-scraper` | 频道信息 |
| `streamers/youtube-comments-scraper` | 评论提取 |
| `streamers/youtube-shorts-scraper` | Shorts 内容 |

### Google Maps (4 Actors)

| Actor ID | 最佳用途 |
|----------|----------|
| `compass/crawler-google-places` | 商家列表、评分、联系信息 |
| `compass/google-maps-extractor` | 详细商家数据 |
| `compass/Google-Maps-Reviews-Scraper` | 评论提取 |
| `poidata/google-maps-email-extractor` | 从列表中发现邮箱 |

## 按用例选择 Actor

| 用例 | 主要 Actors |
|------|-------------|
| **线索生成** | `compass/crawler-google-places`, `poidata/google-maps-email-extractor` |
| **红人发现** | `apify/instagram-profile-scraper`, `clockworks/tiktok-profile-scraper` |
| **品牌监控** | `apify/instagram-tagged-scraper`, `compass/Google-Maps-Reviews-Scraper` |
| **竞品分析** | `apify/facebook-pages-scraper`, `apify/instagram-profile-scraper` |
| **内容分析** | `apify/instagram-post-scraper`, `clockworks/tiktok-scraper` |
| **趋势研究** | `apify/google-trends-scraper`, `clockworks/tiktok-trends-scraper` |
| **评论分析** | `compass/Google-Maps-Reviews-Scraper`, `voyager/booking-reviews-scraper` |

## 工作流程

```
Task Progress:
- [ ] Step 1: 理解用户目标并选择 Actor
- [ ] Step 2: 通过 mcpc 获取 Actor schema
- [ ] Step 3: 询问用户偏好（格式、文件名）
- [ ] Step 4: 运行抓取脚本
- [ ] Step 5: 总结结果并提供后续建议
```

## 运行脚本

```bash
# 快速答案
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT'

# CSV 导出
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.csv --format csv
```

## 找不到合适的 Actor？

搜索 Apify Store：

```bash
export $(grep APIFY_TOKEN .env | xargs) && mcpc --json mcp.apify.com \
  --header "Authorization: Bearer $APIFY_TOKEN" \
  tools-call search-actors keywords:="SEARCH_KEYWORDS" limit:=10 | jq -r '.content[0].text'
```

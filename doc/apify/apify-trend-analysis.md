# apify-trend-analysis

发现和跟踪 Google Trends、Instagram、Facebook、YouTube 和 TikTok 上的新兴趋势，为内容策略提供信息。

## 功能描述

使用 Apify Actors 从多个平台发现和跟踪新兴趋势。

## 工作流程

```
Task Progress:
- [ ] Step 1: 确定趋势类型（选择 Actor）
- [ ] Step 2: 通过 mcpc 获取 Actor schema
- [ ] Step 3: 询问用户偏好（格式、文件名）
- [ ] Step 4: 运行分析脚本
- [ ] Step 5: 总结发现
```

## Step 1: 选择合适的 Actor

| 用户需求 | Actor ID | 最佳用途 |
|----------|----------|----------|
| 搜索趋势 | `apify/google-trends-scraper` | Google Trends 数据 |
| 话题标签跟踪 | `apify/instagram-hashtag-scraper` | 话题标签内容 |
| 话题标签指标 | `apify/instagram-hashtag-stats` | 表现统计 |
| 视觉趋势 | `apify/instagram-post-scraper` | 帖子分析 |
| 趋势发现 | `apify/instagram-search-scraper` | 搜索趋势 |
| YouTube Shorts | `streamers/youtube-shorts-scraper` | Shorts 趋势 |
| YouTube 话题标签 | `streamers/youtube-video-scraper-by-hashtag` | 话题标签视频 |
| TikTok 话题标签 | `clockworks/tiktok-hashtag-scraper` | 话题标签内容 |
| 热门声音 | `clockworks/tiktok-sound-scraper` | 音频趋势 |
| TikTok 广告 | `clockworks/tiktok-ads-scraper` | 广告趋势 |
| 发现页面 | `clockworks/tiktok-discover-scraper` | 发现趋势 |
| 热门内容 | `clockworks/tiktok-trends-scraper` | 病毒内容 |

## Step 2-4: 运行脚本

```bash
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.csv --format csv
```

## Step 5: 总结发现

完成后报告：
- 找到的结果数量
- 关键趋势洞察
- 建议的后续步骤（更深入分析、内容机会）

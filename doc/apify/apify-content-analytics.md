# apify-content-analytics

跟踪 Instagram、Facebook、YouTube 和 TikTok 上的参与指标、衡量活动 ROI 和分析内容表现。

## 功能描述

使用 Apify Actors 从多个平台提取参与指标。

## 工作流程

```
Task Progress:
- [ ] Step 1: 确定内容分析类型（选择 Actor）
- [ ] Step 2: 通过 mcpc 获取 Actor schema
- [ ] Step 3: 询问用户偏好（格式、文件名）
- [ ] Step 4: 运行分析脚本
- [ ] Step 5: 总结发现
```

## Step 1: 选择合适的 Actor

| 用户需求 | Actor ID | 最佳用途 |
|----------|----------|----------|
| 帖子参与指标 | `apify/instagram-post-scraper` | 帖子表现 |
| Reels 表现 | `apify/instagram-reel-scraper` | Reels 分析 |
| 粉丝增长跟踪 | `apify/instagram-followers-count-scraper` | 增长指标 |
| 评论参与 | `apify/instagram-comment-scraper` | 评论分析 |
| 话题标签表现 | `apify/instagram-hashtag-scraper` | 品牌话题标签 |
| Facebook 帖子表现 | `apify/facebook-posts-scraper` | 帖子指标 |
| 反应分析 | `apify/facebook-likes-scraper` | 参与类型 |
| 广告表现跟踪 | `apify/facebook-ads-scraper` | 广告分析 |
| YouTube 视频指标 | `streamers/youtube-scraper` | 视频表现 |
| TikTok 内容指标 | `clockworks/tiktok-scraper` | TikTok 分析 |

## Step 2-4: 运行脚本

```bash
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.csv --format csv
```

## Step 5: 总结发现

完成后报告：
- 分析的内容数量
- 关键表现洞察
- 建议的后续步骤

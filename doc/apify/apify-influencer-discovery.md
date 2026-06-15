# apify-influencer-discovery

发现和评估品牌合作的红人，验证真实性，并跟踪跨 Instagram、Facebook、YouTube 和 TikTok 的合作表现。

## 功能描述

使用 Apify Actors 在多个平台发现和分析红人。

## 工作流程

```
Task Progress:
- [ ] Step 1: 确定发现来源（选择 Actor）
- [ ] Step 2: 通过 mcpc 获取 Actor schema
- [ ] Step 3: 询问用户偏好（格式、文件名）
- [ ] Step 4: 运行发现脚本
- [ ] Step 5: 总结结果
```

## Step 1: 选择合适的 Actor

| 用户需求 | Actor ID | 最佳用途 |
|----------|----------|----------|
| 红人档案 | `apify/instagram-profile-scraper` | 档案指标、简介、粉丝数 |
| 按话题标签发现 | `apify/instagram-hashtag-scraper` | 使用特定话题标签发现红人 |
| Reel 参与 | `apify/instagram-reel-scraper` | 分析 Reel 表现和参与度 |
| 按利基发现 | `apify/instagram-search-scraper` | 按关键词/利基搜索红人 |
| 品牌提及 | `apify/instagram-tagged-scraper` | 跟踪谁标记了品牌/产品 |
| 综合数据 | `apify/instagram-scraper` | 完整档案、帖子、评论分析 |
| API 发现 | `apify/instagram-api-scraper` | 快速 API 数据提取 |
| YouTube 创作者 | `streamers/youtube-channel-scraper` | 频道指标和订阅者数据 |
| TikTok 红人 | `clockworks/tiktok-scraper` | 综合 TikTok 数据提取 |
| TikTok (免费) | `clockworks/free-tiktok-scraper` | 免费 TikTok 数据提取器 |
| 直播红人 | `clockworks/tiktok-live-scraper` | 发现直播红人 |

## Step 2-4: 运行脚本

```bash
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.csv --format csv
```

## Step 5: 总结结果

完成后报告：
- 找到的红人数量
- 可用的关键指标（粉丝、参与率等）
- 建议的后续步骤（过滤、联系、更深入分析）

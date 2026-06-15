# apify-competitor-intelligence

分析 Google Maps、Booking.com、Facebook、Instagram、YouTube 和 TikTok 上的竞品策略、内容、定价、广告和市场定位。

## 功能描述

使用 Apify Actors 从多个平台提取数据进行分析。

## 工作流程

```
Task Progress:
- [ ] Step 1: 确定竞品分析类型（选择 Actor）
- [ ] Step 2: 通过 mcpc 获取 Actor schema
- [ ] Step 3: 询问用户偏好（格式、文件名）
- [ ] Step 4: 运行分析脚本
- [ ] Step 5: 总结发现
```

## Step 1: 选择合适的 Actor

### Google Maps
| 用户需求 | Actor ID |
|----------|----------|
| 竞品商家数据 | `compass/crawler-google-places` |
| 竞品联系发现 | `poidata/google-maps-email-extractor` |
| 功能基准 | `compass/google-maps-extractor` |

### Facebook
| 用户需求 | Actor ID |
|----------|----------|
| 竞品广告策略 | `apify/facebook-ads-scraper` |
| 竞品页面指标 | `apify/facebook-pages-scraper` |
| 竞品内容分析 | `apify/facebook-posts-scraper` |
| 竞品 Reels 表现 | `apify/facebook-reels-scraper` |

### Instagram
| 用户需求 | Actor ID |
|----------|----------|
| 竞品档案指标 | `apify/instagram-profile-scraper` |
| 竞品内容监控 | `apify/instagram-post-scraper` |
| 竞品参与度分析 | `apify/instagram-comment-scraper` |
| 综合竞品数据 | `apify/instagram-scraper` |

### YouTube/TikTok
| 用户需求 | Actor ID |
|----------|----------|
| YouTube 视频分析 | `streamers/youtube-scraper` |
| YouTube 情感分析 | `streamers/youtube-comments-scraper` |
| TikTok 竞品分析 | `clockworks/tiktok-scraper` |
| TikTok 视频策略 | `clockworks/tiktok-video-scraper` |

## Step 2: 获取 Actor Schema

```bash
export $(grep APIFY_TOKEN .env | xargs) && mcpc --json mcp.apify.com \
  --header "Authorization: Bearer $APIFY_TOKEN" \
  tools-call fetch-actor-details actor:="ACTOR_ID" | jq -r ".content"
```

## Step 3-5: 运行和总结

```bash
# 完整导出
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.csv --format csv
```

完成后报告：
- 分析的竞品数量
- 关键竞品洞察
- 建议的后续步骤

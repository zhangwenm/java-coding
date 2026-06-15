# apify-lead-generation

通过抓取 Google Maps、网站、Instagram、TikTok、Facebook、LinkedIn、YouTube 和 Google Search 生成 B2B/B2C 销售线索。

## 功能描述

用于寻找线索、潜在客户、企业信息、建立线索列表、丰富联系信息或抓取销售外联档案。

## 工作流程

```
Task Progress:
- [ ] Step 1: 确定线索来源（选择 Actor）
- [ ] Step 2: 通过 mcpc 获取 Actor schema
- [ ] Step 3: 询问用户偏好（格式、文件名）
- [ ] Step 4: 运行线索查找脚本
- [ ] Step 5: 总结结果
```

## Step 1: 选择合适的 Actor

| 用户需求 | Actor ID | 最佳用途 |
|----------|----------|----------|
| 本地商家 | `compass/crawler-google-places` | 餐厅、健身房、商店 |
| 联系信息丰富 | `vdrmota/contact-info-scraper` | 从 URL 提取邮箱、电话 |
| Instagram 档案 | `apify/instagram-profile-scraper` | 红人发现 |
| Instagram 帖子/评论 | `apify/instagram-scraper` | 帖子、评论、话题标签、地点 |
| TikTok 视频/话题标签 | `clockworks/tiktok-scraper` | 综合 TikTok 数据提取 |
| TikTok 用户搜索 | `clockworks/tiktok-user-search-scraper` | 按关键词查找用户 |
| Facebook 页面 | `apify/facebook-pages-scraper` | 商业联系 |
| Facebook 页面联系信息 | `apify/facebook-page-contact-information` | 提取邮箱、电话、地址 |
| Facebook 群组 | `apify/facebook-groups-scraper` | 购买意图信号 |
| Facebook 活动 | `apify/facebook-events-scraper` | 活动社交、合作关系 |
| Google 搜索 | `apify/google-search-scraper` | 广泛线索发现 |
| Google Maps 邮箱 | `poidata/google-maps-email-extractor` | 直接邮箱提取 |
| YouTube 频道 | `streamers/youtube-scraper` | 创作者合作 |

## Step 2-4: 运行脚本

```bash
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_FILE.csv --format csv
```

## Step 5: 总结结果

完成后报告：
- 找到的线索数量
- 可用的关键字段
- 建议的后续步骤（过滤、丰富）

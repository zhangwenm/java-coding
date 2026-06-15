# apify-audience-analysis

了解 Facebook、Instagram、YouTube 和 TikTok 上的受众特征、偏好、行为模式和参与质量。

## 功能描述

分析社交媒体受众，提取粉丝人口统计、参与模式和行为数据。

## 工作流程

```
Task Progress:
- [ ] Step 1: 确定受众分析类型（选择 Actor）
- [ ] Step 2: 通过 mcpc 获取 Actor schema
- [ ] Step 3: 询问用户偏好（格式、文件名）
- [ ] Step 4: 运行分析脚本
- [ ] Step 5: 总结发现
```

## Step 1: 选择合适的 Actor

| 用户需求 | Actor ID | 最佳用途 |
|----------|----------|----------|
| Facebook 粉丝人口统计 | `apify/facebook-followers-following-scraper` | FB 粉丝/关注列表 |
| Facebook 参与行为 | `apify/facebook-likes-scraper` | FB 帖子点赞分析 |
| Facebook 视频受众 | `apify/facebook-reels-scraper` | FB Reels 观众 |
| Facebook 评论分析 | `apify/facebook-comments-scraper` | FB 帖子/视频评论 |
| Instagram 受众规模 | `apify/instagram-profile-scraper` | IG 粉丝人口统计 |
| Instagram 基于位置 | `apify/instagram-search-scraper` | IG 地理标记受众 |
| Instagram 综合数据 | `apify/instagram-scraper` | 完整 IG 受众数据 |
| Instagram API 访问 | `apify/instagram-api-scraper` | IG API 访问 |
| YouTube 频道受众 | `streamers/youtube-channel-scraper` | YT 频道订阅者 |
| TikTok 粉丝人口统计 | `clockworks/tiktok-followers-scraper` | TT 粉丝列表 |
| TikTok 评论分析 | `clockworks/tiktok-comments-scraper` | TT 评论参与度 |

## Step 2: 获取 Actor Schema

```bash
export $(grep APIFY_TOKEN .env | xargs) && mcpc --json mcp.apify.com \
  --header "Authorization: Bearer $APIFY_TOKEN" \
  tools-call fetch-actor-details actor:="ACTOR_ID" | jq -r ".content"
```

返回：
- Actor 描述和 README
- 必需和可选输入参数
- 输出字段

## Step 3: 询问用户偏好

运行前询问：
1. **输出格式**:
   - **快速答案** - 在聊天中显示少量结果（不保存文件）
   - **CSV** - 所有字段完整导出
   - **JSON** - JSON 格式完整导出
2. **结果数量**: 根据用例特点

## Step 4: 运行脚本

**快速答案（聊天中显示）:**
```bash
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT'
```

**CSV:**
```bash
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_OUTPUT_FILE.csv --format csv
```

**JSON:**
```bash
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output YYYY-MM-DD_OUTPUT_FILE.json --format json
```

## Step 5: 总结发现

完成后报告：
- 分析的受众成员/档案数量
- 文件位置和名称
- 关键人口统计洞察
- 建议的后续步骤

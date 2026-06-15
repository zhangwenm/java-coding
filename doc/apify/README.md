# Apify Skills Documentation

本目录包含 Apify 平台相关的技能文档，用于网页抓取、数据提取、自动化等任务。

## 技能列表

| 技能名称 | 功能描述 |
|----------|----------|
| [apify-actor-development](apify-actor-development.md) | 开发、调试和部署 Apify Actors |
| [apify-actorization](apify-actorization.md) | 将现有项目转换为 Apify Actors |
| [apify-audience-analysis](apify-audience-analysis.md) | 分析社交媒体受众特征 |
| [apify-brand-reputation-monitoring](apify-brand-reputation-monitoring.md) | 监控品牌声誉和评论 |
| [apify-competitor-intelligence](apify-competitor-intelligence.md) | 竞品分析 |
| [apify-content-analytics](apify-content-analytics.md) | 内容表现分析 |
| [apify-ecommerce](apify-ecommerce.md) | 电商数据提取 |
| [apify-influencer-discovery](apify-influencer-discovery.md) | 发现 KOL/网红 |
| [apify-lead-generation](apify-lead-generation.md) | 销售线索生成 |
| [apify-market-research](apify-market-research.md) | 市场调研 |
| [apify-trend-analysis](apify-trend-analysis.md) | 趋势分析 |
| [apify-ultimate-scraper](apify-ultimate-scraper.md) | 通用网页抓取工具 |

## 通用前提条件

大部分 Apify 技能需要以下环境配置：

1. **APIFY_TOKEN** - 在 `.env` 文件中配置
   ```bash
   APIFY_TOKEN=your_token_here
   ```

2. **Node.js 20.6+** - 用于运行脚本

3. **mcpc CLI** (可选) - 用于获取 Actor schema
   ```bash
   npm install -g @apify/mcpc
   ```

## 通用工作流程

### Step 1: 选择合适的 Actor

根据任务需求选择对应的 Actor：
- 社交媒体分析 → audience-analysis, content-analytics
- 品牌监控 → brand-reputation-monitoring
- 竞品分析 → competitor-intelligence
- 电商数据 → ecommerce
- 线索生成 → lead-generation

### Step 2: 获取 Actor Schema

```bash
export $(grep APIFY_TOKEN .env | xargs) && mcpc --json mcp.apify.com \
  --header "Authorization: Bearer $APIFY_TOKEN" \
  tools-call fetch-actor-details actor:="ACTOR_ID" | jq -r ".content"
```

### Step 3: 运行提取脚本

```bash
# 快速显示（无文件保存）
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT'

# CSV 导出
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output OUTPUT_FILE.csv --format csv

# JSON 导出
node --env-file=.env ${CLAUDE_PLUGIN_ROOT}/reference/scripts/run_actor.js \
  --actor "ACTOR_ID" --input 'JSON_INPUT' \
  --output OUTPUT_FILE.json --format json
```

## 错误处理

| 错误信息 | 解决方案 |
|----------|----------|
| `APIFY_TOKEN not found` | 在 `.env` 文件中添加 `APIFY_TOKEN=your_token` |
| `mcpc not found` | 运行 `npm install -g @apify/mcpc` |
| `Actor not found` | 检查 Actor ID 拼写是否正确 |
| `Run FAILED` | 查看错误输出中的 Apify console 链接 |
| `Timeout` | 减小输入大小或增加 `--timeout` |

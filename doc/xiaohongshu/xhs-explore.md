# xhs-explore

小红书内容发现与分析技能。搜索笔记、浏览首页、查看详情、获取用户资料。

## 功能描述

当用户要求搜索小红书、查看笔记详情、浏览首页、查看用户主页时触发。

## 允许使用的 CLI 子命令

| 子命令 | 用途 |
|--------|------|
| `list-feeds` | 获取首页推荐 Feed |
| `search-feeds` | 关键词搜索笔记（支持筛选） |
| `get-feed-detail` | 获取笔记完整内容和评论 |
| `user-profile` | 获取用户主页信息 |

## 账号选择（前置步骤）

每次 skill 触发后，先运行：
```bash
python scripts/cli.py list-accounts
```

根据返回的 `count`：
- **0 个命名账号**：直接使用默认账号
- **1 个命名账号**：告知用户，直接加 `--account <名称>`
- **多个命名账号**：向用户展示列表，询问选择

## 工作流程

### 首页 Feed 列表

```bash
python scripts/cli.py list-feeds
```

输出 JSON 包含 `feeds` 数组和 `count`。

### 搜索笔记

```bash
# 基础搜索
python scripts/cli.py search-feeds --keyword "春招"

# 带筛选搜索
python scripts/cli.py search-feeds \
  --keyword "春招" \
  --sort-by 最新 \
  --note-type 图文

# 完整筛选
python scripts/cli.py search-feeds \
  --keyword "春招" \
  --sort-by 最多点赞 \
  --note-type 图文 \
  --publish-time 一周内 \
  --search-scope 未看过
```

#### 搜索筛选参数

| 参数 | 可选值 |
|------|--------|
| `--sort-by` | 综合、最新、最多点赞、最多评论、最多收藏 |
| `--note-type` | 不限、视频、图文 |
| `--publish-time` | 不限、一天内、一周内、半年内 |
| `--search-scope` | 不限、已看过、未看过、已关注 |
| `--location` | 不限、同城、附近 |

### 获取笔记详情

```bash
# 基础详情
python scripts/cli.py get-feed-detail \
  --feed-id FEED_ID --xsec-token XSEC_TOKEN

# 加载全部评论
python scripts/cli.py get-feed-detail \
  --feed-id FEED_ID --xsec-token XSEC_TOKEN \
  --load-all-comments

# 加载全部评论（展开子评论）
python scripts/cli.py get-feed-detail \
  --feed-id FEED_ID --xsec-token XSEC_TOKEN \
  --load-all-comments --click-more-replies \
  --max-replies-threshold 10
```

### 获取用户主页

```bash
python scripts/cli.py user-profile \
  --user-id USER_ID --xsec-token XSEC_TOKEN
```

## 结果呈现

1. **笔记列表**：每条笔记展示标题、作者、互动数据
2. **详情内容**：完整的笔记正文、图片、评论
3. **用户资料**：基本信息 + 代表作列表
4. **数据表格**：使用 markdown 表格展示关键指标

## 失败处理

- **未登录**：提示用户先执行登录（参考 xhs-auth）
- **搜索无结果**：建议更换关键词或调整筛选条件
- **笔记不可访问**：可能是私密笔记或已删除
- **用户主页不可访问**：用户可能已注销或设置隐私

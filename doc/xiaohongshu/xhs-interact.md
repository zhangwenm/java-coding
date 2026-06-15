# xhs-interact

小红书社交互动技能。发表评论、回复评论、点赞、收藏。

## 功能描述

当用户要求评论、回复、点赞或收藏小红书帖子时触发。

## 允许使用的 CLI 子命令

| 子命令 | 用途 |
|--------|------|
| `post-comment` | 对笔记发表评论 |
| `reply-comment` | 回复指定评论或用户 |
| `like-feed` | 点赞 / 取消点赞 |
| `favorite-feed` | 收藏 / 取消收藏 |

## 账号选择（前置步骤）

每次 skill 触发后，先运行：
```bash
python scripts/cli.py list-accounts
```

## 工作流程

### 发表评论

1. 确认已有 `feed_id` 和 `xsec_token`（如没有，先搜索或获取详情）
2. 向用户确认评论内容
3. 执行发送

```bash
python scripts/cli.py post-comment \
  --feed-id FEED_ID \
  --xsec-token XSEC_TOKEN \
  --content "写得很实用，感谢分享"
```

### 回复评论

```bash
# 回复指定评论（通过评论 ID）
python scripts/cli.py reply-comment \
  --feed-id FEED_ID \
  --xsec-token XSEC_TOKEN \
  --content "谢谢你的分享" \
  --comment-id COMMENT_ID

# 回复指定用户（通过用户 ID）
python scripts/cli.py reply-comment \
  --feed-id FEED_ID \
  --xsec-token XSEC_TOKEN \
  --content "谢谢你的分享" \
  --user-id USER_ID
```

### 点赞 / 取消点赞

```bash
# 点赞
python scripts/cli.py like-feed \
  --feed-id FEED_ID --xsec-token XSEC_TOKEN

# 取消点赞
python scripts/cli.py like-feed \
  --feed-id FEED_ID --xsec-token XSEC_TOKEN --unlike
```

### 收藏 / 取消收藏

```bash
# 收藏
python scripts/cli.py favorite-feed \
  --feed-id FEED_ID --xsec-token XSEC_TOKEN

# 取消收藏
python scripts/cli.py favorite-feed \
  --feed-id FEED_ID --xsec-token XSEC_TOKEN --unfavorite
```

## 约束

- **评论和回复内容必须经过用户确认后才能发送**
- 所有互动操作需要 `feed_id` 和 `xsec_token`
- 评论文本不可为空
- 点赞和收藏操作是幂等的

## 互动策略建议

1. 先搜索目标内容（xhs-explore）
2. 浏览搜索结果，选择要互动的笔记
3. 获取详情确认内容
4. 针对性地发表评论 / 点赞 / 收藏
5. 每次互动之间保持合理间隔

## 失败处理

- **未登录**：提示先登录（参考 xhs-auth）
- **笔记不可访问**：可能是私密或已删除笔记
- **评论输入框未找到**：页面结构可能已变化
- **评论发送失败**：检查内容是否包含敏感词
- **点赞/收藏失败**：重试一次，仍失败则报告错误

# XiaoHongShu Skills Documentation

本目录包含小红书自动化相关的技能文档。

## 技能列表

| 技能名称 | 功能描述 |
|----------|----------|
| [xiaohongshu-skills](xiaohongshu-skills.md) | 小红书自动化技能集合主入口 |
| [xhs-auth](xhs-auth.md) | 认证管理（登录、状态检查、多账号） |
| [xhs-explore](xhs-explore.md) | 内容发现（搜索、浏览、详情） |
| [xhs-interact](xhs-interact.md) | 社交互动（评论、点赞、收藏） |
| [xhs-publish](xhs-publish.md) | 内容发布（图文、视频、长文） |
| [xhs-content-ops](xhs-content-ops.md) | 复合运营（竞品分析、热点追踪） |

## 通用前提条件

1. **Python 3** 和 **uv** 包管理器
2. **Google Chrome** 浏览器
3. **APIFY_TOKEN** (部分功能需要)

## 通用工作流程

```
1. 检查登录状态
   python scripts/cli.py check-login

2. 启动 Chrome (如需要)
   python scripts/chrome_launcher.py

3. 执行具体操作
```

## 路由规则

根据用户意图路由到对应子技能:

1. **认证相关**（"登录/检查登录/切换账号"）→ `xhs-auth`
2. **内容发布**（"发布/发帖/上传图文"）→ `xhs-publish`
3. **搜索发现**（"搜索笔记/查看详情/浏览首页"）→ `xhs-explore`
4. **社交互动**（"评论/回复/点赞/收藏"）→ `xhs-interact`
5. **复合运营**（"竞品分析/热点追踪/一键创作"）→ `xhs-content-ops`

## 安全约束

- 所有操作必须通过 `python scripts/cli.py <子命令>` 完成
- 不得使用 MCP 工具、Go 命令行或其他外部实现
- 发布和评论操作必须经过用户确认后才能执行
- 文件路径必须使用绝对路径

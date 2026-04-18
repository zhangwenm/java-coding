---
tags:
  - 工具链
  - Claude-Code
  - 多Agent
status: done
created: 2026-04-18
---

# cmux — Claude Code 多 Agent 并行开发

## 是什么

cmux 是专为 Claude Code 设计的 Git Worktree 生命周期管理器。

**核心思路**：给每个并行任务创建独立 worktree，共享同一个 `.git` 数据库，多个 Claude Agent 互不干扰。

```
主目录/                    ← review / merge 在这里
.worktrees/
  feature-auth/           ← Agent A 独立工作
  fix-bug-123/            ← Agent B 独立工作
  refactor-service/       ← Agent C 独立工作
```

## 为什么需要它

传统 tmux 是为"人操作终端"设计的，在多 Agent 场景下暴露瓶颈：

| 问题 | 根因 |
|------|------|
| 多窗口长得一样 | 无语义化状态，无法一眼判断 Agent 在做什么 |
| 缺乏上下文 | 分支/PR/端口信息分散，切换成本高 |
| 通知同质化 | 不知道该响应哪个，全是噪音 |
| 共享目录冲突 | 多 Agent 同时改同一文件，互相踩 |

cmux 的本质升级：**从"命令驱动"到"事件驱动"，从"逐个操作"到"监控与干预"**。

## 安装

```bash
curl -fsSL https://github.com/craigsc/cmux/releases/latest/download/install.sh | sh
source ~/.zshrc

# 在项目根目录
echo '.worktrees/' >> .gitignore
```

## 核心命令

```bash
# 新建任务（创建 worktree + 分支，打开 Claude 会话）
cmux new feature-xxx -p "任务描述，给 Claude 的初始 prompt"

# 恢复已有任务的 Claude 会话
cmux start feature-xxx

# 查看所有活跃 worktree
cmux ls

# 合并到主分支
cmux merge feature-xxx          # 普通 merge
cmux merge feature-xxx --squash # squash merge

# 删除 worktree + 分支
cmux rm feature-xxx
cmux rm --all                   # 清空所有

# 生成项目初始化脚本（让 Claude 自动生成）
cmux init
```

## .cmux/setup 初始化脚本

worktree 创建时自动执行，用于 symlink 密钥文件、安装依赖等：

```bash
#!/bin/bash
REPO_ROOT="$(git rev-parse --git-common-dir | xargs dirname)"

# symlink 忽略的配置文件
ln -sf "$REPO_ROOT/.env" .env

# Maven 项目示例
# mvn dependency:resolve -q
```

运行 `cmux init` 让 Claude 根据当前仓库自动生成。

## 与 Obsidian 任务模板的结合

| 任务模板字段 | cmux 对应操作 |
|------------|-------------|
| 下次启动入口 | `cmux start <branch>` |
| status: in-progress | `cmux new <branch>` 已执行 |
| status: done | `cmux merge <branch> && cmux rm <branch>` |

**推荐工作流**：

```
1. 在 obsidian/tasks/ 新建任务文档（用 task-template）
2. cmux new <branch> -p "任务目标"  → Claude 开始执行
3. 会话结束前更新任务模板"当前状态快照"
4. 下次：cmux start <branch> 恢复
5. 完成：cmux merge → cmux rm → 更新任务 status: done
```

## 注意事项

- **cmux vs tmux**：不是替代关系。本地多 Agent 并行用 cmux，远程长任务仍用 tmux
- **写操作风险**：cmux 通过 Socket API 可编排多 Agent 调度，授权范围要清楚
- **Maven 项目**：setup 脚本里建议加 `mvn dependency:resolve`，避免 worktree 里编译找不到依赖

## 参考

- [craigsc/cmux](https://github.com/craigsc/cmux)
- 微博 @药研智能社 2026-04-10：Claude Code 并行开发从 tmux 到多 Agent 编排
- 微博 @OpenClaw 2026-03-26：Claude Code Auto Mode 解决批准疲劳问题

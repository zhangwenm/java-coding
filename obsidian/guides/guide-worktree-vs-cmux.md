---
tags:
  - Claude
  - worktree
  - cmux
  - 对比
  - 工作流
date: 2026-04-21
---

# Claude Worktree vs cmux：选哪个？

> 参考：[[guide-cc-worktree-parallel]] / [[guide-cmux-workflow]]

---

## 一句话区别

| | Claude Worktree | cmux |
|---|---|---|
| **定位** | Claude Code 内置的轻量隔离 | OpenCode (Sisyphus) 的重量级任务管理套件 |
| **核心价值** | 文件隔离 + 并行会话 | 任务记忆 + 跨会话持续推进 |
| **适合时长** | 短中期任务（小时级） | 长期任务（天/周级） |

---

## 深度对比

### 工具链复杂度

```
Claude Worktree：
  Claude Code（内置）
  └─ claude --worktree <name>   # 一个命令搞定

cmux：
  OpenCode (Sisyphus)
  + cmux (shell 工具)
  + Obsidian 任务文档
  + task-resume skill
  + task-checkpoint skill
```

cmux 需要整套工具链就位，claude worktree 开箱即用。

---

### 任务记忆

**Claude Worktree**：无内置任务记忆

- 会话历史靠 `claude --resume` 恢复
- 会话上下文压缩后细节会丢失
- 跨天恢复需要手动重新交代背景

**cmux**：Obsidian 文档是持久化任务记忆

- 每次 checkpoint 记录"待续位置"精确到文件/行
- 跨天说"继续" → task-resume 自动读取文档恢复上下文
- 里程碑、决策记录、阻塞项全部持久化

**结论**：任务超过 1 天或需要多次中断恢复，cmux 有明显优势。

---

### Worktree 管理

| 操作 | Claude Worktree | cmux |
|---|---|---|
| 创建 | `claude --worktree <name>` | `cmux new <name> -p "描述"` |
| 查看 | `git worktree list` | `cmux ls` |
| 切换 | 直接切换终端/目录 | `cmux start <name>` |
| 合并 | `git merge` 手动 | `cmux merge <name>` |
| 清理 | 退出时自动或手动 `git worktree remove` | `cmux rm <name>` |

cmux 封装了完整生命周期，claude worktree 的清理更自动（无改动自动删除）。

---

### 多仓库支持

**Claude Worktree**：单仓库视角

- worktree 目录 `.claude/worktrees/` 在当前仓库内
- 跨仓库需要在每个仓库分别启动会话

**cmux**：天然支持多仓库

- 同一个 branch 名可以在多个仓库下各建一个 worktree
- 一份 Obsidian 文档可以记录三个仓库的涉及文件
- 典型场景：Hessian 接口改动，common + base + manager 三仓库同步推进

```bash
# cmux 多仓库示例
cd ~/appstore/project/common/webservice
cmux new feature-new-api -p "定义接口"

cd ~/appstore/project/base/config  
cmux new feature-new-api -p "实现接口"

cd ~/appstore/project/manager/rw-backend
cmux new feature-new-api -p "调用接口"
# 同一个 branch 名，一份 Obsidian 文档统管
```

---

### 会话恢复对比

**Claude Worktree**：

```bash
claude --continue   # 恢复最近会话
claude --resume     # 弹出选择器
# Ctrl+W 可看所有 worktree 的历史会话
```

靠会话历史恢复，上下文越压缩越模糊。

**cmux**：

```
说"继续"
→ task-resume 扫描 Obsidian tasks/ 找 status: in-progress
→ 读取"当前状态快照"中的待续位置
→ cmux start 恢复 worktree
→ 精确到文件/方法/行继续
```

靠文档恢复，精度不依赖会话历史。

---

### 并行模式差异

**Claude Worktree 的并行**：多个 Claude Code 会话同时跑

```
终端1：Writer 会话写代码
终端2：Reviewer 会话审代码   ← Claude Code 特有的 Writer/Reviewer 双会话模式
终端3：Spike 会话验证方案
```

**cmux 的并行**：多个 Sisyphus 任务并行推进

```
任务A：status: in-progress（当前活跃）
任务B：status: blocked（等待外部）
任务C：status: in-progress（另一个终端活跃）
# 同一时刻实际只有 1-2 个会话在跑，靠文档切换上下文
```

---

## 选择指南

### 用 Claude Worktree 当：

- 任务能在当天完成，不需要跨天持久化
- 想快速探索方案（spike），验证后决定要不要
- 需要 Writer + Reviewer 双 AI 视角互相检查
- 用的是 Claude Code（不是 OpenCode/Sisyphus）
- 不想维护额外工具链

### 用 cmux 当：

- 任务超过 1 天，需要多次中断和恢复
- 涉及多个 git 仓库同步变更（Hessian 接口等）
- 同时有 3+ 个并行任务需要状态管理
- 需要记录里程碑、决策、阻塞项等结构化信息
- 用的是 OpenCode/Sisyphus Agent

---

## 能结合使用吗？

**可以，但有前提**。

当前 cmux 是为 OpenCode (Sisyphus) 设计的，没有 Claude Code 版本的 task-resume / task-checkpoint skill。  
如果你在 Claude Code 里用 cmux 创建的 worktree：

```bash
# 手动创建 worktree（不通过 cmux 命令，直接 git）
git worktree add .worktrees/feature-auth -b feature-auth

# 然后用 cc 启动
cc .worktrees/feature-auth
```

可以用，但失去了 task-resume/checkpoint 的自动化，等于手动维护 Obsidian 文档——还不如直接用 `claude --worktree`。

**真正的结合点**：把 Claude Code Worktree 的 **Writer/Reviewer 双会话模式**移植到 cmux 工作流里，用两个 Sisyphus 会话互相复查。

---

## 总结

```
短任务 / 探索 / 当天完成      → Claude Worktree（轻量，开箱即用）
长任务 / 多仓库 / 跨天持续     → cmux（重量，任务记忆完整）
两者都用但不混用              → 按任务性质分开选，不要在一个任务里混
```

---
tags: [工具链, worktree, 并行开发]
date: 2026-04-21
project: 工具链
status: done
retrieval_triggers: [worktree并行, 多任务, 并行开发, 最佳实践, gstack worktree, using-git-worktrees]
---

# Claude + Git Worktree 并行开发最佳实践

## 核心思想

一个 worktree = 一个独立目录 + 一个独立分支 + 一个 Claude 会话。  
多个 worktree 共享同一个 git 仓库历史，文件彼此隔离，互不干扰。

**适用场景**：同时做多件事，且不想频繁切换分支丢失上下文。

---

## 快速上手

```bash
# 终端1：功能开发
claude --worktree feature-auth

# 终端2：同时修 bug（另开终端）
claude --worktree bugfix-login

# 不在乎名字，随机生成
claude --worktree
```

启动后 Claude 自动完成：
- 在 `.claude/worktrees/<name>/` 创建隔离目录
- 从 `origin/HEAD`（默认远端分支）切出新分支 `worktree-<name>`
- 在该目录启动独立会话

---

## 推荐工作模式

### 模式一：Writer + Reviewer 双会话

```
终端1（Writer）：claude --worktree feat-payment
  → Claude 写代码、跑测试、提交

终端2（Reviewer）：claude --worktree review-payment
  → Claude 审查 feat-payment 的改动，给出意见
```

两个会话互相独立，Reviewer 读 Writer 的 diff，形成内部复查循环。

### 模式二：主线 + 探索双轨

```
终端1（主线）：正常 cc 启动，推进主任务
终端2（探索）：claude --worktree spike-redis-cache
  → 快速验证技术方案，不污染主线
  → 验证完：采纳则合并，放弃则直接删除 worktree
```

### 模式三：多模块并行

```
终端1：claude --worktree iot-api-refactor     # iot-min 模块
终端2：claude --worktree datacenter-push-fix  # datacenter 模块
终端3：claude --worktree common-util-upgrade  # common 模块
```

跨模块独立推进，各自编译各自提交，不相互等待。

---

## 环境初始化（重要）

Worktree 是全新 checkout，**不继承**原目录的本地状态：

```bash
# Java 项目：进入 worktree 后先编译确认环境
cd .claude/worktrees/feature-auth
mvn clean compile

# Node 项目
npm install

# 需要 .env 文件的项目：在项目根创建 .worktreeinclude
echo ".env" >> .worktreeinclude
echo ".env.local" >> .worktreeinclude
# Claude 创建 worktree 时会自动复制这些文件
```

---

## .gitignore 必须配置

```gitignore
# 加到项目根的 .gitignore
.claude/worktrees/
```

不加的话，`git status` 会出现大量未追踪文件，非常干扰。

---

## 会话恢复

```bash
# 恢复当前目录最近的会话
claude --continue

# 弹出选择器（Ctrl+W 切换显示所有 worktree 的会话）
claude --resume
```

---

## Worktree 生命周期

| 退出时状态 | 结果 |
|---|---|
| 无任何改动 | worktree 目录和分支自动删除 |
| 有改动/提交 | Claude 询问是否保留 |
| 手动清理 | `git worktree remove .claude/worktrees/<name>` |
| 查看所有 | `git worktree list` |

---

## 踩坑记录

### 坑1：worktree 里跑编译找不到依赖

**原因**：本地 Maven/npm 缓存在 `~/.m2` / `node_modules`，这部分共享没问题；但如果原目录有未发布到本地仓库的 snapshot 依赖，worktree 里拿不到。

**解法**：在原目录先 `mvn install`，再启动 worktree 里的会话。

### 坑2：两个会话同时改同一个文件

Worktree 隔离的是工作目录，不是 git 历史。如果两个 worktree 都从同一个起点切出，最终合并时仍然会有冲突。

**解法**：任务拆分时按模块/文件维度划分，明确边界，避免两个 Claude 同时动同一个文件。

### 坑3：origin/HEAD 指向不对

Worktree 从 `origin/HEAD` 切分支，如果远端默认分支变更过但本地没同步：

```bash
git remote set-head origin -a  # 重新同步远端 HEAD 指向
```

### 坑4：子 Agent 没有用 worktree 隔离

Claude 有时会在任务中 spawn 子 Agent，子 Agent 默认在当前目录工作，可能踩到主会话的文件。

**解法**：在提示词里明确说 `use worktrees for your agents`，每个子 Agent 会获得独立 worktree。

---

## 与 cc 别名结合使用

当前 `cc` 函数支持指定目录启动：

```bash
# 直接在 worktree 目录启动 Claude（带代理）
cc .claude/worktrees/feature-auth

# 等价于
cd .claude/worktrees/feature-auth && claude
```

---

## 决策树：什么时候用 worktree？

```
任务能在 1 个会话内完成？
  ├─ 是 → 直接 cc，不需要 worktree
  └─ 否 → 是否需要同时推进多个方向？
            ├─ 是 → 用 worktree，每个方向一个会话
            └─ 否 → 是否是探索性/不确定方案？
                      ├─ 是 → 用 worktree spike，验证后决定是否合并
                      └─ 否 → 直接 cc 顺序推进
```

---

## gstack 的 using-git-worktrees skill（与 --worktree 的区别）

**核心区别**：`claude --worktree` 是 Claude Code 内置功能（你手动触发）；`using-git-worktrees` 是 gstack skill，由 Claude 在执行任务时自动调用。

### 触发方式

不是你直接运行命令，而是告诉 Claude 在隔离工作区做事：

```
你：基于 develop 分支，在隔离工作区实现 feature/xxx
Claude：我来用 using-git-worktrees 建隔离工作区...（自动执行）
```

### 存放目录

优先级：`.worktrees/`（项目本地隐藏目录）> `worktrees/` > CLAUDE.md 配置 > 询问用户。

Maven 项目提前建好：

```bash
mkdir .worktrees
echo '.worktrees/' >> .gitignore
```

skill 会验证目录在 `.gitignore` 里，不在则自动补上再提交。

### 基准分支：默认当前 HEAD（关键坑）

skill 执行的是 `git worktree add .worktrees/<branch> -b <branch>`，**不指定来源**，因此新分支基于**当前你所在的分支（HEAD）**，不是 `origin/HEAD`，也不是 `develop`。

**实际影响**：

| 你当前在哪 | 新 worktree 基于 |
|-----------|----------------|
| main | main |
| develop | develop |
| feature/other | feature/other（通常不对）|

**规避方法**：在提示词里明确说来源分支：

```
"基于 develop 分支，在隔离工作区实现 xxx"
```

或者在让 Claude 建 worktree 前先 `git checkout develop`。

### 自动做的事

1. 检查并确保目录在 `.gitignore`
2. `git worktree add` 新建分支
3. 检测项目类型，自动跑依赖安装（Maven → `mvn install`，Node → `npm install`，等）
4. 跑一遍测试，确认基线干净，失败则暂停询问

### 与 claude --worktree 对比

| 维度 | claude --worktree | gstack using-git-worktrees |
|------|-------------------|---------------------------|
| 触发方 | 用户手动 | Claude 自动（任务执行中）|
| 存放位置 | `.claude/worktrees/` | `.worktrees/`（可配置）|
| 基准分支 | `origin/HEAD` | 当前 HEAD ⚠️ |
| 适用场景 | 你要同时跑多个会话 | Claude 在单次任务中建隔离环境 |

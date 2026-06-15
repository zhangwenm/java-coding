---
title: 开发新需求工作流（Claude Code）
tags: [guide, claude-code, workflow, feature-dev]
created: 2026-05-29
retrieval_triggers: [新功能, 开发需求, 怎么开始, 开发流程, 工作流, feature-dev, 需求开发]
---

# 开发新需求工作流

> 基于当前 hooks/rules/skills 配置的标准流程。
> 系统会在正确节点拦截和提醒，**你唯一需要主动记住的只有第 0 步和第 1 步**。

---

## 流程总览

```
正确目录启动
  → 第一条消息描述需求（自动注入上下文）
  → /arch-docs 查有无已有方案
  → 写设计文档（design-before-code hook 强制）
  → 涉及 Hessian → /hessian-orchestrate
  → 编码（gateguard hook 强制先调查）
  → mvn compile + test
  → 提交（说清楚仓库/分支/message）
  → session 结束时 HANDOFF.md 自动更新
```

---

## 第 0 步：从正确目录启动

```bash
# ✅ 正确：从目标子项目启动
cd ~/appstore/project/manager/rw-backend
claude

# ❌ 错误：从根目录启动
cd ~/appstore/project
claude
```

**原因**：`inject-session-context.sh` 根据 CWD 注入对应的 `project-contexts/<模块>.md`。根目录启动会注入全量 112 仓库概览，上下文太宽且会提示子目录建议（已浪费一次注入机会）。

---

## 第 1 步：第一条消息描述需求

直接说需求，无需交代背景，系统自动注入：

| 自动注入内容 | 来源 |
|------------|------|
| 上次工作状态 | `HANDOFF.md`（Stop hook 自动更新） |
| 模块架构概述 | `project-contexts/<模块>.md` |
| 常见坑标题 | `obsidian/meta/` 索引 |
| Resolver 提示 | `inject-session-context.sh` 关键词匹配 |

**Resolver 自动触发对照表**：

| Prompt 含有 | 系统注入 |
|------------|---------|
| 新功能 / 开发 / 需求 / 新增接口 | architect/planner agent 推荐 + 标准流程 |
| Hessian / 接口变更 / 接口新增 | `/hessian-orchestrate` skill 提示 |
| 修复 / 排查 / 报错 / bug | `/bug-audit` 流水线提示 |
| 设计方案 / 架构设计 | `/arch-docs` 索引提示 |

---

## 第 2 步：查有无已有方案

```
/arch-docs
```

列出所有现有设计文档指针，按需读取 1-2 个相关文档。避免重复设计已有方案。

---

## 第 3 步：写设计文档（Hook 强制）

首次 Write/Edit `.java` 文件时，`design-before-code.js` 会阻断：

```
[design-before-code] 本 session 尚无方案文档，即将写入业务代码：XxxService.java

选项：
  A) 先用 Write 创建 docs/设计文档.md，再来编辑代码
  B) 这是 bugfix / 小改动 → 说"跳过方案检查"
```

**操作**：
- **新功能**：让 Claude 先输出 `docs/YYYY-MM-DD-<功能名>.md`，写完后 hook 自动放行
- **纯 bugfix**：说「跳过方案检查」，本 session 不再拦截
- **涉及 Hessian**：先运行 `/hessian-orchestrate`，有独立的三侧同步检查清单

设计文档完成后，同步在 `/arch-docs` 索引中追加一行。

---

## 第 4 步：编码（GateGuard 强制先调查）

首次编辑**已有业务 Java 文件**时，`gateguard.js` 触发：

```
[GateGuard] 即将修改 XxxServiceImpl.java
请先提交调查报告：这个类现在做了什么？有哪些调用方？
```

**操作**：让 Claude grep + read 相关代码，确认类的职责 + 调用方 + 影响范围后再编辑。不可跳过。

> **跳过条件**（自动放行，无需调查）：
> - 测试类（`*Test.java`、`*IT.java`）
> - 纯数据类（`*DTO.java`、`*Entity.java`、`*VO.java`）
> - 配置/常量/异常类

---

## 第 5 步：验收

| 改动类型 | 命令 |
|---------|------|
| 后端代码变更 | `mvn clean compile` + `mvn test` |
| Hessian 接口变更 | `/hessian-orchestrate` Reviewer checklist 逐条过 |
| 跨模块改动 | 所有受影响子模块独立编译通过 |

---

## 第 6 步：提交

说清楚上下文，避免极短 prompt 在上下文断裂时误操作：

```
# ✅ 明确
"把 manager/rw-backend 今天的改动提交到 feature/xxx 分支，message: fix: 货柜流量卡同步逻辑"

# ❌ 容易出问题
"提交"（上下文丢失时可能猜错仓库/分支）
```

---

## 自动运行的后台机制

| Hook | 时机 | 作用 |
|------|------|------|
| `inject-session-context.sh` | 每条消息 | 注入上下文 + Resolver 路由 |
| `design-before-code.js` | 首次写 .java | 强制先出方案 |
| `gateguard.js` | 首次改已有 .java | 强制先调查 |
| `browse-redirect.js` | WebFetch 登录域名 | 重定向到 browse skill |
| `obsidian-daily-commits.py` | session 结束 | 提交记录写入日报 |
| `update-handoff.sh` | session 结束 | 自动更新 HANDOFF.md |

---

## 相关资源

- [[guide-cc-session-management]] — 会话管理与 /compact 时机
- [[guide-daily-task-log]] — 日报写入方式
- [[guide-worktree-vs-cmux]] — 多任务并行策略
- `~/.claude/skills/arch-docs/skill.md` — 架构文档索引
- `~/.claude/skills/hessian-orchestrate/skill.md` — Hessian 三侧同步

---

## 附：多项目相互依赖的任务

> **顶级目录不要打开**这条规则对多项目同样适用。按任务类型选模式。

### 模式 A：2-3 个仓库，当天能完成

从**改动最多的仓库**启动，其他仓库用绝对路径引用：

```bash
# Hessian 改动主要在 common/webservice
cd ~/appstore/project/common/webservice
claude
# 其他仓库说"去 ~/appstore/project/base/config 里改 XxxServiceImpl"
```

涉及 Hessian 三侧联动时加一步：说 `/hessian-orchestrate`，它会自动 grep 所有调用方并给出三侧同步检查清单。

### 模式 B：跨域多仓库，任务超过一天

用 **cmux + 同一 branch 名**，每个仓库建一个 worktree，一份 Obsidian 文档统管进度：

```bash
cd ~/appstore/project/common/webservice
cmux new feature-new-api -p "定义接口"

cd ~/appstore/project/base/config
cmux new feature-new-api -p "实现接口"

cd ~/appstore/project/manager/rw-backend
cmux new feature-new-api -p "调用接口"
```

每个仓库各自启动 Claude，通过同一份 Obsidian 任务文档协调进度。详见 [[guide-cmux-workflow]]。

### 模式 C：同域多仓库（如 base/ 下多个子项目）

从**域级目录**启动，`inject-session-context.sh` 能匹配到对应模块上下文：

```bash
# ✅ 域级目录可以接受（base、manager、lot 等）
cd ~/appstore/project/base
claude

# ❌ 不要去根目录（112 个仓库全量注入）
cd ~/appstore/project
claude
```

### 选择速查

| 情况 | 模式 |
|------|------|
| Hessian 三侧 + 当天完成 | 模式 A + `/hessian-orchestrate` |
| 跨域 + 超过一天 | 模式 B（cmux 多 worktree） |
| 同域多仓库 | 模式 C（域级目录启动） |
| 单仓库 | 标准流程，从子项目目录启动 |

---

## 附：分支确认规范

> 每个 session 首次写入代码时，`branch-guard` hook 自动触发，无需手动检查。

### Hook 行为

| 当前分支 | 行为 |
|---------|------|
| `main` / `master` | ⚠️ 软提醒，建议切分支，回复"确认分支"可继续 |
| 其他分支（feature/fix/hk 等） | 📌 软提醒，显示分支名，回复"确认分支"继续 |
| 同仓库第二次写入 | 直接放行，不重复打扰 |

### 标准分支命名

```bash
git checkout -b feature/<功能描述>   # 新功能
git checkout -b fix/<问题描述>       # bug 修复
git checkout -b refactor/<模块名>    # 重构
git checkout -b chore/<事项>         # 配置/依赖等
```

### 如果忘了切分支

hook 阻断后执行：

```bash
git checkout -b feature/<功能名>
# 然后重新触发写入操作
```

---
tags: [superpowers, claude-code, 工具链, 工作流, ai驱动开发]
date: 2026-04-23
project: 工具链
status: done
scope: cross-domain
generalized: false
---

# Superpowers 日常功能开发工作流

Superpowers 是一套强制 Claude Code 遵循有序开发流程的 skill 框架，核心价值是让 AI 先想清楚再动手，而不是随意跳入编码。

## 安装

```bash
# 已安装到 ~/.claude/skills/，无需重复操作
# 如需更新：
cd /tmp && git clone --depth=1 https://github.com/obra/superpowers.git sp_tmp
cp -r sp_tmp/skills/* ~/.claude/skills/
rm -rf sp_tmp
```

## 日常功能开发标准流程

```
需求来了
  │
  ▼
/brainstorming              ← 发散，澄清需求、边界、风险
  │
  ▼
/writing-plans              ← 拆解为 2-5 分钟粒度的可执行任务块
  │
  ▼
/using-git-worktrees        ← 新建 worktree 隔离开发环境（可选，长线任务必做）
  │
  ▼
/test-driven-development    ← 严格 RED → GREEN → REFACTOR 循环
  │
  ▼
/executing-plans            ← 按计划逐块执行，每块完成后标记
  │
  ▼
/requesting-code-review     ← 提交前自审或请 AI 审查
  │
  ▼
/verification-before-completion  ← 验证功能真实可用（禁止只靠声称完成）
  │
  ▼
/finishing-a-development-branch  ← 收尾：合并决策、清理 worktree
```

## 各 Skill 使用时机速查

| Skill | 何时用 | 关键输出 |
|-------|--------|---------|
| `/brainstorming` | 任何新任务开始前 | 澄清后的需求、风险点 |
| `/writing-plans` | brainstorm 结束后 | 可执行任务列表 |
| `/test-driven-development` | 写任何代码前 | 先写红测试，再实现 |
| `/systematic-debugging` | 遇到 bug 时 | 根因分析，不是乱猜 |
| `/executing-plans` | 有计划后执行时 | 按块完成并打勾 |
| `/dispatching-parallel-agents` | 任务可并行拆分时 | 多 agent 同时跑 |
| `/requesting-code-review` | 代码写完后 | 审查报告（按严重度） |
| `/receiving-code-review` | 收到审查意见后 | 逐条处理方案 |
| `/verification-before-completion` | 声称完成前 | 实际运行验证证据 |
| `/finishing-a-development-branch` | 准备合并时 | 合并 or 放弃决策 |
| `/subagent-driven-development` | 复杂功能需子任务并发 | 子 agent 任务分配 |

## 针对本项目的适配说明

### Spring Boot 功能开发

- `/brainstorming` 重点澄清：是否涉及 Hessian 接口（必须走 `hessian-orchestrate` skill）
- `/test-driven-development` 对应 JUnit5 + Mockito，集成测试不 Mock 数据库
- 编译验证：Hook 已自动执行 `mvn clean compile`，失败会阻断

### 什么情况跳过某些步骤

| 情况 | 可跳过 |
|------|--------|
| 简单 bugfix（<10行）| `/writing-plans`、`/using-git-worktrees` |
| 配置类修改 | `/test-driven-development`（无业务逻辑） |
| 紧急线上修复 | `/brainstorming` 可简化为 2 分钟口头确认 |

**不可跳过**：`/verification-before-completion`——任何改动上线前必须验证。

## 常见反模式（避免）

| 反模式 | 正确做法 |
|--------|---------|
| 让 Claude 直接写代码 | 先 `/brainstorming`，再让它写 |
| 看到报错直接让 Claude 修 | 先 `/systematic-debugging` 找根因 |
| 写完就说"完成了" | 必须 `/verification-before-completion` |
| 一个 PR 改多个无关功能 | 用 worktree 拆分，独立分支独立 PR |

## 相关链接

- [[guide-cc-worktree-parallel]] — worktree 并行开发详解
- [[guide-cc-long-task]] — 长线任务断点管理
- [[guide-cmux-workflow]] — cmux 会话管理配合使用

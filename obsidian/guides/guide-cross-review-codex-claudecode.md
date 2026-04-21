# Codex × Claude Code 互审方案

> 2026-04-20 | 两个 AI 互相审查对方出的方案，你是 tech lead，它们是给你打工的

## 核心思路

Codex（OpenAI）和 Claude Code（Anthropic）用不同模型、不同训练数据，对同一份方案会发现不同角度的问题。**两者分歧点往往是最有价值的地方。**

今天用这个机制审查了 `inject-session-context.sh` 的 Resolver 方案，Claude Code 自审发现了 4 个问题（误触发、重复噪音、python3 静默失效等），全部修复。

---

## 当前状态：半自动（人工传纸条）

Codex CLI 尚未安装，现阶段工作流：

```
Claude Code 产出方案
    ↓
把方案贴给 /codex（或手动给 codex exec）
    ↓
Codex 返回审查意见
    ↓
把意见贴回给 Claude Code
    ↓
Claude Code 回应/修改/反驳
    ↓
（可迭代 2-3 轮）
```

---

## 目标：全自动互审脚本

### 前置条件

| 条件 | 状态 |
|------|------|
| Claude Code login | ✅ 已有（走 OAuth） |
| Anthropic API Key | ❌ 不需要，用 `claude -p` |
| Codex CLI | ❌ 待安装 |
| OpenAI 账号 | 需要登录 codex |

安装步骤：
```bash
npm install -g @openai/codex
codex login
```

### 技术方案

两边都走 OAuth login session，无需任何 API Key：

```bash
SOLUTION=$(claude -p "$PROMPT")       # Claude Code headless 模式
REVIEW=$(codex exec "$SOLUTION ...")  # Codex CLI
```

### 脚本设计（待实现）

**文件**：`~/.claude/bin/cross-review.sh`

```
输入：任务描述 + 轮数（默认3）

Round 1:
  claude -p → 生成方案
  codex exec → 审查方案，输出问题列表

Round 2:
  claude -p（带上审查意见）→ 修改方案
  codex exec → 再次审查，看问题是否解决

Round N:
  输出最终方案 + 每轮分歧点汇总
```

**封装成 skill**：`~/.claude/skills/cross-review/skill.md`

调用方式：
```
/cross-review "任务描述" [轮数]
/cross-review "设计 Hessian 接口变更方案，新增 getUserList" 3
```

---

## 互审的局限

- **不能实时对话**：两个 AI 没有共享消息总线，你是中间人
- **谁是最终决策者**：你。两者分歧时不要让 AI 互相说服，自己判断
- **调用 claude -p 的风险**：从正在运行的 Claude Code session 内调用 `claude -p` 会起新进程，需测试是否有并发冲突
- **codex exec 的沙箱限制**：Codex 运行在 read-only 沙箱，无法访问 `~/.claude/` 目录，prompt 里的文件内容需要嵌入，不能给路径

---

## 实际效果（今天的案例）

| 审查者 | 发现的问题 |
|--------|----------|
| Claude Code（自审） | `fix`/`500` 误触发；Resolver 重复注入噪音；python3 静默失效；输出 JSON 无兜底 |
| Codex（如果已安装） | 可能会发现：shell 脚本 edge case、MARKER 文件竞态、grep 兜底的编码问题等 |

---

## TODO

- [ ] 安装 Codex CLI：`npm install -g @openai/codex && codex login`
- [ ] 实现 `~/.claude/bin/cross-review.sh`
- [ ] 封装为 `/cross-review` skill
- [ ] 测试 `claude -p` 在现有 session 内调用是否有冲突

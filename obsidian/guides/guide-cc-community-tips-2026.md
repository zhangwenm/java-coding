---
tags: [工具链, Claude-Code, 社区技巧]
date: 2026-04-28
updated: 2026-04-28
project: 工具链
status: done
scope: cross-domain
generalized: true
retrieval_triggers: [管道输入, piping, --max-turns, 无人值守, 两阶段session, 子目录CLAUDE.md, headless, json输出, xhigh, 社区技巧]
source: awesome-claude-code GitHub + HackerNews tips 汇总 + Reddit r/ClaudeCode
---

# Claude Code 社区技巧精选（2026-04）

**结论**：补充了本地已有笔记未覆盖的 6 个实用技巧，核心是管道输入和两阶段 session 拆分。

> 已有笔记覆盖：上下文管理 → [[guide-cc-reddit-efficient-patterns]]，session 切换 → [[guide-cc-session-management]]，worktree 并行 → [[guide-cc-worktree-parallel]]，长任务 → [[guide-cc-long-task]]。本笔记只记补充内容。

---

## 1. 管道输入（Piping）⭐ 立刻能用

把命令输出直接喂给 Claude，省去复制粘贴：

```bash
# Maven 编译报错直接修
mvn compile 2>&1 | claude -p "修这个错误"

# 测试失败直接分析
mvn test 2>&1 | claude -p "为什么测试失败？给出修复方案"

# PR review
gh pr diff 123 | claude -p "审查这个 PR，找出 bug 和安全问题"

# 生成 commit message
git diff --staged | claude -p "写一个 conventional commit message" --output-format text

# 日志分析
tail -500 /var/log/app.log | claude -p "总结错误，给出修复建议"
```

---

## 2. 两阶段 Session 拆分

与 [[guide-cc-long-task]] 的 [[handoff]] doc 模式互补，更激进：

```
Session 1（探索）：只读文件，只定位，输出 CONTEXT.md
Session 2（实现）：加载 CONTEXT.md，只改代码，不读不探索
```

**为什么有效**：探索阶段产生大量读取噪音，混在同一个 session 里会拉低 session 2 的输出质量。

**CONTEXT.md 模板提示词**：
```
帮我写一个 CONTEXT.md，内容包括：
1. 需要修改哪些文件（精确路径）
2. 修改方案（不要开始写代码）
3. 关键依赖和注意事项
写完停下，等我新开 session 再实现。
```

---

## 3. `--max-turns` 无人值守

```bash
# 修所有 lint 错误，最多执行 20 轮
claude -p "Fix all lint errors in src/" --max-turns 20

# 跑测试并修复失败，结果输出为 JSON
claude -p "Run tests and fix failures" --max-turns 15 --output-format json
```

适合：离开座位时挂着跑、CI 流水线、批量修复脚本。

---

## 4. 子目录 CLAUDE.md（按层级注入上下文）

Claude 在哪个目录工作，就自动读那一层的 CLAUDE.md，不需要手动 include。

```
~/appstore/project/
├── CLAUDE.md              ← 项目全局
├── iot-min/
│   └── CLAUDE.md          ← IoT 模块专用（数据源、Hessian 注意事项）
├── datacenter/
│   └── CLAUDE.md          ← 数仓专用（分区规则、ETL 约定）
└── lot/
    └── CLAUDE.md          ← lot 模块专用
```

**实操**：在每个业务域目录下放一个精简 CLAUDE.md，只写该域的踩坑和约定（20 行以内），不重复全局内容。

---

## 5. Headless JSON 模式（脚本集成）

```bash
# 结果可被 jq 解析，适合自动化
claude -p "找出 src/ 下所有 TODO 注释" \
  --output-format json | jq '.result'

# 流式输出，适合长任务实时看进度
claude -p "解释 SceneService.java 的逻辑" \
  --output-format stream-json
```

---

## 6. `/effort xhigh`（Opus 4.7 新增）

介于 `high` 和 `max` 之间，比 high 推理更深，比 max 便宜。

**适合场景**：
- 架构设计、方案评审
- Hessian 接口影响面分析
- 复杂 bug 根因分析

```
/effort xhigh
帮我分析这个 SceneService 改动会影响哪些调用方
```

---

## 速查表

| 场景 | 命令 |
|------|------|
| Maven 报错直接修 | `mvn compile 2>&1 \| claude -p "修这个错误"` |
| 无人值守批量修复 | `claude -p "..." --max-turns 20` |
| 大任务先探索再实现 | 两阶段 session + CONTEXT.md |
| 子模块上下文隔离 | 各业务域目录放 CLAUDE.md |
| 深度架构分析 | `/effort xhigh` |
| 脚本集成 | `--output-format json` |

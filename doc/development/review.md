# review

PR 预提交审查技能，分析 diff 查找结构问题。

## 功能描述

在代码合并到 main 前分析 diff，查找测试不捕获的结构问题。

## 工作流程

### Step 1: 检查分支

```bash
git branch --show-current
```

如在 main 上: "没什么可审查 - 你在 main 上或相对 main 没有变更。"

```bash
git fetch origin main --quiet && git diff origin/main --stat
```

如无 diff: 同样消息并停止。

### Step 2: 阅读检查清单

读取 `.claude/skills/review/checklist.md`。

如文件无法读取，**停止并报告错误。**

### Step 3: 检查 Greptile 审查评论

读取 `.claude/skills/review/greptile-triage.md` 并执行获取、过滤和分类步骤。

### Step 4: 获取 diff

```bash
git fetch origin main --quiet
git diff origin/main
```

### Step 5: 两轮审查

1. **Pass 1 (关键)**: SQL & 数据安全、LLM 输出信任边界
2. **Pass 2 (信息)**: 条件副作用、魔法数字、废弃代码、LLM 提示问题、测试缺口、视图/前端

### Step 6: 输出发现

**始终输出所有发现** - 关键和信息性。

- 如找到关键问题: 输出所有发现，然后对每个关键问题用单独的 AskUserQuestion
- 如只有非关键问题: 输出发现，无需进一步行动
- 如没问题: 输出 `Pre-Landing Review: No issues found.`

## Greptile 评论解决

### 存储分类

在 Step 2.5 存储分类（VALID & ACTIONABLE、VALID BUT ALREADY FIXED、FALSE POSITIVE、SUPPRESSED）。

### 输出发现

在输出头部包含 Greptile 摘要: `+ N Greptile comments (X valid, Y fixed, Z FP)`

### 处理类型

1. **VALID & ACTIONABLE**: 已包含在关键发现中 - 遵循相同 AskUserQuestion 流程
2. **FALSE POSITIVE**: 通过 AskUserQuestion 展示每个
3. **VALID BUT ALREADY FIXED**: 无需询问直接回复承认
4. **SUPPRESSED**: 静默跳过

## 重要规则

- **阅读完整 diff 前不评论** - 不要标记 diff 中已解决的问题
- **默认只读** - 除非用户明确选择"立即修复"，否则只修改文件
- **简洁** - 一行问题，一行修复，无前言
- **只标记真实问题** - 跳过任何正常的

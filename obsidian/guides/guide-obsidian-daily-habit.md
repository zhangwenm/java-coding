---
tags:
  - 工具链
  - Obsidian
  - 工作习惯
status: done
created: 2026-04-20
---

# Obsidian 日常记录习惯

> 本文档是 Agent 的行为指令：当用户说"记录到 obsidian"时，按此规范执行。

## 你说 → Agent 做

| 你说 | Agent 动作 |
|------|-----------|
| "记录到 obsidian" | 分析内容类型 → 选目录 → 建文档 → 更新 README 索引 |
| "记一下 xxx" | 同上 |
| "这个要记下来" | 同上 |

## 分类规则

| 内容类型 | 目录 | 文件名 | 举例 |
|---------|------|--------|------|
| 项目/系统架构分析 | `projects/` | `arch-项目名.md` | `arch-data-push.md` |
| 踩坑记录 | `projects/` | `pitfalls-什么坑.md` | `pitfalls-hessian-多项目发布.md` |
| 操作流程/指南 | `guides/` | `guide-做什么.md` | `guide-cmux-workflow.md` |
| 开发任务 | `tasks/` | `task-分支名.md` | `task-fix-robot-offline.md` |
| 具体操作步骤 | `guides/` | `howto-做什么.md` | `howto-配置多数据源.md` |
| 临时记录，还没整理 | `inbox/` | 随意命名 | `会议纪要-0420.md` |

## 文档必须包含

```markdown
---
tags:
  - 标签1
  - 标签2
status: done | in-progress | blocked
created: YYYY-MM-DD
---

# 标题

## 是什么
一句话说明这个文档记录什么。

## 正文
（自包含，打开就能用，不需要猜上下文）
```

## Agent 操作流程

当用户触发"记录到 obsidian"时：

1. **判断内容类型** → 从上表选目录和前缀
2. **创建文档** → 按模板写入，frontmatter 必填
3. **更新 README.md** → 在对应分类下加一行索引
4. **确认** → 告诉用户创建了什么文件

### 不确定时

内容跨多个分类 → 放最相关的那个，加 tag 辅助检索。

### 禁止

- ❌ 不建空文档
- ❌ 不把大段未整理内容直接塞进 inbox（至少写好标题和"是什么"）
- ❌ 不修改已有文档的内容（除非用户明确要求更新）

## 你自己手动记的时候

1. 放对应目录，不确定就放 `inbox/`
2. 文件名用英文，加前缀（`arch-`/`guide-`/`pitfalls-`）
3. 写好 frontmatter 的 tags 和 status
4. 抽空在 README.md 加一行索引
5. 每周清理一次 `inbox/`，有价值的移走，没用的删

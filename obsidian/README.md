# Knowledge Base

> 这个知识库遵循 **LLM Wiki** 原则（inspired by Karpathy）：
> 每篇笔记都是给 LLM 的上下文单元，拿到就能用，不需要额外解释。
> 结构扁平、自描述、frontmatter 可检索。

## 目录结构

```
obsidian/
├── README.md          ← 你在这里，知识库索引
├── projects/          ← 项目级知识：架构、踩坑、设计决策
│   ├── arch-*.md        架构文档
│   └── pitfalls-*.md    踩坑记录
├── guides/            ← 操作指南：工具链、工作流、配置
├── tasks/             ← 开发任务文档（cmux worktree 绑定）
├── inbox/             ← 未整理碎片，定期归档
├── templates/         ← 笔记模板
└── .obsidian/         ← Obsidian 配置（不跟踪）
```

## 原则

1. **LLM-first**：每篇笔记是独立上下文，包含足够背景让任何 LLM 直接使用
2. **扁平优于嵌套**：最多 2 层目录，用文件名前缀分类（`arch-`、`pitfalls-`、`guide-`）
3. **frontmatter 可检索**：tags、status、date、project 等字段保持一致
4. **原子笔记**：一篇笔记讲清一件事，不超过 300 行
5. **双向链接**：用 `[[]]` 关联相关笔记，但每篇笔记独立可读
6. **模板驱动**：新建笔记必须用模板，保证格式一致

## 文件命名规范

| 前缀 | 含义 | 示例 |
|------|------|------|
| `arch-` | 项目架构 | `arch-data-push.md` |
| `pitfalls-` | 踩坑记录 | `pitfalls-datacenter.md` |
| `guide-` | 操作指南 | `guide-cmux-workflow.md` |
| `task-` | 任务文档 | `task-fix-robot-offline.md` |
| `howto-` | 具体操作步骤 | `howto-setup-browser-cookies.md` |

## 现有笔记索引

### 项目架构

| 文件 | 项目 | 描述 |
|------|------|------|
| [[projects/arch-data-push]] | datacenter/data-push | 数据推送服务架构，Kafka→队列→多合作方推送 |
| [[projects/arch-cmux-multi-agent]] | 工具链 | cmux 多 Agent 并行开发模式 |

### 操作指南

| 文件 | 工具 | 描述 |
|------|------|------|
| [[guides/guide-cmux-workflow]] | cmux + Obsidian | 长线任务完整工作流（含 Sisyphus 自动化） |
| [[guides/guide-cc-long-task]] | Claude Code | 长线任务操作指南 |
| [[inbox/cc-oc-快捷启动]] | cc / oc | 快捷启动命令速查 |

## 写作规范

每篇笔记的 frontmatter 至少包含：

```yaml
---
tags: [相关标签]
date: YYYY-MM-DD
project: 项目名（如 datacenter）
status: draft / in-progress / done
---
```

正文结构：
1. **一句话摘要**（第一段）
2. **正文**（按需分节）
3. **相关链接**（底部，指向关联笔记或外部资源）

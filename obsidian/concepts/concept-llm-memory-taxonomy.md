---
tags: [架构思想, LLM, 知识库设计]
date: 2026-04-23
project: 工具链
status: done
scope: cross-domain
generalized: false
retrieval_triggers: [obsidian设计, 知识库优化, 记忆架构, LLM外部记忆, 目录规划]
---

# Obsidian 作为 LLM 外部记忆的设计原则

外部记忆（External Storage）的瓶颈不是存了多少，而是检索时能精准捞到正确的块。这篇记录用 Karpathy 的记忆分类框架来解释为什么这个知识库这样设计。

## Karpathy 四类记忆 → 知识库目录映射

| 记忆类型 | LLM 系统中的角色 | 本知识库对应 |
|---------|----------------|------------|
| In-weights | 训练进参数，永久但更新贵 | Claude 自带的通用知识（不需要记） |
| In-context | 上下文窗口，快但有限（=RAM） | HANDOFF.md + 当次对话 |
| In-cache | KV 缓存，省重复计算 | 不适用 |
| **External** | **磁盘，无限但需检索** | **此 Obsidian vault** |

各目录承担的认知功能：

| 目录 | 认知类型 | 逻辑 |
|------|---------|------|
| `meta/` | 语义记忆 | Claude 会幻觉的跨域规律，存到 weights 但不稳定 → 显式存外部 |
| `guides/` | 程序性记忆 | 操作步骤，weights 没有项目特定版本 |
| `projects/` | 情节记忆 | 历史决策和踩坑，weights 里没有 |
| `tasks/` | 工作记忆 | 活跃状态快照，weights 和 context 都没有持久化 |
| `concepts/` | 框架记忆 | 思维框架，帮助 Claude 在新任务里选对模型 |

## 三条设计原则

### 原则一：检索路由优先

笔记要告诉系统"在什么场景下加载我"。frontmatter 的 `retrieval_triggers` 字段是路由 key，配合 `inject-session-context.sh` 实现关键词触发自动注入。

没有 triggers 的笔记只能靠用户手动提 → 相当于磁盘上没有路径索引。

### 原则二：上下文预算意识

每篇笔记被加载都消耗 context window（RAM）。写笔记时默认目标是：Claude 在前 200 tokens 内判断是否需要继续读。

- 结论必须在第一段
- 表格 > 段落（信息密度高）
- 单篇 < 300 行

### 原则三：防止上下文污染

过期笔记比"没有笔记"更危险——它提供了错误信息且有置信度。`status: stale` 是关键卫生机制。代码重构或工具升级后，主动检查相关笔记是否需要标 stale。

## 相关链接

- [[purpose]] — 知识库整体规则和目录说明
- [[guides/guide-thin-harness-fat-skills]] — 薄缰绳厚技能的落地实践（程序性记忆的样本）
- [[meta/meta-distributed-contract]] — 分布式契约规则（语义记忆的样本）

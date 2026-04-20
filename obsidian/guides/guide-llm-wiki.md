---
tags: [tools, obsidian, llm, batch-processing]
date: 2026-04-20
project: workflow
status: active
---

# llm_wiki 使用指南

**结论**：llm_wiki 只在有大量历史文档积压（PDF/DOCX/网页）时才有价值，日常单条笔记用 CC + Obsidian 质量更高。

## 适用场景 vs CC+Obsidian

| 场景 | 推荐工具 |
|------|---------|
| 零散笔记、有上下文 | CC + Obsidian（质量高，有交叉链接 Hook） |
| 大量历史文档批量消化 | llm_wiki（一次拖入50个文件自动跑） |

## 安装

下载 macOS ARM 版（v0.3.3）：
```
https://github.com/nashsu/llm_wiki/releases/download/v0.3.3/LLM.Wiki_0.3.3_aarch64.dmg
```
拖入 Applications，首次打开在「系统设置 → 隐私与安全」允许运行。

## 配置

1. **LLM**：填入 Claude API Key，模型选 claude-haiku-4-5（够用且便宜）
2. **Wiki 目录**：指向 `~/appstore/project/java-coding/obsidian`（直接融入现有知识库）
3. **System Prompt**（让笔记风格与知识库一致）：

```
你是知识库整理助手。写笔记时：
1. 结论前置，第一句就是核心结论
2. frontmatter 必填：tags、date、project、status
3. 只记结论和依据，不记过程
4. 踩坑格式：根因 + 修复方案 + 预防措施
```

## 使用流程

1. 收集待处理文档（PDF/DOCX/TXT/URL 列表）
2. 打开 llm_wiki，批量拖入
3. 等待处理完成，笔记自动写入 obsidian vault
4. 在 CC 会话里执行 git commit 推送（PostToolUse Hook 不会触发，需手动补交叉链接）

## 适合批量处理的文档类型

- 飞书文档/wiki 导出的 PDF
- 历史踩坑记录（Word 文档）
- 技术博客/GitHub [[README]]（URL 批量导入）
- 论文或规范文档

## 局限

- 无法感知现有笔记的上下文，不会自动补充已有条目
- 不触发 PostToolUse Hook，`[[交叉链接]]`需手动检查
- 笔记质量受 system prompt 约束，不如 CC 对话式精准

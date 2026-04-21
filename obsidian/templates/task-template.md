---
tags:
  - 任务
status: draft        # draft / in-progress / blocked / done / cancelled
priority: medium     # high / medium / low
assignee: agent
created: {{date}}    # Obsidian 模板引擎自动填；Claude Code 直接写文件时需手动填日期
updated: {{date}}    # 同上
deadline:
branch:              # cmux worktree 分支名，如 fix-robot-offline；多仓库并行时填同一个名
---

<!-- 本模板适用于 OpenCode 和 Claude Code 两套工作流，差异见各自的 guide -->
<!-- OpenCode：guide-cmux-workflow | Claude Code：guide-cc-long-task -->

# {{title}}

## 目标

<!-- 一句话说清楚这个任务要达成什么 -->

## 背景

<!-- 为什么做、关联的需求或问题 -->

## 范围

### 包含
-

### 不包含
-

## 里程碑

| 里程碑 | 目标日期 | 实际完成 |
|--------|---------|---------|
| | | |

## 执行计划

### 阶段一：调研
- [ ]

### 阶段二：实现
- [ ]

### 阶段三：验证
- [ ]

## Feature 清单

<!-- 复杂任务用此表跟踪进度，cmux start 前先确认 current -->
<!-- status: pending / in-progress / done -->

| ID | Feature | 验收标准 | Status | Commit |
|----|---------|---------|--------|--------|
| F001 | | | pending | |

**Current**: F001

## 当前状态快照

<!-- 每次会话结束前必须更新，作为下次启动的恢复入口 -->
- 最后更新：{{date}}
- 当前进展：
- 下次启动入口：`cd <项目路径> && cmux start <branch>`
- 待续位置：<!-- 具体到文件/方法/步骤 -->

## 阻塞项

| 阻塞内容 | 等待对象 | 记录时间 | 解除时间 |
|---------|---------|---------|---------|
| | | | |

## 关键决策记录

<!-- 记录任务过程中做过的重要技术决策，避免下次被问到时说不清楚为什么这么设计 -->
<!-- 例如：选用 A 方案而非 B 方案的原因、放弃某个实现路径的原因 -->

| 决策 | 选项 | 结论 | 原因 |
|---|---|---|---|
| | | | |

## 涉及文件

-

## 验收标准

- [ ]

## 备注

<!-- 踩坑记录、依赖说明、回滚方案等 -->

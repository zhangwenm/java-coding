---
tags:
  - 任务
  - 算法
status: done
priority: medium
assignee: agent
created: 2026-04-21
updated: 2026-04-21
deadline:
branch: feature-bubble-sort
---

# 新建冒泡排序（BubbleSort）

## 目标

在 algorithms 模块新建 BubbleSort（提前终止优化版）。

## 当前状态快照

- 最后更新：2026-04-21
- 当前进展：BubbleSort + 测试已实现，9/9 测试通过
- 下次启动入口：`cd ~/appstore/project/java-coding && source ~/.cmux/cmux.sh && cmux start feature-bubble-sort`
- 待续位置：代码已就绪，可 commit 或 merge

## 涉及文件

- `java-coding-algorithms/src/main/java/com/geekzhang/algorithms/niuke/BubbleSort.java`（新建）
- `java-coding-algorithms/src/test/java/com/geekzhang/algorithms/niuke/BubbleSortTest.java`（新建）
- `java-coding-algorithms/pom.xml`（加 JUnit 5）

## 验收标准

- [x] BubbleSort 提前终止优化，注释清晰
- [x] 单元测试 9/9 通过
- [x] `mvn clean compile` 通过

## Feature 清单

| ID | Feature | 验收标准 | Status | Commit |
|----|---------|---------|--------|--------|
| F001 | BubbleSort 实现 + 测试 | 提前终止优化，9/9 tests pass | ✅ done | da456fb |

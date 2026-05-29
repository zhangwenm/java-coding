---
tags: [工具链, Claude-Code, 新特性, 效率]
date: 2026-05-08
updated: 2026-05-08
project: 工具链
status: done
scope: cross-domain
generalized: true
retrieval_triggers: [ultrareview, xhigh, effort, team-onboarding, PermissionDenied, less-permission-prompts, resume加速, PostToolUse输出替换, 新特性, 2026特性, 并行review, Hessian review, 跨模块PR]
source: Claude Code Changelog 2025-04 ~ 2026-05 + 个人工作场景分析
---

# Claude Code 新特性使用指南（2026-05）

**结论**：对当前 Java/IoT 工作最直接有用的是 `/ultrareview`（Hessian + 跨模块 PR）和 `xhigh effort`（复杂 bug 排查 + 架构设计）。其余特性已配置为被动生效。

> 相关笔记：hooks 配置 → 见 settings.json，工作流全貌 → [[guide-ecc-workflow]]，session 管理 → [[guide-cc-session-management]]

---

## 1. `/ultrareview` — 并行多 agent 代码审查

### 是什么

启动多个并行 agent，从不同角度同时审查当前分支 vs main 的全部变更，最后汇总报告。比单次 `/review` 覆盖更全，特别适合改动面广的 PR。

**注意**：按用量计费，别用在小改动上。

### 场景一：Hessian 接口变更后 review

Hessian 三侧同步（common-webservice + base + 调用方）靠人工 checklist 容易漏，`/ultrareview` 可以并行检查每一侧。

```bash
# 三侧都改完，提 PR 前
/ultrareview

# 针对已有 GitHub PR
/ultrareview 142
```

ultrareview 会自动检查：
- 接口签名两侧是否一致
- 调用方是否有未更新的旧签名
- 序列化字段兼容性
- 是否遗漏了某个调用方模块

### 场景二：跨模块改动（datacenter / iot-min / manager）

```bash
# 在改动最多的模块目录下执行
cd ~/appstore/project/datacenter/data-push
/ultrareview

# 或指定 PR 号（需要 GitHub remote）
/ultrareview 88
```

### 场景三：CI 脚本中非交互式执行

```bash
claude ultrareview --output-format json > review-result.json
```

### 触发时机建议

| 场景 | 触发时机 |
|------|---------|
| Hessian 接口变更 | 三侧改完、`/finishing-a-development-branch` 之前 |
| 跨模块 PR | push 之前 |
| 大型重构 | 每个阶段收尾时 |
| 小改动（单文件 bugfix）| **不触发**，成本不值 |

---

## 2. `xhigh effort` — 最大推理深度

### 是什么

Opus 4.7 专属的新努力档位，比 `high` 还高一级，给模型更多 token 预算做内部推理。适合需要深层因果分析的任务。

### 用法

```bash
# 交互式滑块选择
/effort

# 直接设定（high / xhigh）
/effort xhigh

# 本次会话生效，下次会话重置
```

### 场景一：复杂 Bug 排查

遇到"wtf 级"bug（跨模块、并发、数据不一致）时，先设 xhigh 再触发调查：

```bash
/effort xhigh
/investigate
# 描述现象：设备上报数据丢失，日志无报错，偶发，高并发时更明显
```

对比不用 xhigh 的区别：模型会主动推断更多可能路径（线程安全、连接池、序列化边界），而不是只看表面的 NPE 堆栈。

### 场景二：架构设计会话

做新模块架构设计时，开局设 xhigh，让模型在给出方案前充分权衡各种设计选择：

```bash
/effort xhigh
# 然后描述需求，让 Claude 先输出设计文档再写代码
# 对应 CLAUDE.md 中"设计/架构任务：先产出设计文档，不写代码"原则
```

### 什么时候不用 xhigh

- 简单的单文件 bugfix → 默认 effort 够用
- 格式转换、代码生成、重复性任务 → Haiku 就够，别用 Opus
- 会话初期探索阶段 → 先用默认，确认方向后再升

---

## 3. `/team-onboarding` — 自动生成团队上手指南

### 是什么

从本地历史对话中提取你的 Claude Code 使用模式（skills、hooks、工作流），自动生成适合新人阅读的快速上手文档。

### 用法

```bash
/team-onboarding
# 生成后把内容粘贴到飞书 wiki
```

### 适合什么时候跑

- 新同事入职，需要交接工具链时
- 整理一遍自己的工具链做复盘时（`/retro` 的补充）
- 工具链大改动后更新团队文档时

### 预期产出

生成内容通常包含：
- 常用 skill 列表 + 触发场景
- hooks 说明（compile-java / check-dangerous 等的用途）
- 项目上下文注入机制说明
- 推荐的日常工作流

---

## 4. `PermissionDenied` hook + `/less-permission-prompts` — 被动减少弹窗

### 当前配置状态

已配置（2026-05-08），日志写入 `~/.claude/metrics/permission-denied.jsonl`，每累积 5 条提示运行分析。

### 使用流程

1. 正常工作 1-2 周，不用手动做任何事
2. 日志积累到一定量后（hook 会提示），运行：
   ```bash
   /less-permission-prompts
   ```
3. 工具会分析日志，建议把哪些工具调用加入 allowlist
4. 确认后自动写入 `settings.json`

### 查看当前积累情况

```bash
wc -l ~/.claude/metrics/permission-denied.jsonl
cat ~/.claude/metrics/permission-denied.jsonl | python3 -c "
import sys, json, collections
tools = collections.Counter()
for line in sys.stdin:
    try: tools[json.loads(line)['tool']] += 1
    except: pass
for t, c in tools.most_common(10):
    print(f'{c:3d}  {t}')
"
```

---

## 5. `/resume` 大会话加速（被动生效）

### 是什么

大会话（40MB+）的恢复速度提升了 67%，无需任何配置，自动生效。

### 对当前工作的影响

- 复杂架构设计会话中断后继续，等待时间大幅缩短
- 多模块 bug 排查积累了大量上下文后恢复不再慢
- 可以更放心地做大型单会话，不用担心恢复成本

### 主动用好的方式

配合 `PreCompact` hook（已配置）+ `/context-save`：
- 长会话到一半时手动 `/context-save` 保存进度
- 中断后 `/context-restore` 或直接 `/resume` 恢复
- 现在恢复比以前快，这个模式的摩擦成本更低了

---

## 6. `PostToolUse` 输出替换 — 增强 Maven 错误反馈（可选扩展）

### 是什么

新特性允许 PostToolUse hook 覆盖工具的输出结果，Claude 看到的是 hook 处理后的版本，而非原始输出。

### 潜在用法：结构化 Maven 错误

```bash
# 当前 compile-java.sh 在 Stop hook 执行 mvn compile
# 可以新增一个 PostToolUse hook，在 Bash 工具执行 mvn 后，
# 把原始编译错误转换为结构化格式再返回给 Claude：
```

```json
{
  "matcher": "Bash",
  "hooks": [{
    "type": "command",
    "command": "~/.claude/hooks/format-mvn-error.sh"
  }]
}
```

`format-mvn-error.sh` 解析 stdout 中的 `[ERROR]` 行，输出：
```
受影响模块: iot-min/device-service
错误类型: 符号找不到
位置: DeviceService.java:142
建议: 检查 HessianBean 是否同步更新
```

**当前优先级**：低，现有流程够用，记录备用。

---

## 速查卡

| 场景 | 命令 | 频率 |
|------|------|------|
| Hessian PR 前 | `/ultrareview` | 每次 Hessian 变更 |
| 跨模块 PR 前 | `/ultrareview` | 每次跨模块 PR |
| 复杂 bug 排查 | `/effort xhigh` → `/investigate` | 按需 |
| 架构设计开局 | `/effort xhigh` | 按需 |
| 新人入职/工具链复盘 | `/team-onboarding` | 不定期 |
| 减少权限弹窗 | 积累后 `/less-permission-prompts` | 每月一次 |
| 大会话恢复 | `/resume`（自动加速） | 被动生效 |

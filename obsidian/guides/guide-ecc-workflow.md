---
tags: [工具链, Claude-Code, ECC, AI驱动开发, 效率]
date: 2026-04-23
project: 工具链
status: done
retrieval_triggers: [ECC, agent速查, skill速查, 日常开发]
scope: cross-domain
generalized: false
---

# Everything Claude Code (ECC) 日常开发提效指南

ECC 的核心价值是把"直接问 Claude"替换为"指定专职 agent 干活"，每个 agent 有专属上下文，输出更聚焦，减少废话和反复纠正。

## 安装现状

- **版本**：v1.10.0（developer profile，跳过 hooks-runtime）
- **安装路径**：`~/.claude/`（agents / skills / commands / rules）
- **已装 agents**：38 个；**已装 skills**：156 个
- **新增 hooks**：evaluate-session、cost-tracker、desktop-notify、config-protection

---

## 日常开发场景速查

### 场景一：写新功能

```
需求 → /brainstorming → /writing-plans → 用 java-reviewer agent 检查
```

具体：
```
用 architect agent 设计设备心跳超时检测方案，考虑高并发场景
```
```
用 planner agent 把这个需求拆分成可并行的子任务
```

### 场景二：修 Bug

遵循 `bug-audit` skill 流水线，**不要逐个发现逐个修**：
```
/bug-audit 先全量扫描 TaskFlowEngine 里的所有潜在问题
```
然后：
```
用 java-build-resolver agent 修这个编译错误：[粘贴错误信息]
```

### 场景三：Code Review（提交前必跑）

```
/code-review
```
或更细粒度：
```
用 java-reviewer agent 审查 src/main/java/com/xxx/controller/DeviceController.java
```
安全重点时：
```
用 security-reviewer agent 检查 AuthController 的权限校验逻辑
```

### 场景四：架构设计

**原则：先产出设计文档，不写代码，等审批**

```
用 architect agent 设计 IoT 设备影子同步方案
要求：考虑 Hessian RPC 三端同步约束，high availability，给出决策对比表
```

### 场景五：TDD 开发

```
/tdd 实现设备状态变更通知逻辑
```
流程强制变成：**先写失败测试 → 再实现 → 测试通过**，对应 CLAUDE.md"任务目标化"原则。

### 场景六：多模块并行（大功能）

```
/multi-plan 重构 iot-min 设备状态同步模块，识别可并行的子任务
```
确认拆分方案后：
```
/multi-execute
```
多个 worktree 同时跑，结束后合并。配合 [[guide-cc-worktree-parallel]] 使用。

---

## Agent 速查表

| 场景 | 使用的 agent | 调用方式 |
|------|-------------|---------|
| 系统设计/架构 | `architect` | `用 architect agent 设计 XXX` |
| Java 代码审查 | `java-reviewer` | `用 java-reviewer agent 审查 XXX.java` |
| 编译错误修复 | `java-build-resolver` | `用 java-build-resolver agent 修这个错误` |
| 安全漏洞检查 | `security-reviewer` | `用 security-reviewer agent 检查 XXX` |
| TDD 指导 | `tdd-guide` | `用 tdd-guide agent 指导写 XXX 的测试` |
| 任务拆解 | `planner` | `用 planner agent 拆分这个需求` |
| 代码重构 | `refactor-cleaner` | `用 refactor-cleaner agent 清理 XXX` |
| 文档更新 | `doc-updater` | `用 doc-updater agent 更新 XXX 的注释` |
| 代码通读 | `code-explorer` | `用 code-explorer agent 梳理 XXX 模块结构` |
| 多 agent 协调 | `chief-of-staff` | `用 chief-of-staff agent 协调这个任务` |

---

## Skill 速查表（/skill-name 调用）

| Skill | 调用命令 | 适用场景 |
|-------|---------|---------|
| Spring Boot 模式 | `/springboot-patterns` | 遇到 Spring 相关设计问题时 |
| JPA/数据库 | `/jpa-patterns` | 查 JPA 最佳实践 |
| 数据库迁移 | `/database-migrations` | 写 Flyway/Liquibase 脚本前 |
| TDD 工作流 | `/tdd-workflow` | 严格 Red-Green-Refactor |
| 验证循环 | `/verification-loop` | 确保改动前后测试全通 |
| 安全审查 | `/security-review` | OWASP 漏洞自查 |
| 深度调研 | `/deep-research` | 技术选型前调研 |
| 持续学习状态 | `/instinct-status` | 查看自动提取的模式 |
| 代码审查 | `/code-review` | 提交前 |
| Spring Boot 验证 | `/springboot-verification` | 确认改动不破坏现有行为 |
| API 设计 | `/api-design` | 设计 REST 接口 |
| 后端模式 | `/backend-patterns` | 通用后端设计参考 |

---

## 持续学习：自动沉淀经验

每次会话结束，`evaluate-session.js` 自动扫描 transcript，提取高置信度模式。

**查看已提取的模式：**
```
/instinct-status
```

**导入为正式 skill：**
```
/instinct-import
```
确认内容后，该模式在后续会话自动生效。

**手动触发提取（会话中途）：**
```
/continuous-learning-v2
```

学到的 skill 存放在 `~/.claude/skills/learned/`，可手动查阅：
```bash
ls ~/.claude/skills/learned/
```

---

## Token 用量查看

每次 Stop 后 `cost-tracker.js` 自动记录，查看历史：
```bash
cat ~/.claude/metrics/costs.jsonl | tail -20 | python3 -m json.tool
```

输出字段：`model`、`inputTokens`、`outputTokens`、`estimatedCost`（USD）。

---

## 新增 Hooks 行为说明

| Hook | 触发时机 | 行为 |
|------|----------|------|
| `evaluate-session.js` | 每次 Stop | 从会话提取可复用模式 |
| `cost-tracker.js` | 每次 Stop | 记录 token 用量到 costs.jsonl |
| `desktop-notify.js` | 每次 Stop | macOS 通知栏弹出任务摘要 |
| `config-protection.js` | Edit/Write 前 | 拦截对 linter 配置文件的修改 |
| `gateguard.js` | Edit/Write 前（.java 业务类）| 首次编辑强制调查四项（importers/公共方法/依赖 Bean/原始指令），调查后重试放行；跳过测试类/DTO/Entity |
| `maven-protection.js` | Edit/Write 前（pom.xml）| 拦截 `<profiles>`/`<build>`/`skipTests`；仅加依赖则放行 |
| `obsidian-session-end.sh` | 每次 Stop | 检测到代码改动时提醒 `/obsidian-note` |

## 与已有工作流的衔接

| 已有工作流 | ECC 增强点 |
|-----------|-----------|
| [[guide-superpowers-workflow]] | `/brainstorming` 后可接 `architect agent` 做更深的设计 |
| [[guide-cc-worktree-parallel]] | `/multi-plan` + `/multi-execute` 替代手动 worktree 分配 |
| [[guide-cc-session-management]] | `evaluate-session` 自动在会话结束后提取经验，减少手动记录 |
| `hessian-orchestrate` skill | `architect agent` 设计时会主动提示三端同步约束 |
| `bug-audit` skill | `/code-review` 作为提交前最后一关 |

---

## 日常使用最小路径（从这里开始）

**第一周只用这三个，其他按需扩展：**

```
1. 代码审查前：/code-review
2. 遇到架构问题：用 architect agent 设计 XXX
3. 遇到 Java 编译错误：用 java-build-resolver agent 修 [错误信息]
```

---

## 相关链接

- [[guide-superpowers-workflow]]
- [[guide-cc-worktree-parallel]]
- [[guide-cc-session-management]]
- ECC 仓库：https://github.com/affaan-m/everything-claude-code

---

## 2026-05-26 ECC 深度分析与配置优化

分析来源：`github.com/affaan-m/ECC`（193K stars，Anthropic Hackathon Winner），提取对 Java/IoT 项目最有价值的工程技巧并落地。

### 核心结论

GateGuard 是本次最高价值改进：A/B 实验显示有无 GateGuard 代码质量相差 2.25 分（9.0 vs 6.75）。根因是强制调查本身创造了上下文，而非简单的"你确定吗"（模型对此永远回答"是"）。

### 落地的四项改进

| 优先级 | 改动 | 文件 | 说明 |
|--------|------|------|------|
| P1 | GateGuard hook | `hooks/gateguard.js` | 首次编辑 Java 业务类强制提交四项调查报告 |
| P2 | Maven 保护 | `hooks/maven-protection.js` | 阻断 pom.xml build/profiles 逃跑，放行加依赖 |
| P3 | Skill 路由补充 | `CLAUDE.md` | topic 链路 → `/code-explorer`；方案评估 → `/plan-eng-review` |
| P4 | 手动压缩时机 | `CLAUDE.md` | 完成功能模块后手动 `/compact`，防止上下文在中途截断 |

（config-protection / 批量 Stop 编译已提前实现，本次确认现状后跳过。）

### GateGuard 四项强制调查内容

```
1. 谁调用了这个类？（Grep import 和引用）
2. 公共方法签名是什么？（Read 文件）
3. 核心依赖 Bean 是什么？（@Autowired / 构造注入）
4. 用户的原始指令是什么？（原文引用）
```

调查完成后重新发起编辑即通过（文件已加入 session 放行列表）。

### ECC 未落地但值得后续参考

- **continuous-learning-v2**：Hook 触发率 100% vs skill 触发率 50-80%，项目级 instinct 隔离防止跨项目污染
- **De-Sloppify 模式**：两个专注 agent（实现 + 清理）优于一个受约束 agent
- **RFC-Driven DAG**：复杂需求拆成依赖图，按层并行执行，每单元独立 worktree

### 相关链接

- [[guide-cc-session-management]]
- [[guide-superpowers-workflow]]
- ECC 仓库：https://github.com/affaan-m/ECC

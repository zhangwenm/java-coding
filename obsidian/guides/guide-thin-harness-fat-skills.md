---
tags: [工具链, 架构思想, Claude-Code, OpenCode]
date: 2026-04-20
project: 工具链
status: done
source: https://weibo.com/5078115336/QBw11mTId（Garry Tan / YC thin harness fat skills）
---

# 薄缰绳厚技能：落地改进实录

**结论**：AI 编程效率差异在架构而非模型。Harness 越薄越好，逻辑越多放进 Skill。我们在 OpenCode 和 Claude Code 两套工具都做了对应改进。

> 理论来源：Garry Tan 长文，五个概念 Skill/Harness/Resolver/潜在空间/Diarization，详见原链接。

---

## OpenCode 改进（AGENTS.md 体系）

### 改进一：铁律「不允许做一次性的工作」

**文件**：`AGENTS.md`

```markdown
## 铁律：不允许做一次性的工作

1. 先做 3-10 个样本，给用户确认
2. 认可后立刻提炼成 skill 文件
3. 应自动运行的挂到定时任务或 Resolver 路由

检验标准：用户要同一个东西第二次 = AI 失败
```

### 改进二：Resolver 路由规则

**文件**：`AGENTS.md`

#### 代码修改时自动加载

| 触发条件 | 自动加载 |
|---------|---------|
| 改 Mapper / XML / DAO | 多数据源 `@DataSourceKey` 注意事项 |
| 改 `common/webservice` 接口 | `hessian-orchestrate` skill |
| 改 `@Scheduled` 定时任务 | 分布式锁配置 + 踩坑记录 |
| 改前端组件 | 先确认 UI 库（Element UI / TDesign / Arco / Quasar） |
| 改 `base/` 下的 Impl 类 | grep 检查调用方是否需同步 |
| 新建 Java Service 类 | `entity/dto` → `service/impl` → `web/api` 分层 |

#### 问题排查时自动加载

| 触发条件 | 自动加载 |
|---------|---------|
| Hessian 404 / 超时 / 序列化异常 | Gotcha 章节 |
| 多数据源数据查不到 / 写错库 | `@DataSourceKey` 注意事项 |
| Maven 编译失败（找不到符号） | 检查 common/webservice 依赖 |

### 改进三：参数化元流程 skill — `cross-project-change`

同一个七步流程传入不同参数产生不同能力（接口变更/DTO变更/配置变更/数据库变更）。
文件：`.claude/skills/cross-project-change/SKILL.md`

### 改进四：项目档案化 skill — `project-profile`

接受目标项目参数，输出标准化一页画像：技术栈、依赖、健康度、风险点、已知坑位。
文件：`.claude/skills/project-profile/SKILL.md`

---

## Claude Code 改进（.claude/ 体系）

### 改进一：Resolver 路由（inject-session-context.sh）

每条消息扫描关键词，命中则注入专项提示：

| 关键词 | 注入内容 |
|--------|---------|
| `hessian / 接口变更 / HessianProxy` | 提醒三侧同步，指向 `hessian-orchestrate` skill |
| `数据源 / DataSourceKey` | 确认 `@DataSourceKey` 注解正确 |
| `生产库 / 线上 / prod.*db` | 二次确认后执行，告知回滚方案 |
| `设计方案 / 架构设计 / 设计文档` | 先查 `arch-docs` 索引再动手 |
| `bug / 修复 / 报错 / NPE / 500` | 遵循 `bug-audit` 流水线 |

### 改进二：铁律「不做一次性工作」

文件：`~/CLAUDE.md`（工作方式约定章节）。凡是会重复的事，先做 3~10 个样本确认，再固化成 skill。

### 改进三：档案化 Skill — `domain-diarization`

`/domain-diarization <domain>` 读上下文+架构文档+踩坑+git log，输出 7 节画像，重点是「说的」vs「做的」差距。

---

## Skill 体系关系图

```
domain-diarization / project-profile（域/项目档案化）
  ↓ 进域前先看画像
cross-project-change / hessian-orchestrate（跨项目变更）
  ↓ 过程中发现新坑
project-pitfalls（踩坑记录）
  ↓ 改代码时自动触发
Resolver 路由（AGENTS.md / inject-session-context.sh）
  ↓ 每次任务固化
不做一次性工作铁律（AGENTS.md / CLAUDE.md）
```

## 后续 TODO

- [ ] 用 `/domain-diarization` 给各业务域建档（iot-min、lot、yuncang 优先）
- [ ] OpenCode `project-profile` 和 Claude Code `domain-diarization` 输出格式对齐
- [ ] 审查现有 skill 哪些可以参数化，减少重复
- [ ] Resolver 关键词根据实际触发情况持续调优

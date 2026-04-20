# OpenCode Token 消耗优化指南

> 2026-04-20 整理 | 基于实际分析 session ~75K tokens 的经验

## 一、Token 消耗构成分析

| 组件 | 占比 | 说明 |
|---|---|---|
| **工具定义 (Tool Schemas)** | ~35% | 最大头。飞书 MCP 尤甚，参数定义极其冗长且多工具间重复 |
| **系统角色指令** | ~15% | Sisyphus 身份、Phase 0-3 流程、委派规则等 |
| **技能列表 (Skills)** | ~10% | 60+ 个 skill 按 project/user/opencode/builtin 四个 scope 各列一次 |
| **其他（约束、示例、Hook）** | ~25% | Anti-patterns、Delegation 表格、示例代码块等 |
| **AGENTS.md** | ~5% | 项目级 + 全局级两份 |
| **对话历史** | ~5% | 前几轮对话占比很小 |

**核心结论**：Token 主要被**工具定义的冗余**吃掉，对话本身占比很小。

## 二、已验证的优化手段

### 2.1 合并重复 Agent（效果最大）

**问题**：`oh-my-openagent.json` 中 `智谱*` 和 `code_*` 两组 agent 功能完全相同，每组 prompt 数百 tokens。

**操作**：保留 `code_*` 版本（更详细、更新），删除 `智谱*` 旧版。

**文件**：`~/.config/opencode/oh-my-openagent.json`

**效果**：198 行 → 122 行，每次会话省 ~3000-5000 tokens。

### 2.2 精简项目 AGENTS.md

**问题**：项目级 `AGENTS.md` 与全局 `~/.config/opencode/AGENTS.md` 大量重叠（中文回复、先读后改、最小修改、先确认再改等）。

**操作**：项目 AGENTS.md 只保留项目特有内容（构建命令、代码风格、项目约定），通用规则由全局 AGENTS.md 覆盖。

**文件**：项目根目录 `AGENTS.md`

**效果**：182 行 → 66 行，每次会话省 ~800-1200 tokens。

### 2.3 去重 Skill 目录

**问题**：
- 项目级 `skill-creator` 与全局 `~/.claude/skills/skill-creator/` 完全相同
- `xhs-explore`、`xhs-publish`、`xhs-auth` 三个 skill 的 `scripts/` 目录完全一致（各 508K）

**操作**：删除项目级重复的 `skill-creator`。xhs scripts 可手动用 symlink 替代（需绕过安全拦截）。

**效果**：去重 skill-creator 省 ~50-100 tokens/session；xhs 脚本省 ~1MB 磁盘。

## 三、无法直接优化的（需系统级改动）

| 项目 | 原因 | 潜在收益 |
|---|---|---|
| **飞书 MCP Schema 冗余** | MCP 服务端定义，需修改 MCP server 源码 | 15-20% |
| **技能列表三重叠加** | OpenCode 框架按 scope 分别列出，无法控制 | 5-8% |
| **行为指令压缩** | Sisyphus 系统提示词，框架层控制 | 3-5% |
| **按需加载工具** | 需框架支持动态注入 MCP | 10-15% |

### 飞书 MCP 具体冗余点

- `feishu_batch_create_feishu_blocks` 和 `feishu_create_feishu_table` 的 `options.anyOf` 列出了相同的 block 类型定义（text/code/heading/list/image/mermaid/whiteboard），每个都嵌套了完整的 style 对象
- 同一个 block 类型的 Schema 在多个工具中重复出现 3-4 次
- 建议：抽取 `$ref` 引用，或在 MCP server 端合并工具

## 四、优化检查清单

新项目/session 启动时：

- [ ] `oh-my-openagent.json` 中无重复 agent
- [ ] 项目 `AGENTS.md` 不与全局 `AGENTS.md` 重复通用规则
- [ ] 项目 `.claude/skills/` 不与 `~/.claude/skills/` 重复
- [ ] 同类 skill（如 xhs-* 系列）不重复存放脚本

## 五、快速估算 Token 的方法

1. 打开 OpenCode session，观察底部 token 计数
2. 对比"仅发一条消息后"和"多轮对话后"的增量——增量主要就是对话历史
3. 初始 token 数 = 系统提示 + 工具定义 + AGENTS.md + 技能列表
4. 减少初始 token 数的唯一有效手段：精简上述配置文件

---

相关文件：
- `~/.config/opencode/oh-my-openagent.json`
- `~/.config/opencode/AGENTS.md`
- `项目根目录/AGENTS.md`
- `~/.claude/skills/`
- `项目/.claude/skills/`

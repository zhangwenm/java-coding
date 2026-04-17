# Claude Code 日常工作流指南

> 基于 CLAUDE.md + 7 个自建 Skill + 3 个 Hook 的完整体系，指导日常高效使用。

## 体系全景

```
CLAUDE.md（北极星）
├── Skills（专家模式）
│   ├── hessian-rpc      Generator + Reviewer   Hessian 接口操作
│   ├── bug-audit         Pipeline               Bug 全量审查
│   ├── project-pitfalls  持续积累               踩坑记录
│   ├── work-report       自动化 + 持久化        日报/周报生成
│   ├── create-skill      元技能                 创建和优化其他 Skill
│   ├── arch-docs         索引                   架构文档定位
│   └── auto-project      自动识别               项目上下文推断
├── Hooks（自动化护栏）
│   ├── check-dangerous.sh    PreToolUse/Bash    拦截危险命令
│   ├── track-skill.sh        PreToolUse/Skill   记录 Skill 使用
│   └── 项目上下文提醒         UserPromptSubmit   每次提问前提醒确认上下文
└── Memory（跨会话记忆）
    ├── user_role.md           用户画像
    ├── feedback_*.md          行为反馈
    └── MEMORY.md              索引
```

## 日常场景速查

### 场景一：修 Bug

**说法**：`"帮我查下充电调度器的问题"` 或 `"这个类有 bug"`

**自动流程**：
1. `auto-project` → 定位到具体项目
2. `bug-audit` → 全量扫描，列出所有问题
3. 等你确认哪些要修
4. 统一修复 → `mvn clean compile` 验证

**关键**：说"查问题"而不是"修这个 bug"，让它走全量扫描流水线。

---

### 场景二：新增/修改 Hessian 接口

**说法**：`"在 SceneBaseService 新增一个批量查询方法"`

**自动流程**：
1. `hessian-rpc` Generator → 读取参考文件，生成接口定义 + 实现 + XML 配置
2. `hessian-rpc` Reviewer → 逐条检查 checklist（10 项）
3. 三侧编译验证（common/webservice → base → 调用方）

**关键**：不需要记 hessian-bean.xml 在哪，skill 有速查表。

---

### 场景三：设计/架构任务

**说法**：`"设计一个设备状态管理模块"` 或 `"出个技术方案"`

**自动流程**：
1. `arch-docs` → 先查索引，看有没有已有方案
2. 产出设计文档（不写代码），等你审批
3. 审批通过后才进入实现阶段
4. 新文档存到项目 `docs/` 目录，更新 arch-docs 索引

---

### 场景四：生成日报/周报

**说法**：`"日报"` 或 `"周报"` 或 `"总结下今天的工作"`

**自动流程**：
1. `work-report` → 扫描所有仓库当天/当周 git log
2. 合并相关 commit，翻译成业务语言，按项目归类
3. 展示初稿，问你有没有遗漏（会议、沟通等非代码工作）
4. 你补充后生成最终版，记录到 `history.json`

**进阶**：
- 周报时自动从日报记录聚合，不重新扫描
- 上次"明日计划"自动带入今天的"进行中"

---

### 场景五：沉淀经验为 Skill

**说法**：`"把这个流程做成 skill"` 或 `"创建一个 xxx 技能"`

**自动流程**：
1. `create-skill` → 最多问 3 个问题澄清需求
2. 选择设计模式（Generator/Reviewer/Pipeline/Tool Wrapper/Role Reversal）
3. 生成 skill 文件
4. 质量门禁检查（六原则逐条）
5. 高频 skill 可选做 Description 触发率验证

---

### 场景六：踩坑后记录

**不需要专门说**。解决完问题后，`project-pitfalls` skill 会提醒补充踩坑记录。

如果是 Hessian 相关的坑 → 补到 `hessian-rpc` skill 的 Gotcha 章节。
其他坑 → 补到 `project-pitfalls` 对应业务域章节。

---

## 效率技巧

### 1. 不需要指定项目路径

```
❌ "帮我看下 ~/appstore/project/iot-min/open-api 里的 WtDeviceController"
✅ "帮我看下 WtDeviceController 的问题"
```

`auto-project` 会根据类名自动定位。

### 2. 不需要每次说"先出设计文档"

CLAUDE.md 里已有规则：设计/架构任务自动先出文档，不写代码。

### 3. 不需要说"不要一个一个修"

`bug-audit` skill 里已写死：全量扫描 → 确认 → 统一修。

### 4. 不需要记常用网站权限

已加白名单：github.com、stackoverflow.com、maven.apache.org、docs.spring.io 等，不弹确认框。

### 5. 不需要担心危险命令

`rm -rf`、`DROP TABLE`、`git push --force origin main` 等已被 hook 拦截。

---

## Skill 使用统计

用一段时间后检查哪些 skill 真正在用：

```bash
# 查看使用频率
cat ~/.claude/skills/skill-usage.jsonl | jq -r '.skill' | sort | uniq -c | sort -rn

# 查看最近使用
tail -10 ~/.claude/skills/skill-usage.jsonl | jq '.'
```

**根据数据优化**：
- 高频 skill → 用 `create-skill` 的第四步优化 Description 触发率
- 从不触发的 skill → 检查 description 关键词，或者确认场景是否真的存在
- 经常误触发 → 补充"不适用"边界

---

## 设计原则（六原则）

创建或优化任何 skill 时遵循：

1. **Skip the obvious** — 不写 Claude 已经知道的东西
2. **Build a Gotcha section** — 必须有踩坑章节，记录真实踩过的坑
3. **Progressive disclosure** — 细节按需展开，不堆砌
4. **Store data** — 数据/路径/配置存文件引用，不硬编码
5. **Cite code** — 引用真实的文件路径/类名/方法名
6. **Un-constrained** — 不过度限制操作步骤

## 五种设计模式

| 模式 | 适用场景 | 已有示例 |
|------|---------|---------|
| Tool Wrapper | 封装外部工具的使用规范 | — |
| Generator | 按参考文件生成模板代码 | hessian-rpc |
| Reviewer | 变更后自动执行检查清单 | hessian-rpc |
| Pipeline | 多步骤流水线，步骤间有依赖 | bug-audit |
| Role Reversal | 让 Claude 反过来向用户提问 | — |

---

## 文件位置速查

| 文件 | 路径 | 作用 |
|------|------|------|
| CLAUDE.md | `~/CLAUDE.md` | 全局规则（北极星） |
| settings.json | `~/.claude/settings.json` | 权限白名单 + Hook 配置 |
| Skills 目录 | `~/.claude/skills/` | 所有自建 skill |
| Hook 脚本 | `~/.claude/hooks/` | 自动化护栏脚本 |
| Memory | `~/.claude/projects/-Users-admin-appstore-project/memory/` | 跨会话记忆 |
| Skill 使用日志 | `~/.claude/skills/skill-usage.jsonl` | 使用统计 |
| 日报历史 | `~/.claude/skills/work-report/history.json` | 报告持久化 |
| Java 规范 | `~/.claude/rules/java.md` | Java 开发规范 |
| 安全规则 | `~/.claude/rules/safety.md` | 安全防护规则 |
| 飞书规范 | `~/.claude/rules/feishu.md` | 飞书操作规范 |

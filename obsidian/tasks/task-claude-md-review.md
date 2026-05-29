---
tags: [CLAUDE.md, 工具链, 审查, 维护]
date: 2026-05-26
project: 工具链
due: 2026-11-26
status: in-progress
scope: cross-domain
generalized: false
retrieval_triggers: [CLAUDE.md审查, 规则过期, 工具链维护, 配置清理, next-review]
---

# CLAUDE.md 半年审查任务（截止 2026-11-26）

CLAUDE.md 每条规则都有 `since:` 时间戳，本任务记录下次审查截止日期和执行清单。

## 背景

Anthropic 官方建议每 3-6 个月回顾 CLAUDE.md（大版本模型发布后也应单独回顾）。旧规则会因模型能力提升而失效甚至反效果——比如"不做 X"类防御性规则，若新模型已默认遵守，留着只会增加上下文噪音。

> 参考来源：claude.com/blog/how-claude-code-works-in-large-codebases-best-practices-and-where-to-start

## 审查流程

1. 打开 `~/.claude/CLAUDE.md`，检查头部 `meta` 注释的 `last-reviewed` 和 `next-review`
2. 对照下方清单逐节审查
3. 删除/弱化失效规则，补充新发现的模式
4. 更新 `meta: last-reviewed=新日期 | next-review=新日期+6个月`
5. 更新本任务的 `status: done`，新建下一期任务

---

## 审查清单

### Compact Instructions（`since:2026-04`）

- [ ] "改代码前必须先读文件"——新模型是否已默认推断文件内容再修改？
- [ ] "不确定先问"——是否依然匹配当前工作节奏，还是变成了过度打断？
- [ ] 压缩保留规则（架构决策/修改列表/验证状态）——压缩后确认信息完整性

### 已有自动护栏（`since:2026-04, updated:2026-05`）

- [ ] `gateguard.js`：SKIP_SUFFIXES 列表是否需要增删（如新增数据类后缀）？
- [ ] `gateguard.js`：过去 6 个月是否出现过"不该被拦截却被拦截"的误伤？
- [ ] `maven-protection.js`：RISKY_PATTERNS 是否拦截过合法的 pom.xml 变更？
- [ ] `compile-java.sh`：是否所有 Java 修改都触发了正确模块的编译？

### 工作方式约定（`since:2026-04, updated:2026-05`）

- [ ] `bug-audit` 流水线——过去 6 个月是否真实使用过？未使用则考虑降级为建议而非强制
- [ ] "先产出设计文档再写代码"——是否依然是正确约束，还是已经无需强制？
- [ ] "手动压缩时机"——是否可改为 Hook 自动提示？（参见 [[guide-ecc-workflow]]）

### NEVER（`since:2026-04`）

- [ ] 逐条统计：过去 6 个月被模型违反了几次？
- [ ] 零违反次数的规则：模型是否已默认遵守？如是，可考虑删除
- [ ] 新产生的"不该做"是否需要补充到此节？

### ALWAYS（`since:2026-04, updated:2026-05`）

- [ ] "如实报告结果"——是否出现过"看起来""应该"类模糊措辞，抗拒效果如何？
- [ ] "改 Hessian 接口前遵循 checklist"——`hessian-rpc` skill 是否有更新，规则是否同步？

### Skill routing（`since:2026-04-25, updated:2026-05`）

- [ ] 每条路由指向的 skill 文件是否真实存在（`ls ~/.claude/skills/`）？
- [ ] 过去 6 个月哪些路由从未触发过——是触发词不准还是 skill 没有价值？
- [ ] `code-explorer`、`plan-eng-review` 是否在实践中有效命中过？
- [ ] 是否有高频场景没有对应路由，需要补充？

---

## 下次审查时间

**2026-11-26**（今天起 6 个月）

**提前触发条件**：Claude 大版本发布（claude-opus-5.x / claude-sonnet-5.x 等）后，无论时间间隔均应回顾一次。

## 相关链接

- [[guide-ecc-workflow]]
- CLAUDE.md 位置：`~/.claude/CLAUDE.md`

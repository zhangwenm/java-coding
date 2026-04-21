# Session 交接文档

> 每次开启新 session 时，把这个文件丢给 Claude，快速恢复上下文。

## 上次做了什么
- 修复 `[[guide-cmux-workflow]].md` 中 3 处"假自动"标注（cmux new / 任务完成同步 / 阻塞处理）
- 修复 7 处 "Claude" / "Claude Code" 残留引用，统一改为 OpenCode / Sisyphus
- 补上第四阶段（并行任务管理）和第五阶段（任务完成）的 👤 / 🤖 标注
- 更新速查卡片和角色分工表，🤖 / 👤 标注与实际能力一致
- 创建 `handoff.md` 交接文档

## 当前状态
- `guide-cmux-workflow.md` 所有阶段标注完毕，无"假自动"，术语已统一为 OpenCode 体系
- 整体自动化体系：
  - 🤖 已有 skill：`task-checkpoint`（保存进度）、`task-resume`（恢复任务）、`cross-project-change`（跨项目变更）、`project-profile`（项目建档）
  - 👤 所有 `cmux` 命令（new / start / merge / rm / ls）必须人在终端执行
  - 👤 阻塞任务处理（改 frontmatter、切任务）全是手动

## 下一步要做
- [x] 补上第四、第五阶段的 👤 / 🤖 标注
- [ ] 考虑创建 `task-complete` skill（任务完成时自动更新 Obsidian 文档为 done）
- [ ] 用 `project-profile` 给各子项目建档（iot-min、hk、manager、yuncang、lot、datacenter）
- [ ] 审查现有 90+ skills 中哪些是「一次性」的，可以合并/参数化
- [ ] 把铁律加入全局 `~/.config/opencode/AGENTS.md`，让所有项目生效

## 关键文件
- `/Users/admin/appstore/project/AGENTS.md` — 项目级规则（铁律 + Resolver 路由）
- `/Users/admin/.config/opencode/AGENTS.md` — 全局规则（未改过）
- `/Users/admin/appstore/project/java-coding/obsidian/guides/guide-cmux-workflow.md` — 主文档，本轮重点修改对象
- `/Users/admin/appstore/project/java-coding/obsidian/guides/[[guide-thin-harness-fat-skills]].md` — 优化记录笔记
- `/Users/admin/appstore/project/.claude/skills/task-resume/SKILL.md` — 任务恢复 skill
- `/Users/admin/appstore/project/.claude/skills/task-checkpoint/SKILL.md` — 进度保存 skill
- `/Users/admin/appstore/project/.claude/skills/cross-project-change/SKILL.md` — 跨项目变更 skill
- `/Users/admin/appstore/project/.claude/skills/project-profile/SKILL.md` — 项目建档 skill
- `/Users/admin/appstore/project/java-coding/obsidian/templates/task-template.md` — 任务文档模板

## 遗留问题
- 没有 `task-complete` skill，任务完成后的 status: done 由 `task-checkpoint` 兜底，不够精确
- `cmux` 是 shell function，Sisyphus 无法执行任何 cmux 命令，所有 worktree 操作都是 👤
- 全局 AGENTS.md (`~/.config/opencode/AGENTS.md`) 还没加铁律和 Resolver 规则

---
tags:
  - 工具链
  - 工作流
  - OpenCode
tool: OpenCode
status: done
created: 2026-04-18
---

# cmux + Obsidian 长线任务完整工作流指南

> **使用者：OpenCode（Sisyphus Agent）**
> 本指南仅适用于 OpenCode（Sisyphus Agent）。如需 Claude Code 版本，参考 [[guide-cc-long-task]] 自行适配。

## 核心理念

**一个任务 = 一个 Obsidian 文档 + 一个 cmux worktree**

| 角色 | 工具 | 解决的问题 |
|------|------|---------|
| 任务记忆 | Obsidian 任务文档 | OpenCode 会话重置后如何恢复 |
| 代码隔离 | cmux worktree | 多任务并行互不干扰 |
| 唯一纽带 | branch 名 | 两者绑定的 key |

没有 cmux：每次新会话都要重新给 Sisyphus 解释背景  
没有 Obsidian：worktree 里代码在，但不知道做到哪一步了  
两者结合：说"继续" → Sisyphus 自动恢复 → 立刻接着做

---

## 准备工作

### 环境检查

```bash
# 验证 cmux 已安装
cmux version

# 验证 autocli 已安装（可选，用于搜索参考资料）
autocli --version

# 在目标项目加 .gitignore
echo '.worktrees/' >> ~/appstore/project/manager/rw-backend/.gitignore
```

### 项目路径速查

| 业务域 | 常用子模块路径 |
|--------|-------------|
| manager | `~/appstore/project/manager/rw-backend` |
| manager | `~/appstore/project/manager/crowdsourcing-backend` |
| manager | `~/appstore/project/manager/workorder-backend` |
| lot | `~/appstore/project/lot/robot-call-api` |
| lot | `~/appstore/project/lot/open-yuncang-api` |
| datacenter | `~/appstore/project/datacenter/job-executor` |
| yuncang | `~/appstore/project/yuncang/cargotransport` |

---

## 完整生命周期

### 第一阶段：任务启动

**Step 1：告诉 Sisyphus 要做什么（一句话就行）**

```
修复机器人离线告警误报，SceneService 里的离线判断逻辑有问题
```

Sisyphus 自动完成：
1. 探索相关代码，定位关键文件和方法
2. 在 `tasks/` 下生成任务文档（按 task-template 格式）
3. 填好目标、背景、范围、里程碑、执行计划、涉及文件
4. 推荐 branch 名（遵循命名规范）

**你只需要确认文档内容，不用手动建文件。**

> **命名规范**
> - `fix-` 前缀：bug 修复
> - `feature-` 前缀：新功能
> - `refactor-` 前缀：重构
> - `chore-` 前缀：配置/依赖等杂项
> - 全部小写，用 `-` 连接，简短有意义

**Step 2：确认后，你在终端执行 cmux new** 👤

> Sisyphus 会生成完整的 `cmux new` 命令（含 branch 名和 `-p` 描述），你复制到终端执行。

```bash
source ~/.cmux/cmux.sh
cd ~/appstore/project/manager/rw-backend

cmux new fix-robot-offline -p "
任务：修复机器人离线告警误报
背景：NVWA 机器人网络抖动后恢复，仍触发离线告警
入口：SceneService.checkRobotStatus，离线时间窗口判断有误
要求：修改前先写能复现 bug 的测试，修改后让测试通过
参考：ai.yunji.rw.scene 包下的现有实现
"
```

> **注意**：cmux 是 shell function，每次新终端需要 `source ~/.cmux/cmux.sh`

---

### 第二阶段：执行中

> **🤖 已自动化**：`task-checkpoint` skill 已固化。会话结束时 Sisyphus 自动触发，无需手动操作。
>
> 触发方式：
> - 用户说"保存进度"、"checkpoint"、"先到这" → 立即保存
> - 用户说"好了"、"收工"、"谢谢" → 自动检测是否有进行中任务，有则保存
> - `git commit` 完成后 → 自动更新 Feature 清单的 Commit 字段

如果自动 checkpoint 未生效（极少见），可以手动更新：

```markdown
## 当前状态快照
- 最后更新：2026-04-18
- 当前进展：阶段二进行中，已定位问题在 SceneService.java:234，offlineThreshold 计算错误
- 下次启动入口：`cd ~/appstore/project/manager/rw-backend && cmux start fix-robot-offline`
- 待续位置：SceneService.java:234，修改 offlineThreshold 计算逻辑，然后补 SceneServiceTest
```

**待续位置的粒度要够细**，越具体越好：

| 太粗（没用） | 够细（有用） |
|------------|------------|
| "修复 bug" | "SceneService.java:234，offlineThreshold 改为滑动窗口计算" |
| "写测试" | "SceneServiceTest，补 testRobotReconnectNoAlert 方法" |
| "联调接口" | "RobotCallController.handleOffline，入参 robotId 为空时 NPE" |

---

### 第三阶段：任务恢复

> **🤖 已自动化**：`task-resume` skill 已固化，只需说"继续"即可自动恢复。

**方式 A：一句话恢复（推荐）**

直接对 Sisyphus 说：

```
继续
```

Sisyphus 自动完成：
1. 扫描 Obsidian tasks/ 找到 `status: in-progress` 的任务
2. 读取任务文档，提取状态快照、待续位置、关键决策
3. 确认 worktree 存在，执行 `cmux start`
4. 恢复完整上下文，精确到文件/方法/行

**全程不需要手动打开 Obsidian、不需要复制粘贴。**

**方式 B：手动恢复（备选）**

如果自动恢复不生效，可手动操作（30 秒）：

1. 打开 Obsidian，找到对应任务文档
2. 复制"下次启动入口"命令
3. 粘贴到终端，cmux start 会恢复 worktree 并启动 opencode 会话
4. opencode 新会话的第一句话：把任务文档的"当前状态快照"贴进去

```
上次进展：已定位 SceneService.java:234，offlineThreshold 计算错误
待续位置：修改计算逻辑，然后补 SceneServiceTest.testRobotReconnectNoAlert
请继续。
```

Sisyphus 立刻从上次停的地方接着做，不需要重新解释背景。

---

### 第四阶段：并行任务管理

**同时有多个任务时：**

```bash
# 👤 查看所有活跃 worktree（你在终端执行）
cmux ls

# 输出示例：
# /manager/rw-backend/.worktrees/fix-robot-offline     [fix-robot-offline]
# /manager/rw-backend/.worktrees/feature-task-export   [feature-task-export]
# /lot/robot-call-api/.worktrees/refactor-retry-logic  [refactor-retry-logic]
```

**对应 Obsidian tasks/ 目录结构：**

```
tasks/
  fix-robot-offline.md       ← status: in-progress
  feature-task-export.md     ← status: in-progress  
  refactor-retry-logic.md    ← status: blocked（等 lot 接口评审）
  fix-cron-memory-leak.md    ← status: done
```

**快速判断每个任务状态：** 看文档 frontmatter 的 `status` 字段，比 `cmux ls` 更直观。

> **切任务：** 👤 你在终端执行 `cmux start feature-task-export`，会启动新的 opencode 会话。

---

### 第五阶段：任务完成

```bash
# 🤖 Sisyphus 能跑测试
cd ~/appstore/project/manager/rw-backend/.worktrees/fix-robot-offline
mvn clean compile && mvn test

# 👤 你在终端执行合并
cd ~/appstore/project/manager/rw-backend
cmux merge fix-robot-offline

# 👤 你在终端执行清理
cmux rm fix-robot-offline
```

**Sisyphus 通过 task-checkpoint 同步 Obsidian 文档：**

合并完成后，Sisyphus 触发 `task-checkpoint` 更新任务文档（你也会收到确认提示）：

- `status` → `done`
- 填写里程碑实际完成时间
- 补充备注（改了什么、为什么这么改、需同步的其他仓库）

> ⚠️ 目前没有独立的 `task-complete` skill，完成状态由 `task-checkpoint` 兜底处理。
> 你可以在 Sisyphus 说"checkpoint 完成"后看一眼文档确认 status 已改为 done。

```markdown
---
status: done
---

## 里程碑
| 里程碑 | 目标日期 | 实际完成 |
|--------|---------|---------|
| 定位根因 | 2026-04-18 | 2026-04-18 |
| 修复+测试 | 2026-04-19 | 2026-04-19 ✅ |

## 备注
offlineThreshold 原来用固定值 30s，改为滑动窗口 3 次心跳超时才判定离线。
注意：同样逻辑在 hk 分支也有，需要同步修改。
```

---

## 阻塞任务处理 👤

> 以下操作目前均为手动，没有对应的自动化 skill。

遇到外部依赖（等审批、等接口评审、等别人改代码）：

```bash
# 1. 手动更新文档状态（Obsidian 中编辑 frontmatter）
status: blocked

# 2. 手动记录阻塞项
## 阻塞项
| 阻塞内容 | 等待对象 | 记录时间 | 解除时间 |
|---------|---------|---------|---------|
| lot 接口评审通过 | @李工 | 2026-04-18 | |

# 3. worktree 保留不动，手动切换到其他任务 👤
cmux start feature-task-export   # 去做另一个任务

# 4. 阻塞解除后，手动改回 👤
status: in-progress
# 继续：cmux start fix-xxx
```

---

## 与 Hessian RPC 变更的结合

Hessian 接口变更需要同时改 common 和 base，是最典型的多仓库并行场景：

```bash
# common/webservice：定义接口
cd ~/appstore/project/common/webservice
cmux new feature-new-config-api -p "新增配置下发接口，参考现有 ConfigService 定义"

# base：实现接口
cd ~/appstore/project/base/config  
cmux new feature-new-config-api -p "实现 common/webservice 中新增的配置下发接口"

# manager/rw-backend：调用方
cd ~/appstore/project/manager/rw-backend
cmux new feature-new-config-api -p "调用新的配置下发接口，替换旧的轮询方式"
```

三个仓库用**同一个 branch 名**，Obsidian 只需一个任务文档，"涉及文件"字段列清三个仓库的路径。

---

## 常见问题

**Q：worktree 里 Maven 找不到本地依赖怎么办？**

在 `.cmux/setup` 里加 symlink：
```bash
#!/bin/bash
REPO_ROOT="$(git rev-parse --git-common-dir | xargs dirname)"
ln -sf "$REPO_ROOT/.env" .env 2>/dev/null || true
# Maven 本地仓库共享，不需要额外处理（~/.m2 是全局的）
```

**Q：branch 名和 Obsidian 文件名不一致怎么找？**

约定：Obsidian 文件名 = branch 名，如 `fix-robot-offline.md` 对应 `fix-robot-offline` 分支。

**Q：任务做了一半发现需要拆分怎么办？**

```bash
cmux new fix-robot-offline-unitTest -p "从 fix-robot-offline 拆出来，专门补单测"
# 原 worktree 继续做主逻辑
```
Obsidian 里新建子任务文档，在原文档"备注"里加链接。

**Q：临时切换主分支查个东西？**

```bash
# 不用切分支，直接在主目录看
cd ~/appstore/project/manager/rw-backend  # 这里是 main
# worktree 里的修改完全独立，不影响主目录
```

**Q：出现 `_cmux_check_update not found` 错误怎么办？**

cmux 是 shell function，opencode 的非交互式 bash 环境不会自动 source `.zshrc`，导致辅助函数丢失。

```bash
# 在出问题的终端里重新加载
source ~/.cmux/cmux.sh

# 或者直接开一个新终端（会自动 source .zshrc）
```

注意：`cmux version`（无短横线）是正确写法，`cmux --version` 会报 Unknown command。

---

## Feature 清单（复杂任务专用）

任务涉及多个有顺序依赖的功能点时，在任务文档的 Feature 清单里跟踪进度：

| ID | Feature | 验收标准 | Status | Commit |
|----|---------|---------|--------|--------|
| F001 | 设备注册模块 | POST /api/devices 201 + mvn test 通过 | done | abc123 |
| F002 | Adapter 接口定义 | AdapterService + 单测覆盖 | in-progress | |
| F003 | 任务状态机 | ActionTask 状态流转 + Redis 缓存 | pending | |

**Current**: F002

规则：
- `cmux start` 前先看 **Current** 是哪个，只做这一个
- 完成后填 Commit，Status 改 done，Current 指向下一个
- 简单任务（单功能）不需要这个表，用"执行计划"的 checkbox 就够

## 速查卡片

```
新任务   → 告诉 Sisyphus 一句话 → 它自动建文档 + 推荐 branch → 你确认 → 👤 你执行 cmux new
复杂任务 → 先填 Feature 清单（有顺序依赖时）→ cmux start 前确认 Current
会话结束 → 🤖 自动 checkpoint（或说"保存进度"强制触发）
下次开工 → 说"继续"→ 🤖 task-resume 自动恢复（无需打开 Obsidian）
阻塞了   → 👤 手动 status: blocked → 👤 手动 cmux start 其他任务
完成了   → 👤 你执行 cmux merge + cmux rm → 🤖 task-checkpoint 更新 status: done
```

## 角色分工

| 步骤 | 你 | Sisyphus | 自动化 |
|------|---|-----------|--------|
| 描述任务 | ✅ 说一句话 | - | - |
| 探索代码定位 | - | ✅ 自动分析 | 🤖 |
| 生成任务文档 | - | ✅ 按 template 生成 | 🤖 |
| 确认文档内容 | ✅ 看一眼确认 | - | - |
| 创建 worktree | ✅ 复制命令到终端执行 | ✅ 生成 cmux new 命令 | 👤 |
| 执行开发 | - | ✅ 分解+委派子 agent | 🤖 |
| 更新状态快照 | - | ✅ task-checkpoint 自动 | 🤖 |
| 恢复任务上下文 | - | ✅ task-resume 自动（说"继续"） | 🤖 |
| 任务完成同步 | ✅ 确认 status: done | ✅ task-checkpoint 兜底 | 🤖 |
| 合并+清理 | ✅ 你执行 cmux merge + rm | ✅ 生成命令 | 👤 |
| 阻塞处理 | ✅ 手动改 frontmatter | - | 👤 |

## 自动化 skill 清单

| Skill | 触发方式 | 解决的问题 |
|-------|---------|-----------|
| `task-resume` | 说"继续" | 消除手动打开 Obsidian + 复制粘贴 |
| `task-checkpoint` | 会话结束自动 / 说"保存进度" | 消除手动更新状态快照 |
| `cross-project-change` | 涉及多项目改动时 | 通用化的跨项目变更编排 |
| `project-profile` | 说"项目概况" | 一页项目画像，新人秒懂 |

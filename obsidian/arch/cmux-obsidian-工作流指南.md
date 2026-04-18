---
tags:
  - 工具链
  - 工作流
  - Claude-Code
status: done
created: 2026-04-18
---

# cmux + Obsidian 长线任务完整工作流指南

## 核心理念

**一个任务 = 一个 Obsidian 文档 + 一个 cmux worktree**

| 角色 | 工具 | 解决的问题 |
|------|------|---------|
| 任务记忆 | Obsidian 任务文档 | Claude 上下文重置后如何恢复 |
| 代码隔离 | cmux worktree | 多任务并行互不干扰 |
| 唯一纽带 | branch 名 | 两者绑定的 key |

没有 cmux：每次新会话都要重新给 Claude 解释背景  
没有 Obsidian：worktree 里代码在，但不知道做到哪一步了  
两者结合：打开文档 → 复制命令 → Claude 立刻接着做

---

## 准备工作

### 环境检查

```bash
# 验证 cmux 已安装
cmux --version

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

**Step 1：在 obsidian/tasks/ 新建任务文档**

用 task-template 新建，立刻填好三个核心字段：

```markdown
---
branch: fix-robot-offline        ← 分支命名规范见下方
status: in-progress
---

## 目标
修复机器人短暂断网恢复后仍触发离线告警的问题

## 背景
生产反馈：NVWA 机器人网络抖动后告警误报，影响运营判断

## 范围
### 包含
- SceneService 中的离线判断逻辑
- 对应单元测试

### 不包含
- 前端告警展示（不在此次范围）
```

**Step 2：创建 cmux worktree**

```bash
cd ~/appstore/project/manager/rw-backend

cmux new fix-robot-offline -p "
任务：修复机器人离线告警误报
背景：NVWA 机器人网络抖动后恢复，仍触发离线告警
入口：SceneService.checkRobotStatus，离线时间窗口判断有误
要求：修改前先写能复现 bug 的测试，修改后让测试通过
参考：ai.yunji.rw.scene 包下的现有实现
"
```

> **命名规范**
> - `fix-` 前缀：bug 修复
> - `feature-` 前缀：新功能
> - `refactor-` 前缀：重构
> - `chore-` 前缀：配置/依赖等杂项
> - 全部小写，用 `-` 连接，简短有意义

---

### 第二阶段：执行中

**每次 Claude 会话结束前，必须更新任务文档的"当前状态快照"**

这是最重要的习惯，10 秒钟，保证下次能无缝恢复：

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

**下次开工的完整流程（30 秒内完成）：**

1. 打开 Obsidian，找到对应任务文档
2. 复制"下次启动入口"命令
3. 粘贴到终端，cmux start 会恢复 worktree 并打开 Claude
4. Claude 新会话的第一句话：把任务文档的"当前状态快照"贴进去

```
上次进展：已定位 SceneService.java:234，offlineThreshold 计算错误
待续位置：修改计算逻辑，然后补 SceneServiceTest.testRobotReconnectNoAlert
请继续。
```

Claude 立刻从上次停的地方接着做，不需要重新解释背景。

---

### 第四阶段：并行任务管理

**同时有多个任务时：**

```bash
# 查看所有活跃 worktree
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

---

### 第五阶段：任务完成

```bash
# 1. 确保测试通过
cd ~/appstore/project/manager/rw-backend/.worktrees/fix-robot-offline
mvn clean compile && mvn test

# 2. 合并到主分支
cd ~/appstore/project/manager/rw-backend
cmux merge fix-robot-offline

# 3. 清理 worktree
cmux rm fix-robot-offline
```

**同步更新 Obsidian 文档：**

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

## 阻塞任务处理

遇到外部依赖（等审批、等接口评审、等别人改代码）：

```bash
# 1. 更新文档状态
status: blocked

# 2. 记录阻塞项
## 阻塞项
| 阻塞内容 | 等待对象 | 记录时间 | 解除时间 |
|---------|---------|---------|---------|
| lot 接口评审通过 | @李工 | 2026-04-18 | |

# 3. worktree 保留不动，切换到其他任务
cmux start feature-task-export   # 去做另一个任务

# 4. 阻塞解除后
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

---

## 速查卡片

```
新任务   → obsidian 建文档 → cmux new <branch> -p "任务描述"
会话结束 → 更新"当前状态快照"（待续位置要具体到行）
下次开工 → 打开文档 → 复制启动命令 → 贴快照给 Claude
阻塞了   → status: blocked → cmux start 其他任务
完成了   → cmux merge → cmux rm → status: done
```

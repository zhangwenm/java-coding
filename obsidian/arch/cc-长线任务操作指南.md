---
tags:
  - 工具链
  - 工作流
  - Claude-Code
status: done
created: 2026-04-18
---

# Claude Code 长线任务操作指南

> 配套文档：[[cmux-obsidian-工作流指南]]、[[cmux-多Agent并行开发]]

## 与 Sisyphus 的差异

CC 能完成工作流中的所有操作，差别只在**触发方式**：

| 步骤 | Sisyphus | Claude Code |
|------|---------|-------------|
| 探索代码、定位文件 | 自动 | ✅ 自动 |
| 生成任务文档 | 自动 | ✅ 自动 |
| 执行 cmux new | 自动 | ✅ 自动 |
| 执行开发、写测试 | 自动 | ✅ 自动 |
| 会话结束更新快照 | 自动感知 | ⚠️ 你说一句"更新快照" |
| 完成后同步文档 | 自动 | ⚠️ 你说一句"merge 并更新文档" |

**结论**：多说两句话，换来完整的上下文管理。

---

## 工作流全流程

### 启动新任务

直接描述需求，CC 自动完成后续：

```
修复机器人离线告警误报，SceneService 里的离线判断逻辑有问题
```

CC 会自动：
1. 定位相关代码（Grep/Read）
2. 在 `obsidian/tasks/` 生成任务文档（按 task-template 格式）
3. 推荐 branch 名
4. 等你确认后执行 `cmux new`

你只需确认文档内容是否准确。

---

### 会话中途保存进度

随时可以说，不必等会话结束：

```
保存一下当前进度
```

CC 更新任务文档的"当前状态快照"，包括：
- 当前进展（做到哪一步）
- 下次启动入口（`cmux start` 命令）
- 待续位置（具体到文件:行号）

---

### 会话结束前（关键习惯）

**每次结束前说这一句：**

```
更新任务快照
```

CC 将当前进展写入任务文档。下次开工靠这个恢复，忘了说这句等于丢了上下文。

---

### 下次开工恢复上下文

打开 Obsidian 任务文档 → 复制"当前状态快照" → 新会话第一句贴进去：

```
上次进展：【粘贴快照内容】
请继续。
```

CC 立刻从上次停的地方接着做，不需要重新解释背景。

---

### 任务完成

```
测试通过了，merge 并更新文档
```

CC 自动执行：
```bash
source ~/.cmux/cmux.sh
cd <项目路径>
cmux merge <branch>
cmux rm <branch>
```

同时更新任务文档：`status: done`、填写里程碑完成时间、补充备注。

---

## 并行任务切换

同时有多个任务时，切换只需说：

```
先放下这个，去看一下 fix-cron-leak 任务
```

CC 会：
1. 先更新当前任务快照（保存现场）
2. 读取目标任务文档（恢复上下文）
3. 执行 `cmux start fix-cron-leak`

---

## 常用指令速查

| 你说 | CC 做的事 |
|------|---------|
| `新任务：xxx` | 探索代码 → 生成文档 → cmux new |
| `更新任务快照` | 写入当前进展到任务文档 |
| `查看所有任务` | cmux ls + 列出 tasks/ 下的 in-progress 文档 |
| `切换到 <branch>` | 保存当前快照 → cmux start <branch> |
| `merge 并更新文档` | cmux merge + rm + 文档 status: done |
| `这个任务阻塞了，等 xxx` | 更新文档 status: blocked，记录阻塞项 |

---

## Stop Hook 配置（可选）

配置后，CC 每次会话结束**自动**提醒更新快照，省去手动说那句话。

`~/.claude/settings.json`：

```json
{
  "hooks": {
    "Stop": [{
      "matcher": "",
      "hooks": [{
        "type": "command",
        "command": "/bin/bash -c 'branch=$(git -C \"$(pwd)\" rev-parse --abbrev-ref HEAD 2>/dev/null); [[ \"$branch\" != \"main\" && \"$branch\" != \"master\" && -n \"$branch\" ]] && echo \"[提醒] 当前在 worktree 分支 $branch，记得更新 obsidian/tasks/$branch.md 的状态快照\"'"
      }]
    }]
  }
}
```

效果：在非 main 分支的 worktree 里工作时，会话结束自动打印提醒。

---

## 与项目 Hook 的关系

你已有的自动 Hook（不受影响）：

| Hook | 触发时机 | 行为 |
|------|---------|------|
| `compile-java.sh` | 修改 `.java` 后 | 自动 `mvn clean compile` |
| `track-java-edits.sh` | Edit/Write `.java` 后 | 记录修改路径 |
| `check-dangerous.sh` | 执行 Bash 前 | 拦截危险命令 |

cmux worktree 里改代码，这些 Hook 照常生效。

---

## 速查卡片

```
新任务   → "新任务：一句话描述" → CC 自动建文档+cmux new
执行中   → 正常开发，随时说"保存进度"
结束前   → 说"更新任务快照"（必做）
下次开工 → 贴快照给 CC → "请继续"
并行切换 → "切换到 <branch>"
完成     → "merge 并更新文档"
阻塞     → "这个任务阻塞了，等 xxx"
```

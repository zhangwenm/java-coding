# 日报使用指南

> 每天结束后记录完成的任务项，保持上下文连续性。

## 每次会话结束时

告诉 Claude：

```
"帮我更新今天的日报"
```

Claude 会自动：
1. 读取当天 `daily/YYYY-MM-DD.md`（不存在则创建）
2. 根据对话上下文填写今日任务、编码记录、学到的东西
3. 将未完成事项写入明日待做

## 记录 git 改动任务（/daily-task-log）

告诉 Claude：

```
"把 L2 路径下项目的改动记录到 obsidian"
"记录 lot/robot-api 今天的改动"
"把 manager 的改动写入日记"
```

Claude 会：
1. 扫描指定路径下的 git 仓库
2. 提取今日 commit（含 subject + body）
3. 按以下格式写入「今日任务」section

### 任务条目格式

```markdown
- [x] **[<仓库短名> · HH:MM]** `<type>` <subject>
	- <改动要点（类名/接口/文件名保留反引号）>
	- <改动要点>
```

**示例：**

```markdown
- [x] **[L2/l2-orchestrator · 13:53]** `feat` MES-AGV 对接 API-03：任务操作指令
	- 新增 `AgvCommandType` 枚举（11 个指令）和 `MesAgvCommandRequest` DTO
	- 新增 `POST /api/v1/mes/command`，透传指令到 Kafka `mes_agv_callback` topic
	- 复用已有 `MesAgvCallbackProducer`，Kafka 失败静默处理
	- 新增 4 个单元测试用例
```

## 模板位置

`templates/daily.md`

## 文件命名

`daily/YYYY-MM-DD.md`

## 与 task-checkpoint 的区别

| 日报 | task-checkpoint |
|------|----------------|
| 每天一次，记录完成的任务 | 任务中途保存断点 |
| 面向人回顾 | 面向 LLM 恢复上下文 |
| `daily/` 目录 | `tasks/` 目录 |

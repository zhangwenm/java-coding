# Hessian 多 Agent 并行开发指南

> 基于 `hessian-orchestrate` skill，比串行快 3-5 倍。

---

## 整体架构

```
你（主终端）
  ├─ 分析影响范围 → 生成任务分工文件
  ├─ Worker 1：common/webservice（先完成）
  │     ↓ 完成后
  ├─ Worker 2：base/实现层  ─┐ 并行
  ├─ Worker 3：调用方A      ─┤ 并行
  └─ Worker 4：调用方B      ─┘ 并行
```

---

## Step 1：触发 skill（主终端）

直接说：
```
新增 Hessian 接口 XxxService.doSomething(...)
```
或
```
修改 Hessian 接口 XxxService，把参数改为...
```

Skill 会自动 grep 分析受影响模块，**不要自己数，让它分析**。

---

## Step 2：Skill 生成任务文件

Skill 会写入 `~/appstore/project/.hessian-task.md`，内容含：
- 变更规格（接口签名、DTO 字段）
- 每个 Worker 的具体任务、关键文件路径
- 完成标志 `[ ]`

**确认内容无误后再开终端。**

---

## Step 3：开多个终端，按顺序启动 Worker

**顺序规则：Worker 1 先完成，其余并行**

```bash
# 终端 1（先启动）
cd ~/appstore/project/common/webservice
claude
# 输入：读取 ~/appstore/project/.hessian-task.md，执行 Worker 1 的任务
```

等终端 1 的 Claude 把完成标志改为 `[x]` 后，**同时**开终端 2、3...：

```bash
# 终端 2（与终端 3+ 同时启动）
cd ~/appstore/project/base/xxx
claude
# 输入：读取 ~/appstore/project/.hessian-task.md，执行 Worker 2 的任务
```

> Stop Hook 会自动跑 `mvn clean compile`，编译失败 Worker 会自动修复，无需干预。

---

## Step 4：汇总验收（回主终端）

所有 Worker 报告完成后，回主终端说：**所有 Worker 已完成，帮我汇总验收**

Skill 会：
1. 检查所有 `[x]` 是否齐全
2. 对每个模块跑 `mvn clean compile`
3. 清理 `.hessian-task.md`

---

## 注意事项

| 坑 | 说明 |
|---|---|
| Worker 1 必须先完成 | base 和调用方依赖接口定义，顺序错了必编译失败 |
| base 和调用方可并行 | 两者都只依赖 Worker 1，互不依赖 |
| 调用方不一定都要改 | 只新增方法且暂不调用，只需 Worker 1+2 |
| 路径用绝对路径 | 任务文件统一用 `~/appstore/project/.hessian-task.md` |
| DTO 必须序列化 | implements Serializable + serialVersionUID |
| bean name 大小写 | 路径大小写必须与 serviceUrl 完全一致 |

---

## 速查：一句话触发

```
新增/修改/删除 Hessian 接口 [接口名]，[具体变更描述]
```

Skill 接管后按提示走即可。

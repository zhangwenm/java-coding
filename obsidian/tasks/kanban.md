---
tags: [kanban, 任务看板]
date: 2026-05-29
status: active
retrieval_triggers: [看板, 任务列表, kanban, 所有任务]
---

# 任务看板

## 进行中

```dataview
TABLE date, project, scope, branch
FROM "tasks"
WHERE status = "in-progress" AND file.name != "kanban"
SORT date DESC
```

## 待开始

```dataview
TABLE date, project, due
FROM "tasks"
WHERE status = "draft" OR status = "todo"
SORT date ASC
```

## 阻塞

```dataview
TABLE date, project
FROM "tasks"
WHERE status = "blocked"
SORT date ASC
```

## 近期完成（30天）

```dataview
TABLE date, project
FROM "tasks"
WHERE (status = "done" OR status = "verified") AND date >= date(today) - dur(30 days)
SORT date DESC
```

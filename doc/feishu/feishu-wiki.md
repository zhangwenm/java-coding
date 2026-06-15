# feishu-wiki

飞书知识库导航工具，支持浏览和管理知识空间。

## 功能描述

当用户提到知识库、wiki 或 wiki 链接时激活。

## Token 提取

从 URL `https://xxx.feishu.cn/wiki/ABC123def` → `token` = `ABC123def`

## 操作列表

### 列出知识空间

```json
{ "action": "spaces" }
```

返回所有可访问的 wiki 空间。

### 列出节点

```json
{ "action": "nodes", "space_id": "7xxx" }
```

带父节点:

```json
{ "action": "nodes", "space_id": "7xxx", "parent_node_token": "wikcnXXX" }
```

### 获取节点详情

```json
{ "action": "get", "token": "ABC123def" }
```

返回: `node_token`、`obj_token`、`obj_type` 等。使用 `obj_token` 与 `feishu_doc` 读写文档。

### 创建节点

```json
{ "action": "create", "space_id": "7xxx", "title": "New Page" }
```

带类型和父节点:

```json
{
  "action": "create",
  "space_id": "7xxx",
  "title": "Sheet",
  "obj_type": "sheet",
  "parent_node_token": "wikcnXXX"
}
```

`obj_type`: `docx`（默认）、`sheet`、`bitable`、`mindnote`、`file`、`doc`、`slides`

### 移动节点

```json
{ "action": "move", "space_id": "7xxx", "node_token": "wikcnXXX" }
```

到不同位置:

```json
{
  "action": "move",
  "space_id": "7xxx",
  "node_token": "wikcnXXX",
  "target_space_id": "7yyy",
  "target_parent_token": "wikcnYYY"
}
```

### 重命名节点

```json
{ "action": "rename", "space_id": "7xxx", "node_token": "wikcnXXX", "title": "New Title" }
```

## Wiki-Doc 工作流程

要编辑 wiki 页面:

1. 获取节点: `{ "action": "get", "token": "wiki_token" }` → 返回 `obj_token`
2. 读取文档: `feishu_doc { "action": "read", "doc_token": "obj_token" }`
3. 写入文档: `feishu_doc { "action": "write", "doc_token": "obj_token", "content": "..." }`

## 配置

```yaml
channels:
  feishu:
    tools:
      wiki: true # 默认: true
      doc: true # 必需 - wiki 内容使用 feishu_doc
```

**依赖**: 此工具需要启用 `feishu_doc`。Wiki 页面是文档 - 使用 `feishu_wiki` 导航，然后 `feishu_doc` 读写内容。

## 权限

必需: `wiki:wiki` 或 `wiki:wiki:readonly`

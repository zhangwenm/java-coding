# feishu-doc

飞书云文档读写操作工具，支持创建、读取、更新、删除文档内容，以及表格操作。

## 功能描述

当用户提到飞书文档、云文档或 docx 链接时激活。

## Token 提取

从 URL `https://xxx.feishu.cn/docx/ABC123def` → `doc_token` = `ABC123def`

## 操作列表

### 读取文档

```json
{ "action": "read", "doc_token": "ABC123def" }
```

返回: 标题、纯文本内容、块统计。检查 `hint` 字段 - 如存在，表示有需要 `list_blocks` 的结构化内容（表格、图片）。

### 写入文档（替换全部）

```json
{ "action": "write", "doc_token": "ABC123def", "content": "# Title\n\nMarkdown content..." }
```

用 markdown 内容替换整个文档。支持: 标题、列表、代码块、引用、链接、图片（`![](url)` 自动上传）、粗体/斜体/删除线。

**限制**: Markdown 表格不支持。

### 追加内容

```json
{ "action": "append", "doc_token": "ABC123def", "content": "Additional content" }
```

追加 markdown 到文档末尾。

### 创建文档

```json
{ "action": "create", "title": "New Document", "owner_open_id": "ou_xxx" }
```

带文件夹:

```json
{
  "action": "create",
  "title": "New Document",
  "folder_token": "fldcnXXX",
  "owner_open_id": "ou_xxx"
}
```

**重要**: 始终传递 `owner_open_id`（来自入站元数据 `sender_id`），使用户自动获得 `full_access` 权限。

### 列出块

```json
{ "action": "list_blocks", "doc_token": "ABC123def" }
```

返回完整块数据，包括表格、图片。使用此读取结构化内容。

### 获取单个块

```json
{ "action": "get_block", "doc_token": "ABC123def", "block_id": "doxcnXXX" }
```

### 更新块文本

```json
{
  "action": "update_block",
  "doc_token": "ABC123def",
  "block_id": "doxcnXXX",
  "content": "New text"
}
```

### 删除块

```json
{ "action": "delete_block", "doc_token": "ABC123def", "block_id": "doxcnXXX" }
```

### 创建表格

```json
{
  "action": "create_table",
  "doc_token": "ABC123def",
  "row_size": 2,
  "column_size": 2,
  "column_width": [200, 200]
}
```

可选: `parent_block_id` 插入到特定块下。

### 写入表格单元格

```json
{
  "action": "write_table_cells",
  "doc_token": "ABC123def",
  "table_block_id": "doxcnTABLE",
  "values": [
    ["A1", "B1"],
    ["A2", "B2"]
  ]
}
```

### 一站式创建表格

```json
{
  "action": "create_table_with_values",
  "doc_token": "ABC123def",
  "row_size": 2,
  "column_size": 2,
  "column_width": [200, 200],
  "values": [
    ["A1", "B1"],
    ["A2", "B2"]
  ]
}
```

### 上传图片到文档

```json
{
  "action": "upload_image",
  "doc_token": "ABC123def",
  "url": "https://example.com/image.png"
}
```

或本地路径:

```json
{
  "action": "upload_image",
  "doc_token": "ABC123def",
  "file_path": "/tmp/image.png",
  "parent_block_id": "doxcnParent",
  "index": 5
}
```

可选 `index`（0-based）插入到兄弟块中的特定位置。

### 上传文件附件

```json
{
  "action": "upload_file",
  "doc_token": "ABC123def",
  "url": "https://example.com/report.pdf"
}
```

或本地路径:

```json
{
  "action": "upload_file",
  "doc_token": "ABC123def",
  "file_path": "/tmp/report.pdf",
  "filename": "Q1-report.pdf"
}
```

## 读取工作流程

1. 从 `action: "read"` 开始 - 获取纯文本 + 统计
2. 检查响应中的 `block_types` - Table、Image、Code 等
3. 如有结构化内容，用 `action: "list_blocks"` 获取完整数据

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
      doc: true # 默认: true
```

**注意**: `feishu_wiki` 依赖此工具 - wiki 页面内容通过 `feishu_doc` 读写。

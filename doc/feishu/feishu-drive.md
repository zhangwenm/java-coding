# feishu-drive

飞书云空间文件管理工具，支持列出文件夹内容、获取文件信息、创建文件夹、移动和删除文件。

## 功能描述

当用户提到云空间、文件夹或 drive 时激活。

## Token 提取

从 URL `https://xxx.feishu.cn/drive/folder/ABC123` → `folder_token` = `ABC123`

## 操作列表

### 列出文件夹内容

```json
{ "action": "list" }
```

根目录（无 folder_token）:

```json
{ "action": "list", "folder_token": "fldcnXXX" }
```

返回: 带有 token、name、type、url、timestamps 的文件。

### 获取文件信息

```json
{ "action": "info", "file_token": "ABC123", "type": "docx" }
```

在根目录搜索文件。注意: 文件必须在根目录或先用 `list` 浏览文件夹。

`type`: `doc`、`docx`、`sheet`、`bitable`、`folder`、`file`、`mindnote`、`shortcut`

### 创建文件夹

```json
{ "action": "create_folder", "name": "New Folder" }
```

在父文件夹中:

```json
{ "action": "create_folder", "name": "New Folder", "folder_token": "fldcnXXX" }
```

### 移动文件

```json
{ "action": "move", "file_token": "ABC123", "type": "docx", "folder_token": "fldcnXXX" }
```

### 删除文件

```json
{ "action": "delete", "file_token": "ABC123", "type": "docx" }
```

## 文件类型

| Type | 描述 |
|------|------|
| `doc` | 旧格式文档 |
| `docx` | 新格式文档 |
| `sheet` | 电子表格 |
| `bitable` | 多维表格 |
| `folder` | 文件夹 |
| `file` | 上传的文件 |
| `mindnote` | 思维导图 |
| `shortcut` | 快捷方式 |

## 配置

```yaml
channels:
  feishu:
    tools:
      drive: true # 默认: true
```

## 权限

- `drive:drive` - 完全访问（创建、移动、删除）
- `drive:drive:readonly` - 只读（列出、信息）

## 已知限制

- **机器人没有根文件夹**: 飞书机器人使用 `tenant_access_token`，没有"我的空间"概念。这意味着:
  - 不带 `folder_token` 的 `create_folder` 会失败（400 错误）
  - 机器人只能访问已与其共享的文件/文件夹
  - **解决方案**: 用户必须先手动创建文件夹并与机器人共享，然后机器人可以在其中创建子文件夹

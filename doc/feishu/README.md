# Feishu Skills Documentation

本目录包含飞书（Feishu）平台相关的技能文档，用于文档操作、知识库管理、权限控制和云空间管理。

## 技能列表

| 技能名称 | 功能描述 |
|----------|----------|
| [feishu-doc](feishu-doc.md) | 飞书文档读写操作 |
| [feishu-wiki](feishu-wiki.md) | 飞书知识库导航 |
| [feishu-perm](feishu-perm.md) | 飞书文档/文件权限管理 |
| [feishu-drive](feishu-drive.md) | 飞书云空间文件管理 |

## 通用 Token 提取

### 从 URL 提取 Token

| 工具 | URL 格式 | Token 提取 |
|------|----------|------------|
| feishu_doc | `https://xxx.feishu.cn/docx/ABC123def` | `doc_token` = `ABC123def` |
| feishu_wiki | `https://xxx.feishu.cn/wiki/ABC123def` | `token` = `ABC123def` |
| feishu_drive | `https://xxx.feishu.cn/drive/folder/ABC123` | `folder_token` = `ABC123` |

## 配置

在配置文件中启用飞书工具：

```yaml
channels:
  feishu:
    tools:
      doc: true       # 默认: true
      wiki: true      # 默认: true
      drive: true     # 默认: true
      perm: false     # 默认: false（权限管理需要显式启用）
```

## 权限要求

| 工具 | 所需权限 |
|------|----------|
| feishu_doc | `docx:document`, `docx:document:readonly`, `docx:document.block:convert`, `drive:drive` |
| feishu_wiki | `wiki:wiki` 或 `wiki:wiki:readonly` |
| feishu_perm | `drive:permission` |
| feishu_drive | `drive:drive`（完全访问）或 `drive:drive:readonly`（只读） |

## 已知限制

- **机器人没有根文件夹**: 飞书机器人使用 `tenant_access_token`，没有"我的空间"概念
- 机器人只能访问已与其共享的文件/文件夹
- **解决方案**: 用户必须先手动创建文件夹并与机器人共享

## 工具类型

| Type | 描述 |
|------|------|
| `doc` | 旧格式文档 |
| `docx` | 新格式文档 |
| `sheet` | 电子表格 |
| `bitable` | 多维表格 |
| `folder` | 文件夹 |
| `file` | 上传的文件 |
| `wiki` | Wiki 节点 |
| `mindnote` | 思维导图 |

# feishu-perm

飞书文档/文件权限管理工具，支持列出协作者、添加权限和移除权限。

## 功能描述

当用户提到分享、权限或协作者时激活。

## 操作列表

### 列出协作者

```json
{ "action": "list", "token": "ABC123", "type": "docx" }
```

返回: 具有 member_type、member_id、perm、name 的成员。

### 添加协作者

```json
{
  "action": "add",
  "token": "ABC123",
  "type": "docx",
  "member_type": "email",
  "member_id": "user@example.com",
  "perm": "edit"
}
```

### 移除协作者

```json
{
  "action": "remove",
  "token": "ABC123",
  "type": "docx",
  "member_type": "email",
  "member_id": "user@example.com"
}
```

## Token 类型

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

## 成员类型

| Type | 描述 |
|------|------|
| `email` | 邮箱地址 |
| `openid` | 用户 open_id |
| `userid` | 用户 user_id |
| `unionid` | 用户 union_id |
| `openchat` | 群组 chat open_id |
| `opendepartmentid` | 部门 open_id |

## 权限级别

| Perm | 描述 |
|------|------|
| `view` | 仅查看 |
| `edit` | 可编辑 |
| `full_access` | 完全访问（可管理权限） |

## 示例

### 通过邮箱分享文档

```json
{
  "action": "add",
  "token": "doxcnXXX",
  "type": "docx",
  "member_type": "email",
  "member_id": "alice@company.com",
  "perm": "edit"
}
```

### 通过群组分享文件夹

```json
{
  "action": "add",
  "token": "fldcnXXX",
  "type": "folder",
  "member_type": "openchat",
  "member_id": "oc_xxx",
  "perm": "view"
}
```

## 配置

```yaml
channels:
  feishu:
    tools:
      perm: true # 默认: false（权限管理是敏感操作，需显式启用）
```

**注意**: 此工具默认禁用，因为权限管理是敏感操作。需要时显式启用。

## 权限

必需: `drive:permission`

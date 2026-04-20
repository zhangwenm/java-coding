---
tags: [tools, claude-code, dotfiles, migration]
date: 2026-04-20
project: workflow
status: active
---

# Claude Code 配置迁移指南

**结论**：Claude Code 配置完全可移植，换机器只需克隆 dotfiles 仓库 + 手动补充密钥文件，10 分钟内恢复完整工作环境。

## 配置仓库结构

```
~/.claude/                    ← dotfiles 仓库根目录
├── settings.json             ✅ 已提交（无密钥）
├── settings.local.json       ❌ gitignore（含密钥，手动配置）
├── rules/                    ✅ 已提交（全局规则）
├── skills/                   ✅ 已提交（96 个 skills）
├── hooks/                    ✅ 已提交（自动化脚本）
├── projects/                 ✅ 已提交（Memory 记忆文件）
└── AGENTS.md                 ✅ 已提交（工作流编排索引）

~/CLAUDE.md                   ✅ 在 java-coding 仓库中独立管理
~/appstore/project/java-coding/obsidian/  ✅ 在 java-coding 仓库中
```

## 换机器步骤

### 1. 安装基础环境
```bash
# Claude Code
npm install -g @anthropic-ai/claude-code

# nvm + Node
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 20
nvm use 20

# MCP 依赖包
npm install -g feishu-mcp
```

### 2. 克隆 dotfiles
```bash
git clone git@github.com:zhangwenm/claude-dotfiles.git ~/.claude
```

### 3. 手动创建 settings.local.json（含密钥）

```json
{
  "mcpServers": {
    "notionApi": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@notionhq/notion-mcp-server"],
      "env": {
        "NOTION_TOKEN": "<从密码管理器取>"
      }
    },
    "feishu": {
      "type": "stdio",
      "command": "<which node 的输出>",
      "args": [
        "<which feishu-mcp 的输出>",
        "--feishu-app-id=<app-id>",
        "--feishu-app-secret=<从密码管理器取>",
        "--feishu-auth-type=tenant",
        "--feishu-scope-validation=false",
        "--stdio"
      ]
    }
  }
}
```

> 注意：feishu 的 `command` 和第一个 `args` 路径依赖本机 nvm 版本，需用 `which node` 和 `find ~/.nvm -name feishu-mcp` 查出实际路径填入。

### 4. 克隆 Obsidian 知识库
```bash
git clone git@github.com:zhangwenm/java-coding.git ~/appstore/project/java-coding
```

## 注意事项

- `settings.local.json` 永远不提交，含 Notion Token 和飞书 AppSecret
- feishu-mcp 路径是 nvm 版本强绑定的，换机器后必须重新查路径
- `projects/`（Memory）已提交，新机器克隆后历史记忆自动恢复
- `skills/` 96 个全量提交，如果之前有删减记得同步

## 日常维护

配置有变更时（新增 skill、改 hook、改 rules）：
```bash
cd ~/.claude
git add -A
git commit -m "更新配置：<描述>"
git push
```

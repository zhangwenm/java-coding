---
tags: [tools, opencode, dotfiles, migration]
date: 2026-04-20
project: workflow
status: active
---

# OpenCode 配置迁移指南

**结论**：OpenCode 配置完全可移植，换机器只需克隆 3 个 Git 仓库 + 设置 2 个环境变量，5 分钟恢复完整工作环境。

## 配置架构（3 仓库 + 项目级）

```
~/.claude/                        ← claude-dotfiles 仓库（已有）
├── skills/                       ✅ 90+ Skills（含 hooks）
├── hooks/                        ✅ 8 个自动化脚本
├── AGENTS.md                     ✅ 全局 Agent 指令
└── .gitignore                    ✅ 排除缓存/密钥/运行时

~/.config/opencode/               ← opencode-config 仓库（新建）
├── opencode.json                 ✅ 模型/Provider 配置（apiKey 用 {env:}）
├── oh-my-openagent.json          ✅ Agent 定义（6 个自定义 Agent）
├── AGENTS.md                     ✅ 全局编码规范
├── setup-new-machine.sh          ✅ 一键迁移脚本
└── .gitignore                    ✅ 排除 node_modules/备份

~/.agents/skills/                 ← agents-skills 仓库（新建）
├── apify-*/                      ✅ 14 个 Apify 系列 Skills
├── planning-with-files/          ✅ Manus 式文件规划
└── .gitignore                    ✅ 排除缓存

项目级（随项目 Git 走，无需额外操作）：
~/appstore/project/
├── .claude/skills/xhs-*/         ✅ 小红书系列 Skills（6 个）
├── .mcp.json                     ✅ MCP 服务配置（apiKey 用 {env:}）
└── AGENTS.md                     ✅ 项目编码约定
```

## 密钥管理策略

| 密钥 | 存储位置 | 配置文件引用方式 |
|------|----------|-----------------|
| BAILIAN_API_KEY | `~/.zshrc` 环境变量 | `{env:BAILIAN_API_KEY}` |
| OBSIDIAN_API_KEY | `~/.zshrc` 环境变量 | `{env:OBSIDIAN_API_KEY}` |

OpenCode 原生支持 `{env:VAR_NAME}` 语法，官方推荐在 `apiKey` 字段使用。参考文档：https://opencode.ai/docs/en/config/

## 换机器步骤

### 1. 安装基础环境

```bash
# Node.js（通过 bun 或 nvm）
curl -fsSL https://bun.sh/install | bash

# OpenCode
npm install -g opencode

# GitHub CLI（用于后续 gh auth）
brew install gh
```

### 2. 克隆 3 个配置仓库

```bash
git clone git@github.com:zhangwenm/claude-dotfiles.git ~/.claude
git clone git@github.com:zhangwenm/opencode-config.git ~/.config/opencode
git clone git@github.com:zhangwenm/agents-skills.git ~/.agents/skills
```

### 3. 配置 API Keys

在 `~/.zshrc`（或 `~/.bashrc`）末尾添加：

```bash
# ===== OpenCode / AI API Keys =====
export BAILIAN_API_KEY="你的百炼 API Key"
export OBSIDIAN_API_KEY="你的 Obsidian API Key"
```

然后 `source ~/.zshrc` 或新开终端。

### 4. 启动

```bash
cd ~/appstore/project && opencode
```

## 也可以用一键脚本

```bash
# 交互式引导，自动克隆仓库 + 提示输入 API Keys
bash <(curl -sL https://raw.githubusercontent.com/zhangwenm/opencode-config/master/setup-new-machine.sh)
```

## 日常维护

### 配置变更后提交

```bash
# OpenCode 配置变更
cd ~/.config/opencode && git add -A && git commit -m "描述变更" && git push

# Agents Skills 变更
cd ~/.agents/skills && git add -A && git commit -m "描述变更" && git push

# Claude Skills/Hooks 变更（已有 sync-dotfiles.sh 自动同步）
~/.claude/hooks/sync-dotfiles.sh
```

### 误删恢复

```bash
# 查看历史
git -C ~/.config/opencode log --oneline

# 恢复单个文件
git -C ~/.config/opencode checkout <commit-hash> -- opencode.json

# 只查看不恢复
git -C ~/.config/opencode show <commit-hash>:opencode.json
```

## 注意事项

- `~/.zshrc` 中的密钥**永远不提交**到任何 Git 仓库
- `{env:VAR}` 语法要求 `"$schema": "https://opencode.ai/config.json"` 必须保留，否则 OpenCode 启动时会用实际值覆盖变量引用（已在 v2026.1.18 修复）
- gstack 系列 skills（`~/.claude/skills/gstack*/`）是外部克隆的 git repo，`~/.claude/.gitignore` 已排除，换机器后需重新安装 gstack
- 项目级配置（`.claude/skills/`、`AGENTS.md`、`.mcp.json`）随项目代码 Git 一起走，无需额外操作

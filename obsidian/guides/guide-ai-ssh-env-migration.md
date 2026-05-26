---
tags: [tools, claude-code, ssh, migration, homebrew]
date: 2026-05-26
project: 工具链
status: draft
scope: cross-domain
generalized: false
retrieval_triggers: [换电脑, 环境迁移, AI+SSH, 新机初始化, 开发环境复现]
---

# AI+SSH 全自动开发环境迁移

**结论**：旧机开 SSH 控制端，Claude Code 通过 `ssh newmac` 远程操控新机完成 Homebrew/npm/VS Code/dotfiles 全量迁移，人只需审阅验收报告。核心前提是提前处理好 SSH PATH 和安装顺序两个陷阱。

## 架构

```
旧电脑（控制端）
  └── Claude Code
        ├── 审计本机环境 → 生成清单（formula/cask/npm/pip/vscode 分文件）
        └── ssh newmac "命令"  →  新电脑（被控端）
```

## 已验证的陷阱（踩过的坑）

### 陷阱 1：SSH 非交互 session 找不到 brew

SSH 进新机走非交互 shell，不加载 `~/.zshrc`，brew 不在 PATH。

```bash
# ❌ 会报 command not found
ssh newmac "brew install ..."

# ✅ 显式 source shellenv
ssh newmac 'eval "$(/opt/homebrew/bin/brew shellenv)" && brew install ...'
```

根治方案：在新机 `~/.zshenv` 写入 PATH（对所有 SSH session 生效）：
```bash
ssh newmac 'echo '\''eval "$(/opt/homebrew/bin/brew shellenv)"'\'' >> ~/.zshenv'
```

### 陷阱 2：Formula 和 Cask 必须分开处理

```bash
# ❌ cask 装不上（或装错）
brew install $(cat brew-cask.txt)

# ✅ 显式加 --cask
brew install --formula $(cat brew-formula.txt | tr '\n' ' ')
brew install --cask    $(cat brew-cask.txt   | tr '\n' ' ')
```

### 陷阱 3：rsync dotfiles 会覆盖新机 SSH config（断连风险）

`~/.ssh/config` 里有 `Host newmac` 配置，覆盖后会破坏当前连接。SSH 相关文件单独处理：

```bash
# ❌ 不要整体 rsync ~/.ssh/
rsync -avz ~/.ssh/ newmac:~/.ssh/

# ✅ 只同步公钥，config 手动合并
rsync -avz ~/.ssh/id_ed25519.pub newmac:~/.ssh/
ssh newmac "cat >> ~/.ssh/authorized_keys < ~/.ssh/id_ed25519.pub"
```

### 陷阱 4：安装顺序依赖

必须串行，不能并发：

```
Homebrew → node/python → npm/pip 全局包
Homebrew → VS Code (cask) → code CLI → VS Code 扩展
```

### 陷阱 5：整批安装一个失败即中断

```bash
# ❌ 100 个包里 1 个报错，全部中断
brew install pkg1 pkg2 ... pkg100

# ✅ 逐个安装，记录失败项
while read pkg; do
  brew install "$pkg" || echo "FAILED: $pkg" >> /tmp/brew-failed.txt
done < brew-formula.txt
```

## 完整执行步骤

### 第一步：打通连接（新机）

```bash
# macOS 开启 SSH
sudo systemsetup -setremotelogin on

# 查 IP
ifconfig | grep "inet " | grep -v 127.0.0.1
```

### 第二步：配置 SSH（旧机）

```bash
ssh-keygen -t ed25519 -C "migration"
ssh-copy-id user@192.168.x.x

cat >> ~/.ssh/config << 'EOF'
Host newmac
  HostName 192.168.x.x
  User your_username
  ControlMaster auto
  ControlPath ~/.ssh/ctrl/%r@%h:%p
  ControlPersist 30m
  ServerAliveInterval 60
EOF

mkdir -p ~/.ssh/ctrl && chmod 700 ~/.ssh/ctrl
ssh newmac "echo OK"   # 验证
```

### 第三步：新机预处理

```bash
# sudo 免密（迁移期间用）
ssh newmac "sudo sh -c 'echo \"$(whoami) ALL=(ALL) NOPASSWD: ALL\" >> /etc/sudoers'"

# 写入 PATH 到 ~/.zshenv（所有 SSH session 生效）
ssh newmac 'echo '\''eval "$(/opt/homebrew/bin/brew shellenv)"'\'' >> ~/.zshenv'
```

### 第四步：旧机审计（分文件）

```bash
mkdir -p /tmp/migration
brew list --formula > /tmp/migration/brew-formula.txt
brew list --cask    > /tmp/migration/brew-cask.txt
npm list -g --depth=0 2>/dev/null | awk 'NR>1{print $2}' | cut -d@ -f1 > /tmp/migration/npm-globals.txt
pip list --format=freeze 2>/dev/null > /tmp/migration/pip-globals.txt
code --list-extensions 2>/dev/null > /tmp/migration/vscode-ext.txt
```

### 第五步：新机安装（按顺序）

```bash
# 1. Homebrew
ssh newmac '/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'

# 2. Formula（逐个）
while read pkg; do
  ssh newmac "eval \"\$(/opt/homebrew/bin/brew shellenv)\" && brew install $pkg" \
    || echo "FAILED: $pkg" >> /tmp/migration/brew-formula-failed.txt
done < /tmp/migration/brew-formula.txt

# 3. Cask（逐个）
while read pkg; do
  ssh newmac "eval \"\$(/opt/homebrew/bin/brew shellenv)\" && brew install --cask $pkg" \
    || echo "FAILED: $pkg" >> /tmp/migration/brew-cask-failed.txt
done < /tmp/migration/brew-cask.txt

# 4. npm 全局包（node 装好后）
while read pkg; do
  ssh newmac "npm install -g $pkg" || echo "FAILED: $pkg" >> /tmp/migration/npm-failed.txt
done < /tmp/migration/npm-globals.txt

# 5. VS Code 扩展（code CLI 装好后）
while read ext; do
  ssh newmac "code --install-extension $ext" || echo "FAILED: $ext" >> /tmp/migration/vscode-failed.txt
done < /tmp/migration/vscode-ext.txt
```

### 第六步：同步 dotfiles（跳过 ~/.ssh/）

```bash
rsync -avz ~/.zshrc ~/.gitconfig ~/.gitignore_global newmac:~/
rsync -avz ~/.config/ newmac:~/.config/
# ❌ 不同步 ~/.ssh/config，手动合并
```

### 第七步：验收

```bash
for cmd in git node python3 brew; do
  old=$(command -v $cmd 2>/dev/null && $cmd --version 2>&1 | head -1)
  new=$(ssh newmac "source ~/.zshenv; command -v $cmd 2>/dev/null && $cmd --version 2>&1 | head -1")
  echo "[$cmd] 旧: $old | 新: $new"
done

# 查看所有失败项
ls /tmp/migration/*-failed.txt 2>/dev/null | xargs grep . 2>/dev/null
```

## 给 Claude Code 的 CLAUDE.md 模板

```markdown
# 环境迁移任务

## 执行规则
- 新机命令前缀：ssh newmac 'source ~/.zshenv && <命令>'
- 文件传输用 rsync，不用 scp
- 每步验证结果后再进下一步
- 失败项记录到 /tmp/migration/*-failed.txt，不中断整体流程

## 安装顺序（严格遵守）
1. Homebrew → 2. CLI tools (formula) → 3. Apps (cask) → 4. node/python → 5. npm/pip globals → 6. VS Code → 7. VS Code 扩展 → 8. dotfiles sync
```

## 决策记录

| 方案 | 结论 | 原因 |
|------|------|------|
| 整批 `brew install` | ❌ | 单包失败即中断，无法定位具体失败项 |
| 逐个 install + 记录失败 | ✅ | 可继续跑完，最后处理失败列表 |
| rsync 整个 ~/.ssh/ | ❌ | 覆盖新机 SSH config，可能断开当前连接 |
| 公钥单独 rsync + config 手动合并 | ✅ | 安全，保留新机已有配置 |
| 直接 `ssh newmac "brew ..."` | ❌ | 非交互 session PATH 里没有 brew |
| 每条命令前 eval shellenv | ✅ | 根治 PATH 问题，无需依赖登录 shell |

## 相关链接

- [[guide-claude-dotfiles-migration]] — Claude Code 自身配置的 dotfiles git 方案（更轻量，仅迁移 ~/.claude/）

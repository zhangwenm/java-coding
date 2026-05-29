---
tags: [ghostty, terminal, 工具链, 终端]
date: 2026-05-26
project: 工具链
status: done
scope: cross-domain
generalized: false
retrieval_triggers: [ghostty, 终端配置, terminal emulator, 快捷键, 分屏]
---

# Ghostty 终端使用指南

Ghostty 是当前最佳的原生终端模拟器之一：GPU 加速、零依赖、内置 Nerd 字体，开箱即用不需要配置。

---

## 安装

### macOS
```bash
brew install --cask ghostty
```
或下载 `.dmg`：https://ghostty.org/download

### Linux
```bash
# Arch
pacman -S ghostty

# Alpine
apk add ghostty

# Snap（通用）
snap install ghostty --classic

# Ubuntu/Debian/Fedora：使用社区维护包
# 参考：https://ghostty.org/docs/install/binary
```

### 从源码构建（需匹配 Zig 版本）
| Ghostty 版本 | Zig 版本 |
|---|---|
| 1.0.x–1.1.x | 0.13.0 |
| 1.2.x | 0.14.1 |
| 1.3.x+ | 0.15.2 |

```bash
zig build -Doptimize=ReleaseFast
```

---

## 配置文件

**路径**：`~/.config/ghostty/config`（Linux / macOS 均适用）

格式：`key = value`，支持注释 `#`，空值重置为默认。

运行时重载（Linux）：`ctrl+shift+,`

查看所有默认配置：
```bash
ghostty +show-config --default --docs
```

---

## 常用配置速查

### 字体
```ini
font-family = "JetBrains Mono"
font-family-bold = "JetBrains Mono"
font-size = 14
font-thicken = true          # macOS 专属：加粗渲染，视网膜屏更清晰
```

### 颜色与透明度
```ini
background = #1e1e2e
foreground = #cdd6f4
background-opacity = 0.95    # 1.0 = 不透明
cursor-color = #f5e0dc
cursor-style = bar           # block / bar / underline / block_hollow
cursor-style-blink = false
```

### 主题（数百个内置，来自 iterm2-color-schemes）
```ini
# 单一主题
theme = Catppuccin Frappe

# 深浅色自动切换
theme = dark:Catppuccin Frappe,light:Catppuccin Latte
```

查看所有可用主题：
```bash
ghostty +list-themes
```

自定义主题放在：`~/.config/ghostty/themes/<name>`

### 窗口
```ini
window-padding-x = 8
window-padding-y = 8
window-decoration = none     # 去掉标题栏（极简风格）
window-theme = dark          # dark / light / auto / system
maximize = false
```

### 终端行为
```ini
scrollback-limit = 10485760  # 10 MB
shell-integration = zsh      # 自动检测，或手动指定
mouse-reporting = true
```

---

## 快捷键

查看所有默认快捷键：
```bash
ghostty +list-keybinds --default
```

### 自定义快捷键语法
```ini
# 基本格式
keybind = ctrl+shift+t=new_tab
keybind = super+d=new_split:right
keybind = super+shift+d=new_split:down

# 前缀修饰
keybind = global:super+grave_accent=toggle_quick_terminal  # 全局（macOS）
keybind = all:ctrl+c=copy_to_clipboard                     # 所有面板
keybind = unconsumed:ctrl+shift+v=paste_from_clipboard     # 透传给程序
```

### 常用默认快捷键（macOS）
| 操作 | 快捷键 |
|------|--------|
| 新建标签 | `cmd+t` |
| 关闭标签 | `cmd+w` |
| 横向分屏 | `cmd+d` |
| 纵向分屏 | `cmd+shift+d` |
| 切换面板 | `cmd+[` / `cmd+]` |
| 跳转到上一个提示符 | `cmd+shift+up` |
| 重载配置 | `cmd+shift+,` |
| 放大字号 | `cmd+=` |
| 缩小字号 | `cmd+-` |

---

## Shell Integration

Ghostty 自动注入 bash / zsh / fish / elvish / nushell，无需手动配置。

**额外能力：**
- 新终端自动继承上一个窗口的工作目录
- `Ctrl+Click`（macOS：`Cmd+Click`）选中整条命令的输出
- 提示符处光标自动变 bar 样式（区分输入区和输出区）
- 关闭时不弹确认框（如果光标在提示符处）
- `jump_to_prompt` 快捷键在多条命令输出间跳转

手动配置（macOS 自带 bash 需要手动）：
```bash
# ~/.bashrc 或 ~/.zshrc
source "${GHOSTTY_RESOURCES_DIR}/shell-integration/bash/ghostty.bash"
```

---

## 分屏与标签页

| 概念 | 操作 |
|------|------|
| 横向分屏 | `cmd+d`（macOS）/ `keybind = new_split:right` |
| 纵向分屏 | `cmd+shift+d` / `new_split:down` |
| 关闭面板 | `cmd+w` |
| 切换面板 | `cmd+[` / `cmd+]` 或鼠标点击 |
| 新标签页 | `cmd+t` |
| 重命名标签 | 右键标签 |

---

## Quick Terminal（macOS 专属）

菜单栏常驻的轻量终端，按快捷键弹出/收起：

```ini
# 建议绑定全局快捷键
keybind = global:super+grave_accent=toggle_quick_terminal
quick-terminal-position = top    # top / bottom / left / right / center
quick-terminal-size-percent = 40
quick-terminal-animation-duration = 0.2
```

---

## 性能说明

- macOS：Metal 渲染，性能约为 iTerm2 的 100 倍
- Linux：OpenGL 渲染，与 Alacritty 相当
- 多线程架构：读取、写入、渲染三线程独立
- 推荐关闭不需要的 `mouse-reporting` 以减少 event 压力

---

## 与 Claude Code 配合

```ini
# 推荐配置：用于长时间 AI 编码会话
scrollback-limit = 52428800    # 50 MB，保留完整 Claude 输出
font-size = 13
shell-integration = zsh
window-padding-x = 6
window-padding-y = 6
background-opacity = 1.0       # 纯色背景，减少渲染压力
```

---

## 相关链接

- 官方文档：https://ghostty.org/docs
- 配置参考：https://ghostty.org/docs/config/reference
- GitHub：https://github.com/ghostty-org/ghostty
- 主题预览：https://iterm2colorschemes.com

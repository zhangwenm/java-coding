---
tags:
  - 快捷命令
  - 工具
date: 2026-04-18
---

# CC & OC 快捷启动

## cc — Claude Code 快速启动

```bash
cc() { 
  export https_proxy=http://127.0.0.1:7898 && \
  export http_proxy=http://127.0.0.1:7898 && \
  export all_proxy=socks5://127.0.0.1:7898 && \
  cd /Users/admin/appstore/project && \
  claude "$@"; 
}
```

**作用**：
1. 设置代理（`127.0.0.1:7898`）
2. 切到项目目录 `/Users/admin/appstore/project`
3. 启动 Claude Code，透传所有参数

**使用示例**：
- `cc` — 直接启动
- `cc --model sonnet` — 指定模型启动

---

## oc — OpenCode 快速启动

```bash
oc() { 
  cd /Users/admin/appstore/project && \
  opencode "$@"; 
}
```

**作用**：
1. 切到项目目录 `/Users/admin/appstore/project`
2. 启动 OpenCode，透传所有参数
3. 无需代理（OpenCode 走国内接口，不需要翻墙）

**使用示例**：
- `oc` — 直接启动
- `oc --help` — 查看帮助

---

## 配置位置

`~/.zshrc` 第 1-2 行

## 代理说明

仅 `cc`（Claude Code）需要代理，`oc`（OpenCode）不需要。

| 环境变量 | 值 | cc | oc |
|---|---|---|---|
| `http_proxy` | `http://127.0.0.1:7898` | ✅ | ❌ |
| `https_proxy` | `http://127.0.0.1:7898` | ✅ | ❌ |
| `all_proxy` | `socks5://127.0.0.1:7898` | ✅ | ❌ |

代理仅在命令执行期间生效，不会污染当前 shell 环境（因使用 `&&` 链式调用，函数返回后环境变量仍保留在当前 session 中）。

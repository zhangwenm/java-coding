---
tags:
  - 快捷命令
  - 工具
date: 2026-04-21
---

# CC & OC 快捷启动

## cc — Claude Code 快速启动

```bash
cc() {
  export https_proxy=http://127.0.0.1:7898 http_proxy=http://127.0.0.1:7898 all_proxy=socks5://127.0.0.1:7898 no_proxy=127.0.0.1,localhost
  if [[ -n "$1" && -d "$1" ]]; then
    local target="$1"; shift
    cd "$target" && claude "$@"
  else
    cd /Users/admin/appstore/project && claude "$@"
  fi
}
```

**作用**：
1. 设置代理（`127.0.0.1:7898`），同时排除本地地址（`no_proxy`）
2. **智能目录跳转**：若第一个参数是目录路径，则切换到该目录后启动；否则默认切到 `/Users/admin/appstore/project`
3. 启动 Claude Code，透传剩余参数

**使用示例**：
- `cc` — 默认切到项目目录启动
- `cc ~/appstore/project/iot-min` — 切到指定目录启动
- `cc --model sonnet` — 指定模型启动

**升级点（2026-04-21）**：
- 代理设置改为单行 `export`，更简洁
- 新增 `no_proxy=127.0.0.1,localhost`，避免本地请求走代理
- 新增智能目录参数：`cc <目录>` 可直接在指定目录启动

---

## oc — OpenCode 快速启动

```bash
oc() {
  export https_proxy=http://127.0.0.1:7898 http_proxy=http://127.0.0.1:7898 all_proxy=socks5://127.0.0.1:7898 no_proxy=127.0.0.1,localhost
  cd /Users/admin/appstore/project && opencode "$@"
}
```

**作用**：
1. 设置代理（统一和 cc 保持一致）
2. 切到项目目录 `/Users/admin/appstore/project`
3. 启动 OpenCode，透传所有参数

**使用示例**：
- `oc` — 直接启动
- `oc --help` — 查看帮助

---

## 配置位置

`~/.zshrc` 第 1-2 行

## 代理说明

| 环境变量 | 值 | cc | oc |
|---|---|---|---|
| `http_proxy` | `http://127.0.0.1:7898` | ✅ | ✅ |
| `https_proxy` | `http://127.0.0.1:7898` | ✅ | ✅ |
| `all_proxy` | `socks5://127.0.0.1:7898` | ✅ | ✅ |
| `no_proxy` | `127.0.0.1,localhost` | ✅ | ✅ |

# xhs-auth

小红书认证管理技能。检查登录状态、登录（二维码或手机号）、多账号管理。

## 功能描述

当用户要求登录小红书、检查登录状态、切换账号时触发。

## 允许使用的 CLI 子命令

| 子命令 | 用途 |
|--------|------|
| `check-login` | 检查当前登录状态 |
| `get-qrcode` | 获取二维码图片（非阻塞） |
| `wait-login` | 等待扫码完成（阻塞） |
| `send-code --phone` | 发送手机验证码 |
| `verify-code --code` | 提交验证码完成登录 |
| `delete-cookies` | 退出登录并清除 cookies |
| `add-account --name` | 添加命名账号（自动分配端口） |
| `list-accounts` | 列出所有命名账号及端口 |
| `remove-account --name` | 删除命名账号 |
| `set-default-account --name` | 设置默认账号 |

## 账号选择（前置步骤）

> **例外**：用户要求"添加账号/列出账号/删除账号/设置默认账号"时，**跳过此步骤**。

其余操作先运行：
```bash
python scripts/cli.py list-accounts
```

根据返回的 `count`：
- **0 个命名账号**：直接使用默认账号
- **1 个命名账号**：告知用户，直接加 `--account <名称>`
- **多个命名账号**：向用户展示列表，询问操作哪个账号

## 工作流程

### 第一步：检查登录状态

```bash
python scripts/cli.py check-login
```

输出解读：
- `"logged_in": true` → 已登录，可执行后续操作
- `"logged_in": false` + `"login_method": "qrcode"` → 走二维码方式
- `"logged_in": false` + `"login_method": "both"` → 无界面服务器，需询问用户选择方式

### 第二步：选择登录方式

#### 方式 A：二维码登录

**第一步** — 从 `check-login` 返回的 JSON 取 `qrcode_image_url`，展示给用户：

```
请使用小红书 App 扫描以下二维码登录：

![小红书登录二维码]({qrcode_image_url})

您也可以在手机浏览器中直接访问此链接完成登录：
{xr_login_url}
```

**第二步** — 等待登录完成：

```bash
python scripts/cli.py wait-login
```

#### 方式 B：手机验证码登录

**第一步** — 向用户确认手机号：

> "请提供您要登录的手机号（不含国家码，如 13800138000）"

收到手机号后，发送验证码：

```bash
python scripts/cli.py send-code --phone <用户确认的手机号>
```

**第二步** — 向用户询问验证码：

```bash
python scripts/cli.py verify-code --code <用户提供的6位验证码>
```

## 清除 Cookies（切换账号/退出登录）

```bash
python scripts/cli.py delete-cookies
python scripts/cli.py --account work delete-cookies  # 指定账号
```

## 多账号工作流

### 添加账号

```bash
python scripts/cli.py add-account --name work --description "工作号"
python scripts/cli.py add-account --name personal
```

### 使用指定账号

```bash
python scripts/cli.py --account work check-login
python scripts/cli.py --account work get-qrcode
python scripts/cli.py --account personal check-login
```

### 管理账号

```bash
python scripts/cli.py list-accounts                      # 列出所有
python scripts/cli.py set-default-account --name work    # 设置默认
python scripts/cli.py remove-account --name personal      # 删除
```

## 失败处理

- **Chrome 未找到**：提示用户安装 Google Chrome 或设置 `CHROME_BIN` 环境变量
- **登录弹窗未出现**：等待 15 秒超时，重试
- **验证码错误**：重新运行 `verify-code --code <新验证码>`
- **二维码超时**：重新执行 `get-qrcode` 获取新二维码
- **远程 CDP 连接失败**：检查 Chrome 是否已开启 `--remote-debugging-port`

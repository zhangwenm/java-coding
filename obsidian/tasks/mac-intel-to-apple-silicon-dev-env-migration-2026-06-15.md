# Intel Mac 到 Apple Silicon 开发环境迁移方案

创建时间：2026-06-15  
源机器：Intel Mac，`x86_64`，macOS `26.5`  
目标机器：Apple Silicon Mac，按 ARM 原生环境重装

## 结论

不要整盘复制旧机器开发环境。当前机器是 Intel 架构，Homebrew 位于 `/usr/local`，Java、pyenv、uv 管理的部分运行时也是 x86_64。迁移到 M 系列时应采用“清单化重装 + 精选配置迁移 + 项目源码迁移”的方式。

如果目标是“应用、配置、聊天记录、钥匙串尽量完整”，首选混合方案：

```text
迁移助理负责：用户账户、应用、应用配置、钥匙串、聊天记录、桌面/文稿/下载等用户数据
后续清理负责：删除/绕开 Intel 残留，重装 ARM Homebrew、Java、Node、Python、Docker 等开发环境
```

这比纯手工迁移更完整，也比迁移助理后直接使用更适合开发机。

优先目标：

1. 在新机安装 ARM Homebrew 到 `/opt/homebrew`。
2. 按清单重装 CLI、语言运行时和 GUI 应用。
3. 只迁移必要配置，不迁移缓存、虚拟环境、node_modules、容器 VM。
4. API key、私钥、token 类内容只在确认后迁移，不写入脚本和文档。

## 首选方案：迁移助理 + 开发环境重装

适用目标：希望应用迁移更全面，同时又不让开发环境被 Intel 残留污染。

### 阶段 0：旧机准备

1. 更新旧机 macOS 到可用的最新版本。
2. 退出或记录关键软件授权状态：
   - JetBrains
   - Microsoft 365
   - Navicat
   - VPN/远控软件
   - AI 工具账号
3. 确认代理工具可用性。当前旧机 shell 代理指向 `127.0.0.1:7898`，但 Homebrew 访问时该端口不可用。
4. 做一次 Time Machine 备份，作为回滚兜底。
5. 记录旧机 SSH 公钥，确认 GitHub/GitLab/Gitee 等平台可用。

建议旧机迁移前执行：

```bash
uname -m
sw_vers
du -sh ~/Desktop ~/Documents ~/Downloads /Users/admin/appstore/project /Users/admin/appstore/work
```

### 阶段 1：用迁移助理做完整用户迁移

在新 M 系列 Mac 初次开机设置时，选择：

```text
从 Mac、时间机器备份或启动磁盘迁移
```

建议勾选：

```text
用户账户
应用程序
其他文件和文件夹
系统与网络设置
```

迁移助理更可能带过去的内容：

- 应用偏好设置
- 钥匙串
- 浏览器书签/密码/扩展
- Office、聊天软件、网盘软件的本地状态
- 微信/企业微信等应用数据
- 桌面、文稿、下载、图片、音乐等用户数据
- `~/Library/Application Support` 下的大量应用配置

迁移助理仍可能无法完整处理的内容：

- 软件授权激活
- 企业 VPN 设备认证
- 远控软件设备绑定
- 数据库连接密码
- GitHub CLI token
- AI 工具 auth
- Docker/Colima VM
- Homebrew Intel 路径
- x86_64 JDK/Python/Node 运行时

### 阶段 2：迁移后先不要直接开发

迁移完成后，先做只读检查：

```bash
uname -m
which brew
brew --prefix
echo $PATH
/usr/libexec/java_home -V
node -v
python3 --version
docker version
```

重点判断：

| 检查项 | 期望 | 如果不是 |
|---|---|---|
| `uname -m` | `arm64` | 说明当前不是 M 系列环境或终端在 Rosetta 下 |
| `which brew` | `/opt/homebrew/bin/brew` | 如果是 `/usr/local/bin/brew`，说明仍在用 Intel Homebrew |
| `PATH` | `/opt/homebrew/bin` 靠前 | 如果 `/usr/local/bin` 靠前，需调整 shell |
| Java | ARM JDK 8/11/17 可选 | 如果只看到 x86_64 Oracle JDK，需重装 |
| Docker | compose 可用 | 不可用则补 Docker Desktop 或 compose plugin |

### 阶段 3：清理 Intel 开发环境残留

不要立刻删除 `/usr/local`，先绕开：

1. 新装 ARM Homebrew 到 `/opt/homebrew`。
2. 调整 `.zprofile`：

```bash
eval "$(/opt/homebrew/bin/brew shellenv)"
```

3. 确认 `/opt/homebrew/bin` 在 PATH 前面。
4. 暂时保留 `/usr/local`，等项目全部验证后再决定是否清理。

需要重点清理或重建：

- `/usr/local/bin/brew`
- `/usr/local/Cellar`
- `/usr/local/lib/node_modules`
- x86_64 JDK 路径
- `~/.pyenv/versions/3.6.9`，如果从旧机迁过来就是 x86_64
- `~/.local/share/uv/python` 中 x86_64 Python
- `.zshrc` 中写死的 `127.0.0.1:7898` 代理
- 旧 `JAVA_HOME`、`MAVEN_HOME`、`NODE_PATH`

### 阶段 4：重装 ARM 开发环境

按下面顺序执行：

```bash
xcode-select --install
```

```bash
brew install \
  gh git-filter-repo ripgrep tmux pandoc nginx \
  node pyenv python@3.11 python@3.13 \
  colima docker docker-buildx docker-compose \
  qemu coreutils openssl@3 pkgconf
```

```bash
brew install --cask temurin@8 temurin@11 temurin@17
brew install maven
```

```bash
npm config set registry https://registry.npmmirror.com/
npm install -g @openai/codex @vue/cli n obsidian-mcp tdesign-starter-cli
curl -fsSL https://bun.sh/install | bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

Python 3.6.9 需要按项目实际情况决定：

```bash
pyenv install 3.6.9
pyenv global 3.6.9
```

如果 Apple Silicon 上构建失败，优先考虑：

- 用容器跑旧 Python 项目
- 用 Rosetta 单独保留兼容环境
- 升级项目 Python 版本

### 阶段 5：应用配置修复

迁移助理已经尽量迁了应用配置，但这些仍需逐项检查：

| 应用类型 | 迁移后动作 |
|---|---|
| JetBrains | 重新登录账号，检查 JDK/Maven/Python 解释器路径 |
| Cursor/Windsurf | 检查终端 PATH 是否指向 `/opt/homebrew` |
| Postman | 检查 Environment 中 token 是否存在且未过期 |
| DBeaver/Navicat | 连接密码可能需要重新输入 |
| WeChat/企业微信 | 检查聊天记录是否完整 |
| OneDrive | 检查同步目录，避免重复下载 |
| Clash/VPN | 重新确认代理端口，不直接信任旧 `127.0.0.1:7898` |
| Tunnelblick/aTrust | 可能需要重新安装系统扩展或重新设备认证 |
| ToDesk/AweSun | 新设备码需要重新绑定 |
| Chrome/Safari | 检查同步、密码、扩展 |
| Codex/Claude | 认证重新登录，配置和 skills 可保留 |

### 阶段 6：项目级验收

至少验证这些项目/工具：

```bash
cd /Users/admin/appstore/project
git status
```

Java 项目：

```bash
/Users/admin/appstore/apache-maven-3.8.5/bin/mvn -version
/Users/admin/appstore/apache-maven-3.8.5/bin/mvn clean compile
```

前端项目：

```bash
node -v
npm -v
npm run build
```

容器：

```bash
docker version
docker compose version
colima status
```

SSH/GitHub：

```bash
ssh -T git@github.com
gh auth status
```

### 混合方案优缺点

优点：

- 应用、用户配置、钥匙串、聊天记录迁得最全。
- 避免纯手工遗漏微信、Office、浏览器、数据库 GUI、VPN、远控等应用状态。
- 开发环境最终仍按 Apple Silicon 原生方式重建。

缺点：

- 会带入 Intel 残留，需要迁移后清理。
- 旧代理、旧 PATH、旧 JDK/Python/Node 路径可能污染 shell。
- 某些授权和企业设备认证仍需重新登录。

结论：这是当前机器最推荐的路线。

## 当前环境盘点

### 系统与包管理

- macOS：`26.5`，Build `25F71`
- 架构：`x86_64`
- Homebrew：`/usr/local/bin/brew`
- Homebrew taps：
  - `antoniorodr/memo`
  - `farion1231/ccswitch`
  - `homebrew/services`
  - `oven-sh/bun`

重要问题：

- 当前 shell 中存在 `http_proxy`、`https_proxy`、`all_proxy` 指向 `127.0.0.1:7898`。
- Homebrew 执行时因该代理不可用出现 curl 失败。
- `.zprofile` 中配置了 Homebrew USTC 镜像。
- 新机安装前必须先确认代理是否可用；不可直接照搬旧 `.zshrc` 的代理行。

### Homebrew formula

建议新机重装的核心 formula：

```bash
brew install \
  colima docker docker-buildx docker-compose \
  gh git-filter-repo \
  node pyenv python@3.11 python@3.13 \
  ripgrep tmux nginx pandoc qemu \
  coreutils openssl@3 pkgconf
```

当前旧机还装有：

```text
ada-url autoconf brotli c-ares ca-certificates capstone dtc fmt fortune gettext glib gmp gnutls hdrhistogram_c icu4c@78 jpeg-turbo libevent libffi libidn2 libnghttp2 libnghttp3 libngtcp2 libpng libslirp libssh libtasn1 libtommath libunistring libusb libuv libyaml lima llhttp lz4 lzo m4 memo merve mpdecimal nbytes ncurses nettle opencode p11-kit pcre2 pixman simdjson simdutf sl snappy sqlite tcl-tk utf8proc uvwasi vde xz zlib zstd
```

这些多数是依赖包，不必手工逐个安装，让 Homebrew 自动解析即可。

### Homebrew cask / GUI 应用

通过 Homebrew cask 安装的应用：

- `cc-switch` `3.14.1`
- `claude-code@latest` `2.1.175`
- `font-jetbrains-mono-nerd-font` `3.4.0`
- `ghostty` `1.3.1`
- `tabby` `1.0.234`

建议新机安装：

```bash
brew install --cask \
  ghostty tabby font-jetbrains-mono-nerd-font
```

`cc-switch`、`claude-code@latest` 需要按实际使用方式确认是否继续用 cask 安装。

当前 `/Applications` 主要应用：

```text
Cursor, IntelliJ IDEA, PyCharm, DBeaver, Navicat Premium, Postman,
Obsidian, Ghostty, Tabby, Google Chrome, Claude, Windsurf,
WeChat, 企业微信, Lark, OneDrive, Office, Clash Verge,
ShadowsocksX-NG-R8, Tunnelblick, ToDesk, AweSun, MQTTX, Xmind
```

建议新机手动或通过厂商安装器安装 JetBrains、Office、VPN、远控、聊天软件和数据库 GUI。授权类软件不要直接复制 `.app`。

## 应用迁移矩阵

原则：M 系列新机优先安装 Apple Silicon 原生版本。不要把 `/Applications/*.app` 直接复制过去作为主要迁移方式，因为授权、后台服务、登录项、驱动、系统扩展、钥匙串项和架构依赖很容易不完整。

迁移顺序：

1. 先安装应用本体。
2. 再迁移配置、项目、连接信息或聊天数据。
3. 最后重新登录账号和授权。
4. VPN、远控、数据库连接、AI 工具认证单独验证。

### 开发 IDE / 编辑器

| 应用 | 新机安装方式 | 迁移内容 | 不建议迁移 | 验收 |
|---|---|---|---|---|
| Cursor | 官网或 Homebrew cask，安装 ARM 版 | Settings Sync、扩展、`~/Library/Application Support/Cursor/User`，确认后迁移 | 旧 x86_64 app、本地缓存 | 能打开项目、扩展正常、终端 PATH 正常 |
| IntelliJ IDEA | JetBrains Toolbox 或官网下载 ARM 版 | JetBrains 账号同步、设置导出包、项目配置 | 破解/旧 VM options、旧 app | 能打开 Java 项目，JDK/Maven 识别正常 |
| PyCharm | JetBrains Toolbox 或官网下载 ARM 版 | JetBrains 账号同步、解释器配置需重建 | 旧 venv、旧 x86_64 解释器路径 | 能识别新 pyenv/uv Python |
| Windsurf | 官网安装 ARM 版 | 账号登录、扩展和用户设置 | 旧 app/cache | 能打开项目并运行终端 |
| Sublime Text | 官网安装或 cask | `~/Library/Application Support/Sublime Text`，只迁 Package/User 配置 | Cache/Index | 插件和主题正常 |
| QoderWork | 官网安装 | 账号和项目配置，按厂商同步为准 | 旧 app 直接复制 | 能登录并打开项目 |

建议操作：

```bash
brew install --cask cursor sublime-text
```

JetBrains 系列建议用 Toolbox 统一安装和登录，避免手工复制 IDE 配置造成路径污染。

### 终端 / Shell / CLI 应用

| 应用 | 新机安装方式 | 迁移内容 | 注意事项 |
|---|---|---|---|
| Ghostty | `brew install --cask ghostty` | `~/.config/ghostty` | 当前终端正在用 Ghostty，配置可迁 |
| Tabby | `brew install --cask tabby` | Tabby 配置、连接配置需确认是否含密码 | 不要明文导出密码 |
| CC Switch | cask 或项目来源重装 | 配置文件按工具文档迁移 | 先确认新机是否继续使用 |
| Claude Code URL Handler | 随 Claude Code 安装 | 不单独迁移 | 新机重装 Claude Code 后验证 |
| opencode | `brew install opencode` | `~/.config/opencode` | 认证/token 单独处理 |
| Codex CLI | `npm install -g @openai/codex` | 精选迁移 `~/.codex/AGENTS.md`、`config.toml`、skills、plugins | 不迁 `auth.json`、history/log/session |

建议迁移：

```bash
rsync -av ~/.config/ghostty NEW_MAC:~/.config/
rsync -av ~/.config/opencode NEW_MAC:~/.config/
```

Codex/Claude 配置只迁精选文件，不整目录复制。

### 数据库 / API / 调试工具

| 应用 | 新机安装方式 | 迁移内容 | 注意事项 |
|---|---|---|---|
| DBeaver | `brew install --cask dbeaver-community` 或官网下载 | 工作区、连接配置，可用 DBeaver 导出/导入 | 密码通常在安全存储中，可能需重新输入 |
| Navicat Premium | 官网安装 ARM/Universal 版 | 用 Navicat 的导出连接功能或账号同步 | 授权需重新激活，密码不要明文落盘 |
| Another Redis Desktop Manager | 官网/项目 Release | 连接配置导出，如果支持 | Redis 密码需重新确认 |
| Postman | 官网或 cask | 登录账号同步 Workspace；也可导出 Collection/Environment | Environment 可能含 token，导出前脱敏 |
| MQTTX | 官网或 cask | 连接配置导出/导入 | 连接密码需确认 |
| SwitchHosts | 官网/Release | hosts 方案导出或迁 `~/Library/Application Support/SwitchHosts` | 系统 hosts 修改需管理员权限 |

建议优先用应用内导出：

- Postman：登录账号同步，或导出 Collection/Environment。
- DBeaver/Navicat：导出连接配置，密码重新输入。
- SwitchHosts：导出 hosts 方案。

不建议直接复制整个 `~/Library/Application Support` 下的数据库工具目录，容易带入旧架构插件、缓存和失效钥匙串引用。

### 浏览器 / 文档 / 笔记

| 应用 | 新机安装方式 | 迁移内容 | 注意事项 |
|---|---|---|---|
| Google Chrome | `brew install --cask google-chrome` | 登录 Google 账号同步书签、扩展、密码 | 不建议直接复制 Chrome Profile |
| Safari | 系统自带 | iCloud 同步书签、密码 | 确认 iCloud 钥匙串 |
| Obsidian | 官网或 cask | Vault：`/Users/admin/appstore/project/java-coding/obsidian` | 插件配置随 vault 迁移，账号/token 单独确认 |
| Xmind | 官网安装 | 文档文件、账号同步 | 不复制 app |
| Office | 微软官网/App Store | OneDrive/账号同步文档 | 重新登录激活 |
| OneNote | 微软账号同步 | 云笔记本 | 本地缓存不迁 |

Obsidian 迁移命令：

```bash
rsync -av /Users/admin/appstore/project/java-coding/obsidian NEW_MAC:/Users/admin/appstore/project/java-coding/
```

如果整个项目目录会迁移，Obsidian vault 已包含在 `/Users/admin/appstore/project` 中，不需要重复迁。

### 通讯 / 协作 / 社交

| 应用 | 新机安装方式 | 迁移内容 | 注意事项 |
|---|---|---|---|
| WeChat | 官网/App Store | 微信内置迁移或重新登录同步 | 聊天记录不要靠 Finder 直接复制 |
| 企业微信 | 官网/App Store | 企业微信内置聊天记录迁移/重新登录 | 企业策略可能限制迁移 |
| Lark | 官网安装 | 账号云同步 | 本地缓存不迁 |
| Telegram | 官网/App Store | 云同步 | Secret Chat 不跨设备同步 |
| TencentMeeting | 官网安装 | 账号登录 | 无需迁本地缓存 |

微信类重点：

1. 在旧机微信内使用聊天记录迁移/备份能力。
2. 新机登录后按应用提示恢复。
3. 不建议直接复制 `~/Library/Containers` 或 `~/Library/Application Support` 中的微信目录。

### 网盘 / 下载 / 文件工具

| 应用 | 新机安装方式 | 迁移内容 | 注意事项 |
|---|---|---|---|
| OneDrive | 官网/App Store | 重新登录并选择同步目录 | 避免重复下载造成空间占用 |
| BaiduNetdisk_mac | 官网安装 | 重新登录 | 本地下载目录按需迁 |
| Motrix | 官网/Release | 下载任务配置可选迁移 | 未完成任务建议重新添加 |
| Keka | 官网/App Store/cask | 偏好设置可不迁 | 轻量应用，重装即可 |
| NeteaseMusic | 官网/App Store | 登录账号同步歌单 | 本地下载音乐按需迁 |

下载目录建议按数据迁移，不按应用迁移：

```bash
rsync -av ~/Downloads NEW_MAC:~/
```

如下载目录很大，先用 `du -sh ~/Downloads` 确认体量。

### VPN / 代理 / 远控 / 网络工具

| 应用 | 新机安装方式 | 迁移内容 | 注意事项 |
|---|---|---|---|
| Clash Verge | 官网/Release | 配置订阅、规则、profiles | 当前 shell 代理 `127.0.0.1:7898` 依赖它或同类代理，必须验证端口 |
| ShadowsocksX-NG-R8 | 官网/Release | 节点配置，建议重新导入订阅 | 不明文扩散节点密码 |
| Tunnelblick | 官网安装 | `.ovpn` 配置和证书 | 证书/私钥敏感，需确认后迁 |
| aTrustInstaller | 企业安装包 | 企业 VPN/零信任配置 | 通常需要重新安装并设备认证 |
| ToDesk | 官网安装 | 账号登录 | 设备码变化，需重新授权 |
| AweSun | 官网安装 | 账号登录 | 设备码变化，需重新授权 |
| SwitchHosts | Release 安装 | hosts 方案导出/导入 | 修改 hosts 需管理员权限 |

代理迁移规则：

- 不要把 `.zshrc` 中的 `127.0.0.1:7898` 默认写死到新机。
- 新机先装代理应用，确认本地端口后再配置 shell 代理。
- 推荐改成开关函数，例如 `proxy_on` / `proxy_off`。

VPN 迁移规则：

- `.ovpn`、证书、企业 VPN 配置属于敏感数据。
- 迁移前先确认是否允许复制到新设备。
- 企业 VPN 通常需要新设备重新注册。

### AI / 助手 / 自动化工具

| 应用 | 新机安装方式 | 迁移内容 | 不迁移 |
|---|---|---|---|
| Claude.app | 官网安装 | 账号重新登录 | 旧 app/cache |
| Claude Code | cask/npm/官方方式 | `~/.claude/AGENTS.md`、`CLAUDE.md`、settings、skills、plugins | `history.jsonl`、sessions、transcripts、auth |
| Codex CLI | npm 全局安装 | `~/.codex/AGENTS.md`、`config.toml`、skills、plugins | `auth.json`、logs、history、state sqlite |
| Windsurf / Cursor | 官网安装 | 账号同步、扩展设置 | 旧 app/cache |

AI 工具迁移原则：

- 认证重新登录。
- skills、规则、项目上下文可以迁。
- 历史、日志、会话、auth 文件不迁。

### 其他应用

| 应用 | 新机安装方式 | 迁移方式 |
|---|---|---|
| Microsoft Excel/Word/PowerPoint/Outlook | Microsoft 365 官网/App Store | 登录账号重新激活；Outlook 邮箱重新同步 |
| Microsoft OneNote | Microsoft 365/App Store | 云同步 |
| Keka | 官网/App Store | 重装即可 |
| Xmind | 官网/App Store | 文档文件迁移或账号同步 |
| lghub | Logitech 官网 | 重装，设备配置按账号/设备同步情况确认 |
| hisuite | 华为官网 | 如还需要则重装 |
| Safari | 系统自带 | iCloud 同步 |

## 应用迁移执行清单

### 第一批：必须先装

```text
Ghostty/Tabby
Google Chrome
Cursor/Windsurf
IntelliJ IDEA/PyCharm
Obsidian
Clash Verge 或实际代理工具
Postman
DBeaver/Navicat
WeChat/企业微信/Lark
OneDrive
```

### 第二批：配置迁移

```bash
rsync -av ~/.ssh NEW_MAC:~/
rsync -av ~/.gitconfig NEW_MAC:~/
rsync -av ~/.config/ghostty NEW_MAC:~/.config/
rsync -av ~/.config/opencode NEW_MAC:~/.config/
rsync -av /Users/admin/appstore/project NEW_MAC:/Users/admin/appstore/
rsync -av /Users/admin/appstore/work NEW_MAC:/Users/admin/appstore/
```

需要确认后再迁：

```text
~/.npmrc
Tunnelblick .ovpn / 证书
Clash/Shadowsocks 节点配置
数据库 GUI 连接配置
AI 工具 settings.local/auth 类文件
```

### 第三批：应用内导出/导入

```text
Postman Collections / Environments
DBeaver connections
Navicat connections
SwitchHosts profiles
微信/企业微信聊天记录
Tunnelblick VPN profiles
浏览器书签/密码，优先用账号或 iCloud/Google 同步
```

### 第四批：重新登录和授权

```text
JetBrains
Microsoft 365
Navicat
GitHub CLI
Claude / Codex / Cursor / Windsurf
WeChat / 企业微信 / Lark / Telegram
OneDrive / 百度网盘
VPN / 远控软件
```

## 应用迁移验收

| 类别 | 验收动作 |
|---|---|
| IDE | 打开 `/Users/admin/appstore/project` 中至少一个 Java 项目和一个前端项目 |
| JetBrains | 确认 JDK 8/11/17、Maven、项目索引正常 |
| Cursor/Windsurf | 终端 PATH 正常，能识别 `node`、`java`、`mvn` |
| Obsidian | 打开 vault，插件正常，能看到本迁移文档 |
| Postman | Collection/Environment 可用，敏感变量已重新填入 |
| DBeaver/Navicat | 能连接测试库，密码通过安全方式重新录入 |
| VPN/代理 | Clash/Tunnelblick 可连接，shell 代理开关有效 |
| SSH/Git | `ssh -T git@github.com` 和 `gh auth status` 正常 |
| 微信/企业微信 | 聊天记录恢复或确认无需恢复 |
| OneDrive/Office | 文件同步、Office 激活正常 |

### Java / Maven

当前 Java：

- JDK 17：Oracle `17.0.9`，默认
- JDK 11：Oracle `11.0.23`
- JDK 8：Oracle `1.8.0_361`
- Java Applet Plugin：`1.8.361.09`

当前 Maven：

- 主用：`/Users/admin/appstore/apache-maven-3.8.5`
- 另有：`/Users/admin/appstore/apache-maven-3.6.3`
- `~/.m2`：约 `2.1G`
- 未发现 `~/.m2/settings.xml`

新机建议：

```bash
brew install --cask temurin@8 temurin@11 temurin@17
brew install maven
```

如果项目强依赖 Maven `3.8.5`，再单独把 Maven 3.8.5 解压到：

```text
/Users/admin/appstore/apache-maven-3.8.5
```

不建议迁移：

- `~/.m2/repository`

建议迁移：

- `~/.m2/wrapper`
- 如果后续发现 `settings.xml`，只迁移该文件，不迁仓库缓存。

### Node / npm / Bun

当前：

- Node：`v26.3.0`
- npm：`11.16.0`
- npm registry：`https://registry.npmmirror.com/`
- Bun：`1.3.12`

全局 npm 包：

```text
@openai/codex@0.139.0
@vue/cli@5.0.8
n@9.2.0
obsidian-mcp@1.0.6
tdesign-starter-cli@0.3.3
npm@11.16.0
```

新机建议：

```bash
brew install node
npm config set registry https://registry.npmmirror.com/
npm install -g @openai/codex @vue/cli n obsidian-mcp tdesign-starter-cli
curl -fsSL https://bun.sh/install | bash
```

注意：

- 不迁移项目里的 `node_modules`。
- `.npmrc` 当前存在，权限 `600`，可能含 token；只在确认后复制，不在终端输出内容。
- npm 配置存在废弃项 `email`、`always-auth`，迁移后建议清理。

### Python / uv

当前：

- 系统 Python：`3.9.6`
- Homebrew Python：`3.11.12`、`3.13.12`
- pyenv：默认 `3.6.9`
- uv：`0.10.9`
- uv 管理的 Python 指向 x86_64 路径

新机建议：

```bash
brew install pyenv python@3.11 python@3.13
curl -LsSf https://astral.sh/uv/install.sh | sh
pyenv install 3.6.9
pyenv global 3.6.9
```

注意：

- Python `3.6.9` 很旧，Apple Silicon 上构建可能需要额外 openssl/readline/zlib 依赖。
- 旧机器的虚拟环境不要迁移，按项目 `requirements.txt`、`pyproject.toml` 重建。
- `~/.local/bin/uv` 是 x86_64 版本，新机必须重装。

### Docker / Colima / Lima

当前：

- Docker CLI：`29.5.2`
- `docker compose` 不可用
- Colima 未运行
- Lima 无实例

新机建议二选一：

方案 A：Docker Desktop，适合少折腾。

方案 B：Colima，适合轻量 CLI：

```bash
brew install colima docker docker-buildx docker-compose
colima start --cpu 4 --memory 8 --disk 80
docker version
docker compose version
```

不迁移：

- 旧机 Colima/Lima VM
- Docker 镜像缓存

需要保留的镜像用：

```bash
docker save IMAGE:TAG | gzip > image.tar.gz
gzip -dc image.tar.gz | docker load
```

### 数据库工具

PATH 中未发现：

- `mysql`
- `psql`
- `redis-cli`

但 GUI 应用存在：

- DBeaver
- Navicat Premium
- Another Redis Desktop Manager

新机建议安装 CLI：

```bash
brew install mysql-client postgresql@16 redis
```

数据库连接密码不写入迁移脚本，优先从 GUI 自带同步、钥匙串或手动录入。

### Xcode / 编译工具

当前只安装 Command Line Tools：

```text
/Library/Developer/CommandLineTools
```

完整 Xcode 未安装，`xcodebuild -version` 不可用。

新机建议：

```bash
xcode-select --install
```

如果需要 iOS/macOS App 开发，再安装完整 Xcode。

### SSH / Git / GitHub CLI

当前 `~/.ssh`：

- `id_ed25519`
- `id_rsa`
- 对应公钥
- `config`
- `known_hosts`

权限状态：

- `~/.ssh` 为 `700`
- 私钥为 `600`
- 公钥为 `644`

新机可迁移，但必须保持权限：

```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_ed25519 ~/.ssh/id_rsa
chmod 644 ~/.ssh/*.pub
```

GitHub CLI：

- 当前 `gh auth status` 显示 token 已失效。
- 新机不要复制旧 gh token，直接重新登录：

```bash
gh auth login -h github.com
```

### Codex / Claude / AI 工具

当前：

- `~/.codex` 约 `226M`
- `~/.claude` 约 `2.0G`
- `~/.agents` 存在共享 skills
- 全局 npm 包有 `@openai/codex@0.139.0`
- Homebrew 有 `opencode@1.17.3`
- cask 有 `claude-code@latest@2.1.175`

建议迁移：

- `~/.codex/AGENTS.md`
- `~/.codex/config.toml`，先确认不含敏感 token
- `~/.codex/skills`
- `~/.codex/plugins`
- `~/.claude/AGENTS.md`
- `~/.claude/CLAUDE.md`
- `~/.claude/settings.json`
- `~/.claude/settings.local.json`，先确认不含敏感 token
- `~/.claude/skills`
- `~/.agents/skills`

不建议迁移：

- `history.jsonl`
- `logs_*.sqlite`
- `sessions`
- `transcripts`
- `cache`
- `paste-cache`
- `auth.json`

理由：这些目录含历史、日志、认证和缓存，体积大且敏感。

### shell 配置

当前存在：

- `~/.zshrc`
- `~/.zprofile`
- `~/.gitconfig`
- `~/.npmrc`

已知内容类别：

- `.zshrc` 包含代理 `127.0.0.1:7898`
- `.zshrc` 包含 Bun 配置
- `.zshrc` 包含 API key 类环境变量
- `.zprofile` 包含 Homebrew USTC 镜像配置

新机建议：

1. 先生成干净 `.zshrc`。
2. 只合并 PATH、Bun、pyenv、Java 切换函数。
3. 代理单独做开关函数，不默认开启。
4. API key 不写入 `.zshrc`，改用单独未纳入同步的本地 secrets 文件或系统钥匙串。

## 迁移分层

### 必须重装

- Homebrew ARM 版
- JDK 8/11/17 ARM 版
- Maven
- Node/npm
- Bun
- pyenv / Python / uv
- Docker Desktop 或 Colima
- GUI 应用
- Xcode Command Line Tools
- JetBrains IDE

### 可以复制

- `~/.ssh`，需确认私钥迁移风险
- `~/.gitconfig`
- 精简后的 `~/.zshrc`、`~/.zprofile`
- `~/.npmrc`，需确认 token
- `~/.m2/wrapper`
- 项目源码：`/Users/admin/appstore/project`
- 工作目录：`/Users/admin/appstore/work`
- Obsidian vault：`/Users/admin/appstore/project/java-coding/obsidian`
- AI 工具的配置文件和 skills

### 不建议复制

- `/usr/local`
- `~/.m2/repository`
- `node_modules`
- Python venv
- uv 下载的 x86_64 Python
- Colima/Lima VM
- Docker 镜像缓存
- Homebrew Cellar
- `.codex` / `.claude` 的 auth、history、logs、sessions、transcripts、cache
- `/Users/admin/appstore/protoc-3.20.0-osx-x86_64`
- x86_64 专用二进制

### 需要人工确认

- `.npmrc` token
- SSH 私钥是否复制到新机
- API key 类环境变量
- VPN 配置
- JetBrains/Office/Navicat 授权
- Clash/代理配置
- 数据库 GUI 连接配置

## 新机安装顺序

### 1. 基础系统

```bash
xcode-select --install
```

安装 Homebrew ARM 版后确认：

```bash
which brew
brew --version
uname -m
```

期望：

```text
/opt/homebrew/bin/brew
arm64
```

### 2. Homebrew 镜像和代理

如果新机在国内网络，可先配置镜像：

```bash
export HOMEBREW_API_DOMAIN=https://mirrors.ustc.edu.cn/homebrew-bottles/api
export HOMEBREW_BOTTLE_DOMAIN=https://mirrors.ustc.edu.cn/homebrew-bottles/bottles
```

不要默认写入旧机的：

```text
127.0.0.1:7898
```

除非新机确认代理服务已启动。

### 3. CLI 和开发工具

```bash
brew tap homebrew/services
brew tap oven-sh/bun
brew install \
  gh git-filter-repo ripgrep tmux pandoc nginx \
  node pyenv python@3.11 python@3.13 \
  colima docker docker-buildx docker-compose \
  qemu coreutils openssl@3 pkgconf
```

### 4. GUI 应用

```bash
brew install --cask \
  ghostty tabby font-jetbrains-mono-nerd-font \
  google-chrome visual-studio-code cursor obsidian postman dbeaver-community
```

JetBrains、Office、Navicat、VPN、远控、聊天软件建议手动从官网或企业渠道安装。

### 5. Java / Maven

```bash
brew install --cask temurin@8 temurin@11 temurin@17
brew install maven
```

如需固定 Maven 3.8.5：

```bash
mkdir -p /Users/admin/appstore
```

然后把 Maven 3.8.5 解压到：

```text
/Users/admin/appstore/apache-maven-3.8.5
```

### 6. Node / npm

```bash
npm config set registry https://registry.npmmirror.com/
npm install -g @openai/codex @vue/cli n obsidian-mcp tdesign-starter-cli
```

### 7. Python / Bun / uv

```bash
curl -fsSL https://bun.sh/install | bash
curl -LsSf https://astral.sh/uv/install.sh | sh
pyenv install 3.6.9
pyenv global 3.6.9
```

如果 `pyenv install 3.6.9` 在 Apple Silicon 上失败，优先检查 openssl/readline/zlib 依赖，再决定是否用 Rosetta 或容器保留旧项目运行环境。

### 8. 容器

Colima 方案：

```bash
colima start --cpu 4 --memory 8 --disk 80
docker version
docker compose version
```

Docker Desktop 方案：安装后直接验证 `docker version` 和 `docker compose version`。

### 9. 配置迁移

建议通过 `rsync` 精选迁移，不直接拖整个 home：

```bash
rsync -av ~/.gitconfig NEW_MAC:~/
rsync -av ~/.ssh NEW_MAC:~/
rsync -av ~/.m2/wrapper NEW_MAC:~/.m2/
rsync -av /Users/admin/appstore/project NEW_MAC:/Users/admin/appstore/
rsync -av /Users/admin/appstore/work NEW_MAC:/Users/admin/appstore/
```

迁移后修权限：

```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_ed25519 ~/.ssh/id_rsa
chmod 644 ~/.ssh/*.pub
```

## 新机验收命令

```bash
uname -m
sw_vers
which brew
brew --version
git --version
gh --version
java -version
/usr/libexec/java_home -V
mvn -version
node -v
npm -v
npm config get registry
python3 --version
python3.11 --version
python3.13 --version
pyenv versions
bun --version
uv --version
docker version
docker compose version
colima status
rg --version
tmux -V
nginx -v
ssh -T git@github.com
gh auth status
```

## 已发现风险与处理

| 风险 | 影响 | 处理 |
|---|---|---|
| Intel Homebrew 在 `/usr/local` | 不能直接用于 M 系列 | 新机重装 ARM Homebrew 到 `/opt/homebrew` |
| 旧代理 `127.0.0.1:7898` 不可用 | Homebrew/npm/curl 下载失败 | 新机先不默认启用代理，确认代理应用后再配置 |
| `.zshrc` 含 API key 类变量 | 明文泄露风险 | 不写入迁移脚本，改为本地 secrets 或钥匙串 |
| `gh` token 已失效 | GitHub CLI 不可用 | 新机重新 `gh auth login` |
| `docker compose` 不可用 | Compose 项目无法运行 | 新机补装 `docker-compose` 或 Docker Desktop |
| pyenv `3.6.9` 很旧 | Apple Silicon 构建可能失败 | 优先重建，失败再用 Rosetta/容器兜底 |
| `.codex`/`.claude` 体积大且含认证/日志 | 敏感且迁移噪声大 | 只迁配置、skills、plugins，不迁 auth/history/log/session |
| `/Users/admin/appstore/protoc-3.20.0-osx-x86_64` | x86_64 二进制 | 新机重装 ARM 版 protobuf |

## 推荐执行策略

第一阶段：只装基础环境和工具链，完成命令验收。  
第二阶段：迁移项目源码、Obsidian、SSH/Git 配置。  
第三阶段：逐个项目运行构建，缺什么补什么。  
第四阶段：迁移 GUI 应用配置和授权。  
第五阶段：确认新机可独立工作后，再考虑清理旧机。

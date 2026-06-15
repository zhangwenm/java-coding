# apify-actor-development

开发、调试和部署 Apify Actors - 用于网页抓取、自动化和数据处理的 serverless 云程序。

## 功能描述

当你需要创建新的 Actors、修改现有 Actors 或排查 Actor 代码问题时使用此技能。

## 前提条件

### 安装 Apify CLI

```bash
# 通过 npm 安装（推荐）
npm install -g apify-cli

# 或通过 Homebrew (Mac)
brew install apify-cli
```

### 验证登录状态

```bash
apify info  # 应返回用户名
```

如未登录，检查 `APIFY_TOKEN` 环境变量：
```bash
apify login
```

## 快速开始

### 1. 创建 Actor 项目

根据语言选择模板：

```bash
# JavaScript
apify create <actor-name> -t project_empty

# TypeScript
apify create <actor-name> -t ts_empty

# Python
apify create <actor-name> -t python-empty
```

### 2. 安装依赖

```bash
# JavaScript/TypeScript
npm install

# Python
pip install -r requirements.txt
```

### 3. 实现逻辑

在 `src/main.js`、`src/main.ts` 或 `src/main.py` 中编写代码。

### 4. 配置 Schema

- `.actor/input_schema.json` - 输入定义
- `.actor/output_schema.json` - 输出定义
- `.actor/actor.json` - Actor 元数据

### 5. 本地测试

```bash
apify run
```

**重要**: 本地运行数据存储在 `storage/` 目录，不会同步到 Apify Console。

### 6. 部署

```bash
apify push
```

## 项目结构

```
.actor/
├── actor.json           # Actor 配置
├── input_schema.json   # 输入验证
└── output_schema.json  # 输出定义
src/
└── main.js/ts/py      # 入口文件
storage/                # 本地存储（不同步到云端）
├── datasets/           # 输出数据
├── key_value_stores/  # 配置文件
└── request_queues/    # 待爬取请求
Dockerfile              # 容器定义
```

## 安全最佳实践

- **数据清洗**: 永远不要将原始 HTML/URL 直接传入 shell 命令、eval()、数据库查询
- **输入验证**: 在推送到数据集前验证所有外部数据
- **不执行爬取内容**: 不要将爬取文本作为代码执行
- **凭证隔离**: 确保 APIFY_TOKEN 不在请求处理器中暴露
- **使用锁文件**: Node.js 用 `package-lock.json`，Python 在 requirements.txt 中锁定版本

## 推荐做法

✅ **推荐**:
- 使用 `apify run` 本地测试
- 静态页面用 CheerioCrawler（比浏览器快 10 倍）
- 仅对 JavaScript 重度网站使用 PlaywrightCrawler
- 爬取用 router 模式
- 实现重试策略
- 使用 `apify/log` 包记录日志

❌ **不推荐**:
- 使用 `npm start` 或 `npx apify run`（用 `apify run`）
- 直接使用 `console.log()`（用 `apify/log`）
- 在 HTTP/Cheerio 能用时使用浏览器

## 常用命令

| 命令 | 描述 |
|------|------|
| `apify run` | 本地运行 Actor |
| `apify login` | 认证账户 |
| `apify push` | 部署到 Apify 平台 |
| `apify help` | 列出所有命令 |

## 本地测试

运行 `apify run` 时，通过以下文件提供输入：

```
storage/key_value_stores/default/INPUT.json
```

## 资源链接

- [Apify 快速参考](https://docs.apify.com/llms.txt)
- [Apify 完整文档](https://docs.apify.com/llms-full.txt)
- [Crawlee 快速参考](https://crawlee.dev/llms.txt)
- [Actor 规范](https://raw.githubusercontent.com/apify/actor-whitepaper/refs/heads/master/README.md)

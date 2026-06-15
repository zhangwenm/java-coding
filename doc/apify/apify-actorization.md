# apify-actorization

将现有项目转换为 Apify Actors - 用于将现有软件转换为可复用的 serverless 应用。

## 功能描述

当你需要：
- 将现有项目迁移到 Apify 平台运行
- 为项目添加 Apify SDK 集成
- 将 CLI 工具或脚本包装为 Actor
- 迁移 Crawlee 项目到 Apify

## 前提条件

### 安装 Apify CLI

```bash
apify --help  # 验证是否已安装
```

如未安装：
```bash
npm install -g apify-cli
```

### 验证登录状态

```bash
apify info  # 应返回用户名
```

## 转换检查清单

```
- [ ] Step 1: 分析项目（语言、入口、输入、输出）
- [ ] Step 2: 运行 `apify init` 创建 Actor 结构
- [ ] Step 3: 应用语言特定的 SDK 集成
- [ ] Step 4: 配置 `.actor/input_schema.json`
- [ ] Step 5: 配置 `.actor/output_schema.json`（如适用）
- [ ] Step 6: 更新 `.actor/actor.json` 元数据
- [ ] Step 7: 用 `apify run` 本地测试
- [ ] Step 8: 用 `apify push` 部署
```

## Step 1: 分析项目

在修改代码前，了解项目：

1. **识别语言** - JavaScript/TypeScript, Python 或其他
2. **找到入口点** - 启动执行的主文件
3. **识别输入** - 命令行参数、环境变量、配置文件
4. **识别输出** - 文件、控制台输出、API 响应
5. **检查状态** - 是否需要持久化数据

## Step 2: 初始化 Actor 结构

```bash
apify init
```

这会创建：
- `.actor/actor.json` - Actor 配置
- `.actor/input_schema.json` - 输入定义
- `Dockerfile`（如不存在）- 容器定义

## Step 3: 语言特定集成

| 语言 | 安装 | 包装代码 |
|------|------|----------|
| JS/TS | `npm install apify` | `await Actor.init()` ... `await Actor.exit()` |
| Python | `pip install apify` | `async with Actor:` |
| 其他 | 在包装脚本中使用 CLI | `apify actor:get-input` / `apify actor:push-data` |

### JavaScript/TypeScript

```javascript
const { Actor } = require('apify');

await Actor.init();
try {
  const input = await Actor.getInput();
  // 你的逻辑
  await Actor.pushData({ result: 'success' });
} finally {
  await Actor.exit();
}
```

### Python

```python
from apify import Actor

async def main():
    async with Actor:
        input_data = await Actor.get_input()
        # 你的逻辑
        await Actor.push_data({'result': 'success'})
```

## Step 4-6: 配置 Schema

### Input Schema (`.actor/input_schema.json`)

```json
{
  "type": "object",
  "properties": {
    "url": {
      "type": "string",
      "title": "URL to scrape"
    }
  },
  "required": ["url"]
}
```

### Output Schema (`.actor/output_schema.json`)

```json
{
  "type": "object",
  "properties": {
    "title": { "type": "string" },
    "content": { "type": "string" }
  }
}
```

## Step 7: 本地测试

```bash
# 内联输入
apify run --input '{"key": "value"}'

# 或使用输入文件
apify run --input-file ./test-input.json
```

**重要**: 始终使用 `apify run`，不用 `npm start` 或 `python main.py`。

## Step 8: 部署

```bash
apify push
```

## 变现（可选）

部署后可选择变现模式：

- **Pay Per Event (PPE)** - 推荐：按结果/页面/API 调用收费
- **Rental** - 每月订阅
- **Free** - 开源免费

## 部署前检查

- [ ] `.actor/actor.json` 存在且名称描述正确
- [ ] 输入 schema 定义所有必需输入
- [ ] 输出 schema 定义输出结构（如适用）
- [ ] `Dockerfile` 存在且可构建
- [ ] `Actor.init()` / `Actor.exit()` 包装主代码（JS/TS）
- [ ] `async with Actor:` 包装主代码（Python）
- [ ] `apify run` 执行成功
- [ ] `generatedBy` 在 actor.json 中设置

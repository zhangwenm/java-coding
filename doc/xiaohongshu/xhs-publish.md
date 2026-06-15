# xhs-publish

小红书内容发布技能。支持图文发布、视频发布、长文发布、定时发布、标签、可见性设置。

## 功能描述

当用户要求发布内容到小红书、上传图文、上传视频、发长文时触发。

## 允许使用的 CLI 子命令

| 子命令 | 用途 |
|--------|------|
| `fill-publish` | 填写图文表单（不发布） |
| `fill-publish-video` | 填写视频表单（不发布） |
| `publish` | 图文一步发布 |
| `publish-video` | 视频一步发布 |
| `click-publish` | 点击发布按钮 |
| `long-article` | 填写长文内容并触发排版 |
| `select-template` | 选择长文排版模板 |
| `next-step` | 进入长文发布页并填写描述 |
| `publish_pipeline.py` | 发布流水线（含图片下载） |

## 输入判断

1. 用户说"发长文 / 写长文 / 长文模式" → **长文发布流程（流程 B）**
2. 用户已提供 `标题 + 正文 + 视频` → **视频发布流程（流程 A.2）**
3. 用户已提供 `标题 + 正文 + 图片` → **图文发布流程（流程 A.1）**
4. 用户只提供网页 URL → 先提取内容和图片，再给用户确认
5. 信息不全 → 先补齐缺失信息

## 流程 A: 图文/视频发布

### Step 1: 处理内容

#### URL 提取模式
1. 使用 WebFetch 提取网页内容
2. 提取关键信息：标题、正文、图片 URL
3. 适当总结内容，保持语言自然

#### 图片提取规则

- **优先取 `data-src`**：若 `img` 标签同时有 `src` 和 `data-src`，以 `data-src` 为准
- **跳过占位图**：路径含 `/shims/`、`/placeholder`、`/theme/` 等的图片为占位符
- **只取内容图**：跳过网站 logo、图标、视频封面缩略图
- **格式验证**：图片 URL 应以 `.jpg`、`.jpeg`、`.png`、`.webp`、`.gif` 结尾

### Step 2: 内容检查

#### 标题长度
标题长度必须 ≤ 20（UTF-16 字节数向上取整除以 2）。规则：汉字/全角符号计 1，英文/数字/半角符号每 2 个计 1。

### Step 3: 用户确认

通过 `AskUserQuestion` 展示即将发布的内容，获得明确确认后继续。

### Step 4: 执行发布（分步方式）

```bash
# 步骤 1: 填写图文表单（不发布）
python scripts/cli.py fill-publish \
  --title-file /tmp/xhs_title.txt \
  --content-file /tmp/xhs_content.txt \
  --images "/abs/path/pic1.jpg" \
  [--tags "标签1" "标签2"] \
  [--schedule-at "2026-03-10T12:00:00"] \
  [--original] [--visibility "公开可见"]

# 步骤 2: 用户在浏览器中确认预览

# 步骤 3a: 用户确认发布
python scripts/cli.py click-publish

# 步骤 3b: 用户取消 → 必须先保存草稿！
python scripts/cli.py save-draft
```

### 一步到位发布

```bash
# 图文一步到位
python scripts/cli.py publish \
  --title-file /tmp/xhs_title.txt \
  --content-file /tmp/xhs_content.txt \
  --images "/abs/path/pic1.jpg"

# 带标签和定时发布
python scripts/cli.py publish \
  --title-file /tmp/xhs_title.txt \
  --content-file /tmp/xhs_content.txt \
  --images "/abs/path/pic1.jpg" \
  --tags "标签1" "标签2" \
  --schedule-at "2026-03-10T12:00:00" \
  --original
```

## 流程 B: 长文发布

```bash
# 步骤 1: 填写长文内容
python scripts/cli.py long-article \
  --title-file /tmp/xhs_title.txt \
  --content-file /tmp/xhs_content.txt \
  [--images "/abs/path/pic1.jpg"]

# 步骤 2: 选择排版模板
python scripts/cli.py select-template --name "用户选择的模板名"

# 步骤 3: 进入发布页
python scripts/cli.py next-step \
  --content-file /tmp/xhs_description.txt

# 步骤 4: 用户确认并发布
python scripts/cli.py click-publish
```

## 常用参数

| 参数 | 说明 |
|------|------|
| `--title-file path` | 标题文件路径（必须） |
| `--content-file path` | 正文文件路径（必须） |
| `--images path1 path2` | 图片路径/URL 列表（图文必须） |
| `--video path` | 视频文件路径（视频必须） |
| `--tags tag1 tag2` | 话题标签列表 |
| `--schedule-at ISO8601` | 定时发布时间 |
| `--original` | 声明原创 |
| `--visibility` | 可见范围 |
| `--headless` | 无头模式（未登录自动降级到有窗口模式） |
| `--account name` | 指定账号 |

## 失败处理

- **登录失败**：提示用户重新扫码登录
- **图片下载失败**：提示更换图片 URL 或改用本地图片
- **标题过长**：自动缩短标题
- **页面选择器失效**：提示检查脚本中的选择器定义
- **用户取消发布**：必须运行 `save-draft` 保存草稿

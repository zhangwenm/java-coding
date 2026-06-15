# gstack

快速无头浏览器用于 QA 测试和站点自测。

## 功能描述

用于测试功能、验证部署、dogfood 用户流程或提交带证据的 bug。

## 特点

- ~100ms/命令
- 持久化 Chromium
- 30 分钟空闲后自动关闭
- 状态在调用间持久化（cookies、tabs、sessions）
- 对话框默认自动接受

## 设置检查

```bash
_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
B=""
[ -n "$_ROOT" ] && [ -x "$_ROOT/.claude/skills/gstack/browse/dist/browse" ] && B="$_ROOT/.claude/skills/gstack/browse/dist/browse"
[ -z "$B" ] && B=~/.claude/skills/gstack/browse/dist/browse
if [ -x "$B" ]; then
  echo "READY: $B"
else
  echo "NEEDS_SETUP"
fi
```

如需设置:
1. 告诉用户: "gstack browse 需要一次性构建（约10秒）。是否继续？"
2. 运行: `cd <SKILL_DIR> && ./setup`

## QA 工作流程

### 测试用户流程

```bash
# 1. 转到页面
$B goto https://app.example.com/login

# 2. 查看可交互内容
$B snapshot -i

# 3. 填写表单
$B fill @e3 "test@example.com"
$B fill @e4 "password123"
$B click @e5

# 4. 验证结果
$B snapshot -D
$B is visible ".dashboard"
$B screenshot /tmp/after-login.png
```

### 验证部署

```bash
$B goto https://yourapp.com
$B text
$B console
$B network
$B is visible ".hero-section"
$B screenshot /tmp/prod-check.png
```

### 测试响应式布局

```bash
# 快速: 3 个分辨率截图
$B goto https://yourapp.com
$B responsive /tmp/layout

# 手动: 特定视口
$B viewport 375x812     # iPhone
$B screenshot /tmp/mobile.png
```

### 测试表单验证

```bash
$B goto https://app.example.com/form
$B snapshot -i

# 提交空表单 - 检查验证错误
$B click @e10
$B snapshot -D
$B is visible ".error-message"
```

## 快照系统

```
-i        --interactive           仅交互元素（按钮、链接、输入）
-c        --compact               紧凑（无空结构节点）
-d <N>    --depth                 限制树深度
-s <sel>  --selector              作用域为 CSS 选择器
-D        --diff                  与上一个快照对比
-a        --annotate              带注释截图
-o <path> --output                注释截图输出路径
-C        --cursor-interactive    光标交互元素
```

## 快速断言模式

```bash
# 元素存在且可见
$B is visible ".modal"

# 按钮启用/禁用
$B is enabled "#submit-btn"
$B is disabled "#submit-btn"

# 复选框状态
$B is checked "#agree"

# 页面包含文本
$B js "document.body.textContent.includes('Success')"
```

## 命令参考

### 导航
| 命令 | 描述 |
|------|------|
| `goto <url>` | 导航到 URL |
| `back` | 后退 |
| `forward` | 前进 |
| `reload` | 重新加载 |
| `url` | 打印当前 URL |

### 交互
| 命令 | 描述 |
|------|------|
| `click <sel>` | 点击元素 |
| `fill <sel> <val>` | 填写输入 |
| `hover <sel>` | 悬停元素 |
| `press <key>` | 按键 |
| `upload <sel> <file>` | 上传文件 |
| `select <sel> <val>` | 选择下拉选项 |

### 视觉
| 命令 | 描述 |
|------|------|
| `screenshot [path]` | 保存截图 |
| `pdf [path]` | 保存为 PDF |
| `responsive [prefix]` | 多分辨率截图 |
| `diff <url1> <url2>` | 页面对比 |

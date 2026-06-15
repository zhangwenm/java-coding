# deep-research

使用 Google Gemini Deep Research Agent 执行自主多步骤研究。

## 功能描述

用于市场分析、竞品分析、文献综述、技术研究、尽职调查。

## 需求

- Python 3.8+
- httpx: `pip install -r requirements.txt`
- `GEMINI_API_KEY` 环境变量

## 设置

1. 从 [Google AI Studio](https://aistudio.google.com/) 获取 Gemini API 密钥
2. 设置环境变量:
   ```bash
   export GEMINI_API_KEY=your-api-key-here
   ```

## 用法

### 开始研究任务
```bash
python3 scripts/research.py --query "Research the history of Kubernetes"
```

### 结构化输出格式
```bash
python3 scripts/research.py --query "Compare Python web frameworks" \
  --format "1. Executive Summary\n2. Comparison Table\n3. Recommendations"
```

### 实时流式显示进度
```bash
python3 scripts/research.py --query "Analyze EV battery market" --stream
```

### 不等待启动
```bash
python3 scripts/research.py --query "Research topic" --no-wait
```

### 检查运行中的研究状态
```bash
python3 scripts/research.py --status <interaction_id>
```

### 等待完成
```bash
python3 scripts/research.py --wait <interaction_id>
```

### 从之前的研究继续
```bash
python3 scripts/research.py --query "Elaborate on point 2" --continue <interaction_id>
```

### 列出最近研究
```bash
python3 scripts/research.py --list
```

## 输出格式

- **默认**: 人类可读的 markdown 报告
- **JSON** (`--json`): 结构化数据用于编程使用
- **Raw** (`--raw`): 未经处理的 API 响应

## 成本和时间

| 指标 | 值 |
|------|-----|
| 时间 | 每任务 2-10 分钟 |
| 成本 | 每任务 $2-5 |
| Token 使用 | ~250k-900k 输入，~60k-80k 输出 |

## 最佳用途

- 市场分析和竞品分析
- 技术文献综述
- 尽职调查
- 历史研究和时间线
- 比较分析（框架、产品、技术）

## 工作流程

1. 用户请求研究 → 运行 `--query "..."`
2. 告知用户预计时间（2-10 分钟）
3. 用 `--stream` 监控或用 `--status` 轮询
4. 返回格式化结果
5. 用 `--continue` 跟进问题

## 退出码

- **0**: 成功
- **1**: 错误（API 错误、配置问题、超时）
- **130**: 用户取消（Ctrl+C）

# Claude Code Skills Documentation

本目录包含所有可用 Claude Code Skills 的文档，用于快速参考和技能使用指南。

## 🚀 快速开始

| 指南 | 内容 |
|------|------|
| **[使用指南](usage-guide.md)** | 按任务类型组织，找到适合的技能 |
| **[使用示例](usage-examples.md)** | 每个技能的详细使用场景示例 |
| **[快速参考卡](quick-reference.md)** | 一页纸速查，触发词快速索引 |

## 目录结构

```
doc/
├── README.md                    # 本文档 - 技能总览
├── apify/                      # Apify 数据抓取平台
│   ├── README.md
│   ├── apify-actor-development.md
│   ├── apify-actorization.md
│   ├── apify-audience-analysis.md
│   ├── apify-brand-reputation-monitoring.md
│   ├── apify-competitor-intelligence.md
│   ├── apify-content-analytics.md
│   ├── apify-ecommerce.md
│   ├── apify-influencer-discovery.md
│   ├── apify-lead-generation.md
│   ├── apify-market-research.md
│   ├── apify-trend-analysis.md
│   └── apify-ultimate-scraper.md
├── feishu/                     # 飞书办公平台
│   ├── README.md
│   ├── feishu-doc.md
│   ├── feishu-wiki.md
│   ├── feishu-perm.md
│   └── feishu-drive.md
├── design/                      # 设计和文档工具
│   ├── README.md
│   ├── pdf.md
│   ├── docx.md
│   ├── pptx.md
│   ├── xlsx.md
│   ├── canvas-design.md
│   └── frontend-design.md
├── development/                # 开发工具
│   ├── README.md
│   ├── gstack.md
│   ├── qa.md
│   ├── test-driven-development.md
│   ├── systematic-debugging.md
│   ├── review.md
│   ├── owasp-security.md
│   ├── deep-research.md
│   └── brainstorming.md
└── xiaohongshu/                # 小红书自动化
    ├── README.md
    ├── xiaohongshu-skills.md
    ├── xhs-auth.md
    ├── xhs-explore.md
    ├── xhs-interact.md
    ├── xhs-publish.md
    └── xhs-content-ops.md
```

## 技能总览

### Apify Skills (11 个)

用于网页抓取和数据提取。

| 技能 | 用途 |
|------|------|
| `apify-actor-development` | 开发、调试和部署 Apify Actors |
| `apify-actorization` | 将现有项目转换为 Apify Actors |
| `apify-audience-analysis` | 分析社交媒体受众特征 |
| `apify-brand-reputation-monitoring` | 监控品牌声誉和评论 |
| `apify-competitor-intelligence` | 竞品分析 |
| `apify-content-analytics` | 内容表现分析 |
| `apify-ecommerce` | 电商数据提取 |
| `apify-influencer-discovery` | 发现 KOL/网红 |
| `apify-lead-generation` | 销售线索生成 |
| `apify-market-research` | 市场调研 |
| `apify-trend-analysis` | 趋势分析 |
| `apify-ultimate-scraper` | 通用网页抓取工具 |

### Feishu Skills (4 个)

用于飞书文档和办公自动化。

| 技能 | 用途 |
|------|------|
| `feishu-doc` | 飞书文档读写操作 |
| `feishu-wiki` | 飞书知识库导航 |
| `feishu-perm` | 飞书文档/文件权限管理 |
| `feishu-drive` | 飞书云空间文件管理 |

### Design & Document Skills (6 个)

用于文档处理和视觉设计。

| 技能 | 用途 |
|------|------|
| `pdf` | PDF 文件处理 |
| `docx` | Word 文档创建和编辑 |
| `pptx` | PowerPoint 演示文稿创建和编辑 |
| `xlsx` | Excel 电子表格创建和编辑 |
| `canvas-design` | 创建视觉艺术（PNG/PDF） |
| `frontend-design` | 创建前端界面和 Web 组件 |

### Development Tools (8 个)

用于代码质量和开发效率。

| 技能 | 用途 |
|------|------|
| `gstack` | 快速无头浏览器用于 QA 测试 |
| `qa` | 系统化 QA 测试流程 |
| `test-driven-development` | 测试驱动开发 |
| `systematic-debugging` | 系统化调试流程 |
| `review` | PR 预提交审查 |
| `owasp-security` | OWASP 安全最佳实践 |
| `deep-research` | 深度研究工具 |
| `brainstorming` | 头脑风暴和设计流程 |

### XiaoHongShu Skills (6 个)

用于小红书自动化运营。

| 技能 | 用途 |
|------|------|
| `xiaohongshu-skills` | 小红书自动化技能集合主入口 |
| `xhs-auth` | 认证管理（登录、状态检查、多账号） |
| `xhs-explore` | 内容发现（搜索、浏览、详情） |
| `xhs-interact` | 社交互动（评论、点赞、收藏） |
| `xhs-publish` | 内容发布（图文、视频、长文） |
| `xhs-content-ops` | 复合运营（竞品分析、热点追踪） |

## 常用组合工作流

### 小红书运营
```
1. 登录账号 → xhs-auth
2. 发现内容 → xhs-explore
3. 发布笔记 → xhs-publish
4. 互动运营 → xhs-interact
5. 竞品分析 → xhs-content-ops
```

### 竞品监控
```
1. 抓取数据 → apify-ultimate-scraper
2. 内容分析 → apify-content-analytics
3. 声誉监控 → apify-brand-reputation-monitoring
4. 生成报告 → deep-research
5. 写入飞书 → feishu-doc
```

### 电商监控
```
1. 价格监控 → apify-ecommerce
2. 评论分析 → apify-brand-reputation-monitoring
3. 生成周报 → docx / xlsx
4. 发送团队 → feishu-doc
```

## 按任务类型索引

### 数据采集和提取

| 任务 | 推荐技能 |
|------|----------|
| 抓取社交媒体数据 | `apify-ultimate-scraper` |
| 电商数据提取 | `apify-ecommerce` |
| 市场调研 | `apify-market-research` |
| 竞品分析 | `apify-competitor-intelligence` |
| 线索生成 | `apify-lead-generation` |

### 文档处理

| 任务 | 推荐技能 |
|------|----------|
| 处理 PDF | `pdf` |
| 创建 Word 文档 | `docx` |
| 创建 Excel 表格 | `xlsx` |
| 创建演示文稿 | `pptx` |

### 视觉设计

| 任务 | 推荐技能 |
|------|----------|
| 创建海报/艺术品 | `canvas-design` |
| 创建 Web 界面 | `frontend-design` |

### 开发效率

| 任务 | 推荐技能 |
|------|----------|
| QA 测试 | `gstack` + `qa` |
| TDD 开发 | `test-driven-development` |
| 调试问题 | `systematic-debugging` |
| PR 审查 | `review` |
| 安全审查 | `owasp-security` |

### 办公自动化

| 任务 | 推荐技能 |
|------|----------|
| 飞书文档操作 | `feishu-doc` |
| 飞书知识库 | `feishu-wiki` |
| 小红书内容发布 | `xhs-publish` |
| 小红书运营分析 | `xhs-content-ops` |

## 快速开始

### 使用技能

当用户请求触发技能时，系统会自动加载对应的技能文档。根据技能描述和文档进行操作。

### 前提条件检查

大部分技能需要特定的环境配置：

- **Apify 技能**: 需要 `APIFY_TOKEN` 环境变量
- **XiaoHongShu 技能**: 需要 Python 3、uv、Chrome 浏览器
- **文档技能**: 需要相应的工具库（Node.js、Python 等）

### 获取帮助

每个技能文档包含：
- 功能描述
- 前提条件
- 使用示例
- 命令参考
- 错误处理

## 贡献指南

添加新技能文档：

1. 在对应类别目录下创建 Markdown 文件
2. 使用标准模板：
   - 功能描述
   - 前提条件
   - 工作流程
   - 命令参考
   - 错误处理
3. 更新本 index.md 文件

## 相关资源

| 文档 | 说明 |
|------|------|
| [使用指南](usage-guide.md) | 按任务类型组织，快速找到合适技能 |
| [使用示例](usage-examples.md) | 35+ 详细场景示例 |
| [快速参考卡](quick-reference.md) | 触发词速查表 |

---

**最后更新**: 2026-03-21
**总技能数**: 35
**文档总数**: 45 (35 技能文档 + 3 使用指南 + 5 分类索引 + 1 主索引 + 1 README)

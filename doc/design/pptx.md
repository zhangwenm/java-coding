# pptx

PowerPoint 演示文稿 (.pptx) 创建、编辑和分析技能。

## 功能描述

当需要处理 PowerPoint 演示文稿时激活，包括：
- 创建新演示文稿
- 编辑/修改内容
- 使用布局
- 添加评论或演讲者备注
- 其他演示任务

## 读取和分析内容

### 文本提取
```bash
python -m markitdown path-to-file.pptx
```

### 解压获取原始 XML
```bash
python ooxml/scripts/unpack.py <office_file> <output_dir>
```

## 创建新演示文稿

### html2pptx 工作流程

1. **阅读完整指南**: `html2pptx.md`
2. 为每张幻灯片创建 HTML 文件
3. 使用 `html2pptx.js` 转换为 PowerPoint
4. **视觉验证**: 生成缩略图检查布局

### 设计原则

**重要**: 创建前分析内容并选择合适的设计元素:
1. 考虑主题 - 什么色调/风格适合
2. 检查品牌 - 提及公司时考虑品牌颜色
3. 匹配调色板 - 选择匹配内容的颜色
4. 说明方法 - 解释设计选择

### 颜色选择示例

| 调色板 | 描述 |
|--------|------|
| Classic Blue | 深海军蓝 (#1C2833), 石板灰, 银色, 米白 |
| Teal & Coral | 青绿色, 深青, 珊瑚红, 白色 |
| Bold Red | 红色系 (#C0392B, #E74C3C), 橙色, 黄色, 绿色 |
| Warm Blush | 淡紫色, 腮红, 玫瑰, 奶油色 |
| Burgundy Luxury | 勃艮第红, 深红, 铁锈色, 金色 |

### 布局提示

- **两栏布局（推荐）**: 标题跨全宽，下面两栏 - 文本在一栏，特色内容在另一栏
- **全幻灯片布局**: 让特色内容（图表/表格）占据整张幻灯片以获得最大影响力
- **绝不垂直堆叠**: 不要在单栏中将图表/表格放在文本下方

## 编辑现有演示文稿

### 工作流程

1. **阅读完整指南**: `ooxl.md`
2. 解压: `python ooxml/scripts/unpack.py <office_file> <output_dir>`
3. 编辑 XML 文件
4. **验证**: `python ooxml/scripts/validate.py <dir> --original <file>`
5. 打包: `python ooxml/scripts/pack.py <input_directory> <office_file>`

## 使用模板创建

### 工作流程

1. 提取内容和创建缩略图:
   ```bash
   python -m markitdown template.pptx > template-content.md
   python scripts/thumbnail.py template.pptx
   ```

2. 分析模板并保存清单

3. 创建演示文稿大纲

4. 使用 `rearrange.py` 重排幻灯片:
   ```bash
   python scripts/rearrange.py template.pptx working.pptx 0,34,50,52
   ```

5. 提取文本清单:
   ```bash
   python scripts/inventory.py working.pptx text-inventory.json
   ```

6. 生成替换文本并保存

7. 应用替换:
   ```bash
   python scripts/replace.py working.pptx replacement-text.json output.pptx
   ```

## 创建缩略图网格

```bash
python scripts/thumbnail.py template.pptx [output_prefix]
```

## 依赖

- **markitdown**: `pip install "markitdown[pptx]"`
- **pptxgenjs**: `npm install -g pptxgenjs`
- **playwright**: `npm install -g playwright`
- **react-icons**: `npm install -g react-icons react react-dom`
- **sharp**: `npm install -g sharp`
- **LibreOffice**: PDF 转换
- **Poppler**: PDF 转图像

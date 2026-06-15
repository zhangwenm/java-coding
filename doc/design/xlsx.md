# xlsx

Excel 电子表格 (.xlsx) 创建、编辑和分析技能。

## 功能描述

当需要处理电子表格文件时激活，包括：
- 打开、读取、编辑现有 .xlsx、.xlsm、.csv 或 .tsv 文件
- 从头创建新电子表格
- 在电子表格格式之间转换

## 财务模型要求

### 颜色编码标准

除非用户或现有模板另有说明:

| 颜色 | 含义 |
|------|------|
| 蓝色文本 (RGB: 0,0,255) | 硬编码输入和用户将更改的数字 |
| 黑色文本 (RGB: 0,0,0) | 所有公式和计算 |
| 绿色文本 (RGB: 0,128,0) | 从同一工作簿内其他工作表拉取的链接 |
| 红色文本 (RGB: 255,0,0) | 到其他文件的外部链接 |
| 黄色背景 (RGB: 255,255,0) | 需要注意的关键假设或需要更新的单元格 |

### 数字格式标准

| 类型 | 格式 |
|------|------|
| 年份 | 格式化为文本字符串（如 "2024"） |
| 货币 | 使用 $#,##0 格式；始终在标题指定单位 |
| 零值 | 使用数字格式使所有零显示为 "-" |
| 百分比 | 默认 0.0% 格式 |
| 倍数 | 格式为 0.0x |
| 负数 | 使用括号 (123) 而非负号 -123 |

## Excel 工作流程

### 读取数据 (pandas)
```python
import pandas as pd

df = pd.read_excel('file.xlsx')  # 默认: 第一个工作表
all_sheets = pd.read_excel('file.xlsx', sheet_name=None)  # 所有工作表

df.head()      # 预览数据
df.info()      # 列信息
df.describe()  # 统计
```

### 创建新文件
```python
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment

wb = Workbook()
sheet = wb.active

sheet['A1'] = 'Hello'
sheet['B2'] = '=SUM(A1:A10)'  # 公式

sheet['A1'].font = Font(bold=True, color='FF0000')
sheet['A1'].fill = PatternFill('solid', start_color='FFFF00')
sheet['A1'].alignment = Alignment(horizontal='center')

wb.save('output.xlsx')
```

### 编辑现有文件
```python
from openpyxl import load_workbook

wb = load_workbook('existing.xlsx')
sheet = wb.active

sheet['A1'] = 'New Value'
sheet.insert_rows(2)
sheet.delete_cols(3)

wb.save('modified.xlsx')
```

## 关键规则

### 绝不使用硬编码值

**错误** - 硬编码计算值:
```python
total = df['Sales'].sum()
sheet['B10'] = total  # 硬编码 5000
```

**正确** - 使用 Excel 公式:
```python
sheet['B10'] = '=SUM(B2:B9)'
```

### 重新计算公式

创建或修改后必须重新计算公式:
```bash
python scripts/recalc.py output.xlsx
```

脚本返回:
```json
{
  "status": "success",
  "total_errors": 0,
  "error_summary": { /* 如有错误 */ }
}
```

## 常见错误修复

| 错误 | 解决方案 |
|------|----------|
| #REF! | 无效的单元格引用 |
| #DIV/0! | 除以零 |
| #VALUE! | 公式中错误的数据类型 |
| #NAME? | 无法识别的公式名称 |

## 最佳实践

### 库选择
- **pandas**: 数据分析、批量操作、简单数据导出
- **openpyxl**: 复杂格式化、公式、Excel 特定功能

### openpyxl 注意事项
- 单元格索引从 1 开始（row=1, column=1 指向 A1）
- 使用 `data_only=True` 读取计算值
- 公式保留但不计算 - 使用 scripts/recalc.py 更新值

### pandas 注意事项
- 指定数据类型避免推断问题
- 大文件读取特定列: `usecols=['A', 'C', 'E']`

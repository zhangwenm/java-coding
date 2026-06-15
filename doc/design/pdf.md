# pdf

PDF 文件处理技能，用于读取、提取、合并、分割、创建 PDF 等操作。

## 功能描述

当用户需要处理 PDF 文件时激活，包括：
- 读取或提取 PDF 中的文本/表格
- 合并或拆分 PDF
- 旋转页面
- 添加水印
- 创建新 PDF
- 填写 PDF 表单
- 加密/解密 PDF
- 提取图片
- OCR 扫描 PDF

## Python 库

### pypdf - 基本操作

#### 合并 PDF
```python
from pypdf import PdfWriter, PdfReader

writer = PdfWriter()
for pdf_file in ["doc1.pdf", "doc2.pdf", "doc3.pdf"]:
    reader = PdfReader(pdf_file)
    for page in reader.pages:
        writer.add_page(page)

with open("merged.pdf", "wb") as output:
    writer.write(output)
```

#### 分割 PDF
```python
reader = PdfReader("input.pdf")
for i, page in enumerate(reader.pages):
    writer = PdfWriter()
    writer.add_page(page)
    with open(f"page_{i+1}.pdf", "wb") as output:
        writer.write(output)
```

#### 提取文本
```python
reader = PdfReader("document.pdf")
text = ""
for page in reader.pages:
    text += page.extract_text()
```

#### 旋转页面
```python
page = reader.pages[0]
page.rotate(90)  # 顺时针旋转 90 度
writer.add_page(page)
```

### pdfplumber - 文本和表格提取

#### 提取文本
```python
import pdfplumber

with pdfplumber.open("document.pdf") as pdf:
    for page in pdf.pages:
        text = page.extract_text()
        print(text)
```

#### 提取表格
```python
with pdfplumber.open("document.pdf") as pdf:
    for i, page in enumerate(pdf.pages):
        tables = page.extract_tables()
        for j, table in enumerate(tables):
            print(f"Table {j+1} on page {i+1}:")
            for row in table:
                print(row)
```

### reportlab - 创建 PDF

#### 基本 PDF 创建
```python
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

c = canvas.Canvas("hello.pdf", pagesize=letter)
width, height = letter

c.drawString(100, height - 100, "Hello World!")
c.save()
```

#### 多页 PDF
```python
from reportlab.platypus import SimpleDocTemplate, Paragraph, PageBreak
from reportlab.lib.styles import getSampleStyleSheet

doc = SimpleDocTemplate("report.pdf", pagesize=letter)
styles = getSampleStyleSheet()
story = []

title = Paragraph("Report Title", styles['Title'])
story.append(title)
story.append(PageBreak())
story.append(Paragraph("Page 2", styles['Heading1']))

doc.build(story)
```

**重要**: 绝不使用 Unicode 下标/上标字符 (₀₁₂₃₄₅₆₇₈₉, ⁰¹²³⁴⁵⁶⁷⁸⁹)。使用 XML 标记标签:
```python
# 下标
chemical = Paragraph("H<sub>2</sub>O", styles['Normal'])
# 上标
squared = Paragraph("x<super>2</super>", styles['Normal'])
```

## 命令行工具

### pdftotext (poppler-utils)
```bash
# 提取文本
pdftotext input.pdf output.txt

# 保留布局提取
pdftotext -layout input.pdf output.txt

# 提取特定页面
pdftotext -f 1 -l 5 input.pdf output.txt  # 页面 1-5
```

### qpdf
```bash
# 合并 PDF
qpdf --empty --pages file1.pdf file2.pdf -- merged.pdf

# 分割页面
qpdf input.pdf --pages . 1-5 -- pages1-5.pdf

# 旋转页面
qpdf input.pdf output.pdf --rotate=+90:1

# 移除密码
qpdf --password=mypassword --decrypt encrypted.pdf decrypted.pdf
```

## 常见任务

### OCR 扫描 PDF
```python
import pytesseract
from pdf2image import convert_from_path

images = convert_from_path('scanned.pdf')
text = ""
for i, image in enumerate(images):
    text += f"Page {i+1}:\n"
    text += pytesseract.image_to_string(image)
```

### 添加水印
```python
watermark = PdfReader("watermark.pdf").pages[0]
reader = PdfReader("document.pdf")
writer = PdfWriter()

for page in reader.pages:
    page.merge_page(watermark)
    writer.add_page(page)

with open("watermarked.pdf", "wb") as output:
    writer.write(output)
```

### 密码保护
```python
writer.encrypt("userpassword", "ownerpassword")
```

## 快速参考

| 任务 | 最佳工具 | 命令/代码 |
|------|----------|-----------|
| 合并 PDF | pypdf | `writer.add_page(page)` |
| 分割 PDF | pypdf | 每页一个文件 |
| 提取文本 | pdfplumber | `page.extract_text()` |
| 提取表格 | pdfplumber | `page.extract_tables()` |
| 创建 PDF | reportlab | Canvas 或 Platypus |
| 命令行合并 | qpdf | `qpdf --empty --pages ...` |
| OCR 扫描 | pytesseract | 先转换为图像 |

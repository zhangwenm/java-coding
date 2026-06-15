# docx

Word 文档 (.docx) 创建、编辑和分析技能。

## 功能描述

当需要创建、读取、编辑或操作 Word 文档时激活。

## 快速参考

| 任务 | 方法 |
|------|------|
| 读取/分析内容 | `pandoc` 或解压获取原始 XML |
| 创建新文档 | 使用 `docx-js` |
| 编辑现有文档 | 解压 → 编辑 XML → 重新打包 |

## 创建新文档

安装: `npm install -g docx`

### 基本结构
```javascript
const { Document, Packer, Paragraph, TextRun } = require('docx');

const doc = new Document({
  sections: [{ children: [/* content */] }]
});

Packer.toBuffer(doc).then(buffer => fs.writeFileSync("doc.docx", buffer));
```

### 页面大小

**重要**: docx-js 默认 A4，不是 US Letter。始终显式设置页面大小:

```javascript
sections: [{
  properties: {
    page: {
      size: {
        width: 12240,   // 8.5 英寸 (DXA)
        height: 15840   // 11 英寸 (DXA)
      },
      margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } // 1 英寸边距
    }
  },
  children: [/* content */]
}]
```

### 样式（覆盖内置标题）

```javascript
const doc = new Document({
  styles: {
    default: { document: { run: { font: "Arial", size: 24 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal",
        run: { size: 32, bold: true, font: "Arial" },
        paragraph: { spacing: { before: 240, after: 240 }, outlineLevel: 0 } },
      // ... 更多样式
    ]
  }
});
```

### 列表（绝不使用 unicode 项目符号）

```javascript
const doc = new Document({
  numbering: {
    config: [{
      reference: "bullets",
      levels: [{ level: 0, format: LevelFormat.BULLET, text: "•",
        style: { paragraph: { indent: { left: 720, hanging: 360 } } } }]
    }]
  },
  sections: [{
    children: [
      new Paragraph({ numbering: { reference: "bullets", level: 0 },
        children: [new TextRun("Bullet item")] }),
    ]
  }]
});
```

### 表格

**关键**: 表格需要双重宽度 - 在表格和每个单元格上都要设置 `columnWidths` 和 `width`。

```javascript
new Table({
  width: { size: 9360, type: WidthType.DXA },
  columnWidths: [4680, 4680],
  rows: [
    new TableRow({
      children: [
        new TableCell({
          borders: { top: border, bottom: border, left: border, right: border },
          width: { size: 4680, type: WidthType.DXA },
          children: [new Paragraph({ children: [new TextRun("Cell")] })]
        })
      ]
    })
  ]
})
```

### 图片

```javascript
new Paragraph({
  children: [new ImageRun({
    type: "png", // 必需: png, jpg, jpeg, gif, bmp, svg
    data: fs.readFileSync("image.png"),
    transformation: { width: 200, height: 150 },
    altText: { title: "Title", description: "Desc", name: "Name" }
  })]
})
```

### 分页符

```javascript
// 关键: PageBreak 必须在 Paragraph 内
new Paragraph({ children: [new PageBreak()] })
```

## 编辑现有文档

### Step 1: 解压
```bash
python scripts/office/unpack.py document.docx unpacked/
```

### Step 2: 编辑 XML
在 `unpacked/word/` 中编辑文件。

### Step 3: 打包
```bash
python scripts/office/pack.py unpacked/ output.docx --original document.docx
```

## 关键规则

- **设置页面大小** - docx-js 默认 A4
- **绝不换行** - 使用单独的 Paragraph 元素
- **绝不使用 unicode 项目符号** - 使用 LevelFormat.BULLET
- **PageBreak 必须在 Paragraph 中** - 独立使用创建无效 XML
- **ImageRun 需要 type** - 始终指定 png/jpg 等
- **表格需要双重宽度** - columnWidths 和 cell width 都设置
- **使用 WidthType.DXA** - 绝不使用 WidthType.PERCENTAGE
- **使用 ShadingType.CLEAR** - 绝不使用 SOLID 进行表格着色

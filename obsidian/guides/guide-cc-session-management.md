---
tags: [工具链, Claude-Code, 上下文管理]
date: 2026-04-17
project: 工具链
status: done
source: https://weibo.com/2194035935/QB29fgdIA（翻译自 Claude 官方博客）
---

# Claude Code 会话管理实践口诀

**结论**：同一任务继续推进用 Continue，走偏了用 Rewind，会话臃肿用 Compact，新任务直接 Clear，大量中间噪音交给 Subagent。

## 5 种场景速查

| 场景 | 工具 | 原因 |
|------|------|------|
| 同一任务继续推进，上下文仍相关 | **Continue** | 窗口内容仍有效，无需重建 |
| Claude 走错方向 | **Rewind**（双击 Esc / `/rewind`） | 保留有用的文件读取，丢掉失败尝试 |
| 会话臃肿但任务未完 | **/compact \<hint\>** | 成本低；hint 可引导压缩方向 |
| 开始全新任务 | **/clear** | 上下文最干净，由自己决定带什么 |
| 下一步产生大量中间输出，只需结论 | **Subagent** | 中间噪音留在子代理，主会话只收结论 |

## 实用技巧

**Rewind 优于事后纠正**：与其说"这个方法不行，试试 X"，不如回退到文件读取完成的时间点，直接说"不要用 A，走 B"。

**引导压缩方向**：
```
/compact focus on the auth refactor, drop the test debugging
```
主动触发并加 hint，避免压缩时丢掉关键信息。

**避免在调试中途触发自动压缩**：压缩在调试过程中触发时，摘要会围绕排障展开，下一步转向别的任务时关键信息已丢失。解决方案：任务切换前主动 `/compact`，并说明接下来的方向。

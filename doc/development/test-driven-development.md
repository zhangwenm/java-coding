# test-driven-development

测试驱动开发技能，遵循红-绿-重构循环。

## 核心原则

```
没有失败的测试就不写生产代码
```

写代码前删除它。重新开始。

## 红-绿-重构循环

```
RED: 写失败的测试 → 验证失败正确
GREEN: 最少代码 → 验证通过
REFACTOR: 清理 → 保持绿色
重复
```

### RED - 写失败的测试

写一个最小测试展示应该发生什么:

```typescript
test('重试失败操作3次', async () => {
  let attempts = 0;
  const operation = () => {
    attempts++;
    if (attempts < 3) throw new Error('fail');
    return 'success';
  };

  const result = await retryOperation(operation);

  expect(result).toBe('success');
  expect(attempts).toBe(3);
});
```

### 验证 RED - 看它失败

**必需。绝不跳过。**

```bash
npm test path/to/test.test.ts
```

确认:
- 测试失败（不是错误）
- 失败消息是预期的
- 因为功能缺失失败（不是拼写错误）

### GREEN - 最少代码

写最简单的代码让测试通过:

```typescript
async function retryOperation<T>(fn: () => Promise<T>): Promise<T> {
  for (let i = 0; i < 3; i++) {
    try {
      return await fn();
    } catch (e) {
      if (i === 2) throw e;
    }
  }
  throw new Error('unreachable');
}
```

不要添加功能、重构其他代码或超过测试范围。

### 验证 GREEN - 看它通过

**必需。**

```bash
npm test path/to/test.test.ts
```

确认:
- 测试通过
- 其他测试仍通过
- 输出干净（无错误、警告）

### REFACTOR - 清理

仅在绿色后:
- 移除重复
- 改进名称
- 提取辅助函数

## 优秀测试

| 质量 | 优秀 | 差劲 |
|------|------|------|
| **最小** | 一件事。名称中有"and"？分开。 | `test('验证邮箱和域名和空白')` |
| **清晰** | 名称描述行为 | `test('test1')` |
| **展示意图** | 展示期望的 API | 隐藏代码应该做什么 |

## 为什么顺序重要

**"之后写测试来验证它工作"**

之后写的测试立即通过。立即通过证明不了什么:
- 可能测试了错误的东西
- 可能测试实现，不是行为
- 可能漏掉你忘记的边界情况
- 你没看到它 catch bug

**测试优先** 强迫你看到测试失败，证明它真的测试了什么。

## 常见借口

| 借口 | 现实 |
|------|------|
| "太简单不用测" | 简单代码也会坏。测试只需30秒。 |
| "之后测试" | 测试立即通过证明不了什么。 |
| "之后测试也能达到同样目标" | 测试后 = "这是什么？" 测试先 = "这应该是什么？" |
| "手动测试过了" | 手动测试是随意的。无法记录、无法重跑。 |

## 完成前验证清单

- [ ] 每个新函数/方法有测试
- [ ] 看每个测试在实现前失败
- [ ] 每个测试因预期原因失败（功能缺失，不是拼写错误）
- [ ] 写最少代码通过每个测试
- [ ] 所有测试通过
- [ ] 输出干净（无错误、警告）
- [ ] 测试使用真实代码（尽量避免 mocks）
- [ ] 覆盖边界情况和错误

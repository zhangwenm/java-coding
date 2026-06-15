# owasp-security

OWASP 安全最佳实践技能，覆盖 OWASP Top 10:2025、ASVS 5.0 和 Agentic AI 安全 (2026)。

## OWASP Top 10:2025 快速参考

| # | 漏洞 | 关键预防 |
|---|------|----------|
| A01 | 访问控制破坏 | 默认拒绝、强制服务端、验证所有权 |
| A02 | 安全配置错误 | 强化配置、禁用默认值、最小化功能 |
| A03 | 供应链失败 | 锁定版本、验证完整性、审计依赖 |
| A04 | 加密失败 | TLS 1.2+、AES-256-GCM、Argon2/bcrypt |
| A05 | 注入 | 参数化查询、输入验证、安全 API |
| A06 | 不安全设计 | 威胁模型、限速、设计安全控制 |
| A07 | 认证失败 | MFA、检查泄露密码、安全会话 |
| A08 | 完整性失败 | 签名包、CDN 的 SRI、安全序列化 |
| A09 | 日志记录失败 | 记录安全事件、结构化格式、告警 |
| A10 | 异常处理 | 故障关闭、隐藏内部、记录上下文 |

## 安全代码审查清单

### 输入处理
- [ ] 所有用户输入在服务端验证
- [ ] 使用参数化查询（不是字符串连接）
- [ ] 强制执行输入长度限制
- [ ] 白名单验证优先于黑名单

### 认证与会话
- [ ] 密码用 Argon2/bcrypt 哈希（不是 MD5/SHA1）
- [ ] 会话令牌有足够熵（128+ 位）
- [ ] 注销时使会话失效
- [ ] 敏感操作提供 MFA

### 访问控制
- [ ] 检查框架级认证中间件
- [ ] 每次请求检查授权
- [ ] 使用用户无法操纵的对象引用
- [ ] 默认拒绝策略
- [ ] 审查权限提升路径

### 数据保护
- [ ] 敏感数据静态加密
- [ ] 传输中全用 TLS
- [ ] URL/日志中无敏感数据
- [ ] 密钥在环境/保险库（不是代码）

### 错误处理
- [ ] 不向用户暴露堆栈跟踪
- [ ] 故障关闭（拒绝，不是允许）
- [ ] 所有异常记录上下文
- [ ] 一致错误响应（无枚举）

## 安全代码模式

### SQL 注入预防
```python
# 不安全
cursor.execute(f"SELECT * FROM users WHERE id = {user_id}")

# 安全
cursor.execute("SELECT * FROM users WHERE id = %s", (user_id,))
```

### 命令注入预防
```python
# 不安全
os.system(f"convert {filename} output.png")

# 安全
subprocess.run(["convert", filename, "output.png"], shell=False)
```

### 密码存储
```python
# 不安全
hashlib.md5(password.encode()).hexdigest()

# 安全
from argon2 import PasswordHasher
PasswordHasher().hash(password)
```

### 访问控制
```python
# 不安全 - 无授权检查
@app.route('/api/user/<user_id>')
def get_user(user_id):
    return db.get_user(user_id)

# 安全 - 强制授权
@app.route('/api/user/<user_id>')
@login_required
def get_user(user_id):
    if current_user.id != user_id and not current_user.is_admin:
        abort(403)
    return db.get_user(user_id)
```

### 故障关闭
```python
# 不安全 - 故障打开
def check_permission(user, resource):
    try:
        return auth_service.check(user, resource)
    except Exception:
        return True  # 危险!

# 安全 - 故障关闭
def check_permission(user, resource):
    try:
        return auth_service.check(user, resource)
    except Exception as e:
        logger.error(f"Auth check failed: {e}")
        return False  # 故障时拒绝
```

## Agentic AI 安全 (OWASP 2026)

| 风险 | 描述 | 缓解 |
|------|------|------|
| ASI01 | 目标劫持 | 提示注入改变代理目标 | 输入消毒、目标边界、行为监控 |
| ASI02 | 工具滥用 | 工具被意外使用 | 最小权限、细粒度权限、验证 I/O |
| ASI03 | 权限滥用 | 跨代理凭证升级 | 短命作用域令牌、身份验证 |
| ASI04 | 供应链 | 受损插件/MCP 服务器 | 验证签名、沙箱、白名单插件 |
| ASI05 | 代码执行 | 不安全代码生成/执行 | 沙箱执行、静态分析、人工批准 |

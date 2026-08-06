# 登录模块 — 设计规格说明

**日期**：2026-08-01
**项目**：古玩寄卖平台 — 服务端
**参考**：`C:\Users\20844\Desktop\ciTY Art` 项目架构模式
**接口文档**：`01登录接口文档.md`

---

## 一、设计目标

实现登录模块全部 6 个 REST 接口，基于 Redis Token 认证机制（非 JWT），遵循 ciTY Art 参考项目的代码分层模式。

## 二、架构决策

### 2.1 Token 机制：Redis 随机 Token（非 JWT）

**与参考项目 ciTY Art 的核心区别**：ciTY Art 使用 JWT 自包含令牌 + Redis 黑名单；本项目使用 Redis 存储令牌。

| 决策项 | 选择 | 理由 |
|--------|------|------|
| Token 格式 | 随机 UUID（去横线） | 接口文档要求，无业务含义不可解码 |
| Token 存储 | Redis `antique:token:{token}` → JSON | 文档明确，可随时失效 |
| Token 续期 | 每次请求刷新 TTL 7 天 | 用户体验：活跃用户无需重新登录 |
| 登出方式 | 直接 DEL Redis Key | 比黑名单方案更简单、更干净 |

### 2.2 单端设计（仅用户端）

ciTY Art 有 user + admin 两个端，本项目第一版只需要用户端。后续加管理端时，可参考 ciTY Art 的 `AdminContext` 模式扩展。

## 三、模块划分

### antique-common — 公共模块

| 类 | 包 | 职责 |
|----|-----|------|
| `Result<T>` | `com.antique.result` | 统一响应（code=1/0） |
| `MessageConstant` | `com.antique.constant` | 操作消息常量 |
| `RedisConstant` | `com.antique.constant` | Redis Key 前缀与 TTL |
| `UserContext` | `com.antique.context` | ThreadLocal 用户上下文 |
| `BaseException` | `com.antique.exception` | 抽象业务异常基类 |
| `AuthException` | `com.antique.exception` | 认证异常 |

### antique-pojo — 数据模型模块

| 类 | 包 | 职责 |
|----|-----|------|
| `User` | `com.antique.entity` | 用户实体（@TableName("user")） |
| `SendCodeDTO` | `com.antique.dto` | 发送验证码请求体 |
| `SmsLoginDTO` | `com.antique.dto` | 短信登录请求体 |
| `PasswordLoginDTO` | `com.antique.dto` | 密码登录请求体 |
| `UpdateProfileDTO` | `com.antique.dto` | 修改个人信息请求体 |
| `LoginVO` | `com.antique.vo` | 登录返回（token + userInfo） |
| `UserProfileVO` | `com.antique.vo` | 用户信息返回 |

### antique-server — 服务模块

| 类 | 包 | 职责 |
|----|-----|------|
| `AntiqueApplication` | `com.antique` | 启动类 + @MapperScan |
| `WebMvcConfiguration` | `com.antique.config` | CORS + 拦截器注册 |
| `TokenInterceptor` | `com.antique.interceptor` | Token 校验 + 续期 + UserContext 设置 |
| `GlobalExceptionHandler` | `com.antique.handler` | 全局异常 → Result.error() |
| `MyMetaObjectHandler` | `com.antique.handler` | createTime/updateTime 自动填充 |
| `AuthController` | `com.antique.controller` | 验证码发送、短信登录、密码登录 |
| `UserController` | `com.antique.controller` | 个人信息读写、退出登录 |
| `UserService` | `com.antique.service` | 用户业务逻辑 |
| `UserServiceImpl` | `com.antique.service.impl` | 用户业务实现 |
| `UserMapper` | `com.antique.mapper` | MyBatis-Plus BaseMapper |

## 四、关键流程

### 4.1 短信登录（含自动注册）

```
POST /api/auth/login/sms {phone, code}
→ Redis GET sms:code:{phone}
→ 校验 code == 666666
→ Redis DEL sms:code:{phone}
→ DB SELECT * FROM user WHERE phone = ? AND deleted_time IS NULL
→ 用户不存在? → INSERT user (nickname = "藏友" + 随机6位)
→ 用户 status != 1? → 抛 AuthException("账号已被禁用")
→ token = UUID.randomUUID() 去横线
→ Redis SET antique:token:{token} = {"userId":id,"phone":"138****8000"}, EX 604800
→ 返回 LoginVO{token, userInfo}
```

### 4.2 密码登录

```
POST /api/auth/login/password {phone, password}
→ DB SELECT * FROM user WHERE phone = ? AND deleted_time IS NULL
→ 用户不存在 → "账号或密码错误"
→ status != 1 → "账号已被禁用"
→ password == null (未设密码) → "手机号未注册"
→ BCrypt.checkpw(password, user.password) 失败 → "账号或密码错误"
→ 生成 token（同短信登录第6步起）
```

### 4.3 Token 拦截器

```
preHandle():
→ 提取 Authorization: Bearer <token>
→ 无 token → 401 "未登录，请先登录"
→ Redis GET antique:token:{token}
→ 不存在 → 401 "未登录，请先登录"
→ 解析 JSON → userId
→ Redis EXPIRE antique:token:{token} 604800  (续期)
→ UserContext.set(userId, phone)
→ return true
```

### 4.4 退出登录

```
POST /api/user/logout
→ 从 Header 提取 token
→ Redis DEL antique:token:{token}
→ UserContext.clear()
→ Result.success()
```

## 五、错误处理策略

- 业务异常：`throw new AuthException("message")` → GlobalExceptionHandler → `Result.error(msg)`
- 参数校验：`@Valid` + DTO 注解 → `MethodArgumentNotValidException` → 取第一条错误信息
- 系统异常：未预期的 Exception → `Result.error("系统异常")`
- Redis 不可用：TokenService 中 try-catch，Redis 异常时返回 "系统繁忙"，不暴露内部错误

## 六、数据库表

```sql
CREATE TABLE user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(20) NOT NULL,
  password VARCHAR(255) DEFAULT NULL COMMENT 'BCrypt加密，可为空(仅短信登录用户)',
  nickname VARCHAR(50) DEFAULT NULL,
  avatar VARCHAR(500) DEFAULT NULL,
  status TINYINT DEFAULT 1 COMMENT '1=正常, 0=禁用',
  points INT DEFAULT 0,
  sign_in_days INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_time DATETIME DEFAULT NULL
);
```

## 七、文件清单（共约 20 个文件）

- antique-common：6 个 Java 文件
- antique-pojo：6 个 Java 文件
- antique-server：9 个 Java 文件
- 配置文件：application-template.yml（已有，需微调）

## 八、自检清单

- [x] 占位符检查：无 TBD/TODO，所有字段已明确
- [x] 内部一致性：DTO-VO-Entity 一一对应接口文档
- [x] 范围控制：仅 6 个接口，不扩增功能
- [x] 歧义检查：所有错误消息与文档一致

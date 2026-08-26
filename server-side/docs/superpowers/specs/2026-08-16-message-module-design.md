# 消息模块 — 设计规格说明

**日期**：2026-08-16
**项目**：古玩寄卖平台 — 服务端
**接口文档**：`05消息接口文档.md`（团队文档中心）
**数据库文档**：`message.md` / `message.sql`（团队文档中心）

---

## 一、设计目标

实现下单消息通知：**用户下单成功后，系统自动生成一条订单消息并实时推送给该用户**，前端消息页可拉取消息列表、未读数并标记已读。

- 练手项目，不做商家端，不做前端（前端由组员基于接口文档开发）
- 消息类型预留扩展（系统通知 / 订单消息 / AI 助手），V1 仅使用订单消息

## 二、架构决策

### 2.1 实时通道：Spring 原生 WebSocket（非 STOMP / SSE）

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 实时通道 | `spring-boot-starter-websocket` 原生 WebSocket | 双向长连接，为后续 AI 助手聊天流式输出预留；不引入 STOMP 增加复杂度 |
| 端点路径 | `/api/ws` | 遵循全部接口 `/api/` 前缀规范；在 `WebMvcConfiguration` 的拦截器中排除该路径（浏览器 WebSocket 无法自定义请求头） |
| 认证方式 | 握手时 query 参数携带 Token | 浏览器 `new WebSocket()` 只能传 URL，Redis Token 随机串安全可接受；握手拦截器内查 Redis 校验，失败拒绝连接 |
| 会话管理 | `ConcurrentHashMap<Long, Set<WebSocketSession>>` | 支持同一用户多端登录；断开时清理 |
| 推送格式 | 文本 JSON `{type, data}` | 前端按 type 分发处理 |

### 2.2 消息落库 + 推送双写

下单成功 → `sendOrderMessage()`：
1. **写库**：插入 `message` 表（type=2 订单消息，is_read=0）
2. **推送**：向该用户所有在线会话发送 `newMessage` JSON

- 写库在 `createOrder` 的同一事务内：下单失败消息随之回滚（合理）；推送不依赖数据库，先写库后推送
- **推送失败不影响下单**：send 异常仅记日志

### 2.3 消息类型枚举（预留）

| type | 含义 | V1 状态 |
|------|------|---------|
| 1 | 系统通知 | 预留 |
| 2 | 订单消息 | ✅ 使用 |
| 3 | AI 助手 | 预留 |

## 三、模块划分

### 新增表：message（antique 库）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED | 主键自增 |
| user_id | BIGINT UNSIGNED | 接收者用户 ID |
| type | TINYINT | 1-系统 2-订单 3-AI 助手 |
| title | VARCHAR(50) | 标题（如"订单消息"，前端分组用） |
| content | VARCHAR(500) | 内容（如"您的订单 xxx 已创建成功，请尽快完成支付"） |
| is_read | TINYINT | 0-未读 1-已读 |
| read_time | DATETIME | 已读时间 |
| created_time / updated_time / deleted_time | DATETIME | 自动填充 / 软删除（同 orders 表风格） |

索引：`idx_user_id`（按用户查）、`idx_user_is_read`（未读数）、`idx_user_created`（列表分页排序）

### antique-pojo — 数据模型模块

| 类 | 包 | 职责 |
|----|-----|------|
| `Message` | `com.antique.entity` | 消息实体（@TableName("message")） |
| `MessageReadDTO` | `com.antique.dto` | 标记已读请求体（messageId） |
| `MessageVO` | `com.antique.vo` | 消息返回（id/type/title/content/isRead/createdTime） |
| `UnreadCountVO` | `com.antique.vo` | 未读数返回（count） |

### antique-server — 服务模块

| 类 | 包 | 职责 |
|----|-----|------|
| `MessageController` | `com.antique.controller` | 消息列表 / 未读数 / 标记已读 |
| `MessageService` / `MessageServiceImpl` | `com.antique.service(.impl)` | 发送消息（写库+推送）、查询、已读 |
| `MessageMapper` | `com.antique.mapper` | MyBatis-Plus BaseMapper + 未读数统计 |
| `WebSocketConfig` | `com.antique.config` | 注册 `/api/ws` 端点 + 握手拦截器 |
| `WsTokenHandshakeInterceptor` | `com.antique.websocket` | 从 query 取 token → 查 Redis → 校验 → 存入 attributes |
| `MessageWebSocketHandler` | `com.antique.websocket` | 连接建立/关闭维护会话表 |
| `WebSocketSessionManager` | `com.antique.websocket` | userId → 会话集合管理 + 按用户推送 |

### antique-server — 修改

| 文件 | 改动 |
|------|------|
| `pom.xml` | 新增 `spring-boot-starter-websocket` |
| `WebMvcConfiguration` | 拦截器排除 `/api/ws` |
| `OrderServiceImpl.createOrder()` | 步骤 8 后调用 `messageService.sendOrderMessage(...)` |
| `MessageConstant` | 新增消息模块提示语常量 |
| `sql/message.sql`（server-side/sql/） | 建表语句，同步团队文档中心 |

## 四、关键流程

### 4.1 WebSocket 连接（前端）

```
ws://{host}:{port}/api/ws?token={登录返回的token}
→ WsTokenHandshakeInterceptor 校验 Redis antique:token:{token}
→ 校验通过：attributes 写入 userId，握手成功
→ 校验失败：拒绝连接（握手 401）
→ 连接建立：MessageWebSocketHandler 注册会话（userId → Set<Session>）
→ 连接关闭/异常：移除会话
```

### 4.2 下单触发推送

```
POST /api/order/create（买家下单）
→ createOrder() 事务内：锁定藏品、写 orders、写 order_items
→ 步骤 8：messageService.sendOrderMessage(userId, orderNo)
   ├─ ① INSERT message（type=2, title=订单消息, content=您的订单 {orderNo} 已创建成功，请尽快完成支付）
   └─ ② 若用户在线：WebSocketSessionManager.sendToUser(userId, {type:newMessage, data:{...}})
       失败仅记日志，不影响下单
```

### 4.3 标记已读

```
POST /api/message/read {messageId}
→ 仅更新本人消息：UPDATE message SET is_read=1, read_time=NOW() WHERE id=? AND user_id=?
→ 幂等：已读过的消息重复调用也成功（不带 is_read=0 条件，与接口文档"无副作用"一致）
```

### 4.4 未读数

```
GET /api/message/unread-count
→ SELECT COUNT(*) FROM message WHERE user_id=? AND is_read=0 AND deleted_time IS NULL
```

## 五、推送协议（服务端 → 客户端）

```json
{
  "type": "newMessage",
  "data": {
    "id": 1,
    "messageType": 2,
    "title": "订单消息",
    "content": "您的订单 20260816103000123456 已创建成功，请尽快完成支付",
    "createdTime": "2026-08-16 10:30:00"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | 消息类型标识，V1 仅 `newMessage` |
| data.id | long | 消息 ID |
| data.messageType | int | 消息类型（1-系统 2-订单 3-AI 助手） |
| data.title | string | 标题 |
| data.content | string | 内容 |
| data.createdTime | string | 创建时间 yyyy-MM-dd HH:mm:ss |

## 六、接口清单

| 接口 | 路径 | 说明 |
|------|------|------|
| 消息列表 | `GET /api/message/list?page=1&size=10` | 分页，按创建时间倒序 |
| 未读数 | `GET /api/message/unread-count` | 角标轮询/进入页面拉取 |
| 标记已读 | `POST /api/message/read` `{messageId}` | 单条已读 |
| WebSocket | `GET /api/ws?token=xxx` | 实时推送通道（升级协议） |

均需认证（🔒），当前用户 ID 从 `UserContext` 获取，无法伪造他人消息。

## 七、容错与边界

| 场景 | 处理 |
|------|------|
| WebSocket 推送失败 / 用户离线 | 仅记日志；消息已落库，前端进页面/轮询未读数兜底 |
| Redis 不可用（握手校验失败） | 拒绝连接，前端自动重连 |
| 多端登录同一账号 | 每端一个会话，全部收到推送 |
| 断线重连 | 前端职责（文档中说明，指数退避重连） |
| 已读重复调用 | UPDATE 不带 is_read=0 条件，重复调用永远成功（幂等） |
| 消息只属于本人 | 所有查询/更新强制带 user_id 条件 |

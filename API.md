# PasswordDisk API 接口文档

本文档描述 Cloudflare Worker（D1 数据库）提供的全部 HTTP 接口，实现代码见 `workers/index.js`。

## 基本信息

| 项目 | 说明 |
| --- | --- |
| 基础地址 | `https://password-disk.ilei.workers.dev` |
| 数据格式 | 请求与响应均为 `application/json`（`404` 除外，返回纯文本） |
| 认证方式 | 无 Token / Session，敏感操作在请求体中携带用户名和密码即时校验 |
| 跨域 | 允许所有来源；`Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS`；`Access-Control-Allow-Headers: Content-Type, Authorization` |
| 数据库 | Cloudflare D1，绑定名 `DB` |

## 接口总览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/register` | 注册账号 |
| `POST` | `/api/login` | 登录，可同时上传密码库备份 |
| `DELETE` | `/api/delete-account` | 校验用户名密码后删除账号及关联数据 |
| `GET` | `/api/passwords` | 获取指定用户的密码列表 |
| `POST` | `/api/passwords` | 新增密码记录 |
| `PUT` | `/api/passwords` | 更新密码记录 |
| `DELETE` | `/api/passwords` | 删除密码记录 |
| `POST` | `/api/sync` | 校验账号并获取密码库备份 |

## 通用响应约定

成功响应均包含 `success: true`（`GET /api/passwords` 例外，直接返回 `passwords` 数组）。

错误响应：

```json
{
  "error": "错误描述"
}
```

服务端异常时可能额外包含 `full_error` 字段（调试用）：

```json
{
  "error": "错误描述",
  "full_error": "详细错误信息"
}
```

| 状态码 | 含义 |
| --- | --- |
| `200` | 成功 |
| `400` | 缺少必要参数 |
| `401` | 用户不存在 / 密码错误 |
| `404` | 路径或方法不匹配（纯文本 `Not Found`） |
| `409` | 用户名已存在 |
| `500` | 数据库或服务端异常 |

---

## 1. 注册账号

`POST /api/register`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名，需唯一 |
| `password` | string | 是 | 登录密码 |

```json
{
  "username": "alice",
  "password": "secret123"
}
```

### 响应

成功（`200`）：

```json
{
  "success": true,
  "id": "1660000000000abcdef12345"
}
```

失败：

| 状态码 | 响应 |
| --- | --- |
| `400` | `{ "error": "缺少必要参数" }` |
| `409` | `{ "error": "用户名已存在" }` |
| `500` | `{ "error": "数据库查询失败" }` 等 |

### 示例

```bash
curl -X POST "https://password-disk.ilei.workers.dev/api/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'
```

### 说明

- 用户 ID 为「毫秒时间戳 + 9 位随机字符串」。
- 密码以 `Base64(password + salt)` 形式存储，盐为随机字符串。

---

## 2. 登录

`POST /api/login`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 登录密码 |
| `backup` | string | 否 | 密码库备份内容；传入时写入/更新备份表 |

```json
{
  "username": "alice",
  "password": "secret123",
  "backup": "{\"passwords\":[]}"
}
```

### 响应

成功（`200`）：

```json
{
  "success": true,
  "id": "1660000000000abcdef12345"
}
```

失败：

| 状态码 | 响应 |
| --- | --- |
| `400` | `{ "error": "缺少必要参数" }` |
| `401` | `{ "error": "用户不存在" }` 或 `{ "error": "密码错误" }` |
| `500` | 数据库异常 |

### 示例

```bash
curl -X POST "https://password-disk.ilei.workers.dev/api/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'
```

### 说明

- 传入 `backup` 时：该用户已有备份则更新，否则新增。
- 备份写入失败会返回 `500`，而不是登录成功。

---

## 3. 删除账号

`DELETE /api/delete-account`

校验用户名和密码，通过后在同一个事务中删除账号及其关联的密码信息。

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 登录密码 |

```json
{
  "username": "alice",
  "password": "secret123"
}
```

### 响应

成功（`200`）：

```json
{
  "success": true
}
```

失败：

| 状态码 | 响应 |
| --- | --- |
| `400` | `{ "error": "缺少必要参数" }` |
| `401` | `{ "error": "用户不存在" }` 或 `{ "error": "密码错误" }` |
| `500` | 数据库异常 |

### 示例

```bash
curl -X DELETE "https://password-disk.ilei.workers.dev/api/delete-account" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'
```

### 说明

删除范围（按顺序，事务内执行）：

1. `passwords` 表中 `user_id` 对应的密码记录
2. `password` 表中 `user_id` 对应的密码库备份
3. `users` 表中对应账号

> 注意：删除后不可恢复。

---

## 4. 获取密码列表

`GET /api/passwords?user_id={user_id}`

### 查询参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `user_id` | string | 是 | 用户 ID（登录/注册接口返回的 `id`） |

### 响应

成功（`200`）：

```json
{
  "passwords": [
    {
      "id": 1660000000000,
      "user_id": "1660000000000abcdef12345",
      "password": "加密后的密码内容",
      "created_at": 1660000000000,
      "updated_at": 1660000000000
    }
  ]
}
```

无数据时返回：

```json
{
  "passwords": []
}
```

失败：

| 状态码 | 响应 |
| --- | --- |
| `400` | `{ "error": "缺少用户ID" }` |
| `500` | 数据库异常 |

### 示例

```bash
curl "https://password-disk.ilei.workers.dev/api/passwords?user_id=1660000000000abcdef12345"
```

### 说明

- 返回的表字段仅包含 `id`、`user_id`、`password`、`created_at`、`updated_at`。
- `passwords` 表未做账号校验，仅凭 `user_id` 查询。

---

## 5. 新增密码

`POST /api/passwords`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `user_id` | string | 是 | 用户 ID |
| `password` | string | 是 | 密码内容（由客户端加密后上传） |

```json
{
  "user_id": "1660000000000abcdef12345",
  "password": "加密后的密码内容"
}
```

### 响应

成功（`200`）：

```json
{
  "success": true,
  "id": 1660000000000
}
```

失败：

| 状态码 | 响应 |
| --- | --- |
| `400` | `{ "error": "缺少必要参数" }` |
| `500` | 数据库异常 |

### 示例

```bash
curl -X POST "https://password-disk.ilei.workers.dev/api/passwords" \
  -H "Content-Type: application/json" \
  -d '{"user_id":"1660000000000abcdef12345","password":"加密后的密码内容"}'
```

### 说明

- `title`、`username`、`url`、`notes`、`category` 会写入空字符串。
- `id` 为毫秒时间戳。

---

## 6. 更新密码

`PUT /api/passwords`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | number | 是 | 密码记录 ID |
| `user_id` | string | 是 | 用户 ID |
| `password` | string | 是 | 新的密码内容 |

```json
{
  "id": 1660000000000,
  "user_id": "1660000000000abcdef12345",
  "password": "新的加密内容"
}
```

### 响应

成功（`200`）：

```json
{
  "success": true
}
```

失败：

| 状态码 | 响应 |
| --- | --- |
| `400` | `{ "error": "缺少必要参数" }` |
| `500` | 数据库异常 |

### 示例

```bash
curl -X PUT "https://password-disk.ilei.workers.dev/api/passwords" \
  -H "Content-Type: application/json" \
  -d '{"id":1660000000000,"user_id":"1660000000000abcdef12345","password":"新的加密内容"}'
```

### 说明

- 仅更新 `id` 与 `user_id` 同时匹配的记录。

---

## 7. 删除密码

`DELETE /api/passwords`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | number | 是 | 密码记录 ID |
| `user_id` | string | 是 | 用户 ID |

```json
{
  "id": 1660000000000,
  "user_id": "1660000000000abcdef12345"
}
```

### 响应

成功（`200`）：

```json
{
  "success": true
}
```

失败：

| 状态码 | 响应 |
| --- | --- |
| `400` | `{ "error": "缺少必要参数" }` |
| `500` | 数据库异常 |

### 示例

```bash
curl -X DELETE "https://password-disk.ilei.workers.dev/api/passwords" \
  -H "Content-Type: application/json" \
  -d '{"id":1660000000000,"user_id":"1660000000000abcdef12345"}'
```

### 说明

- 仅删除 `id` 与 `user_id` 同时匹配的记录。

---

## 8. 同步密码库

`POST /api/sync`

校验用户名密码后，返回该用户保存的密码库备份。

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 登录密码 |

```json
{
  "username": "alice",
  "password": "secret123"
}
```

### 响应

成功（`200`）：

```json
{
  "success": true,
  "backup": "{\"passwords\":[]}"
}
```

无备份时：

```json
{
  "success": true,
  "backup": null
}
```

失败：

| 状态码 | 响应 |
| --- | --- |
| `400` | `{ "error": "缺少必要参数" }` |
| `401` | `{ "error": "用户不存在" }` 或 `{ "error": "密码错误" }` |
| `500` | 数据库异常 |

### 示例

```bash
curl -X POST "https://password-disk.ilei.workers.dev/api/sync" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'
```

---

## 跨域预检

任意路径的 `OPTIONS` 请求返回 `204 No Content`，并附带 CORS 响应头，不进入业务逻辑。

---

## 数据表结构

```sql
-- 用户
CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  username TEXT,
  password_hash TEXT,
  salt TEXT,
  created_at INTEGER,
  updated_at INTEGER
);

-- 密码记录
CREATE TABLE IF NOT EXISTS passwords (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL,
  title TEXT,
  username TEXT,
  password TEXT,
  url TEXT,
  notes TEXT,
  category TEXT,
  created_at INTEGER,
  updated_at INTEGER
);

-- 密码库备份（每个用户一条）
CREATE TABLE IF NOT EXISTS password (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL,
  backup TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id)
);
```

## 安全说明

- 密码哈希为 `Base64(password + salt)`，属于可逆的编码方式，生产环境建议改用 `bcrypt`、`scrypt` 或 `Argon2`。
- `/api/passwords` 系列接口仅依赖 `user_id` 操作数据，未校验请求者身份，建议增加 Token 或签名校验。
- 建议对登录、删除账号等接口增加频率限制。

## 部署

```bash
npx wrangler deploy
```

首次部署前需在 `wrangler.toml` 中配置 D1 数据库绑定，并按 README 创建上述数据表。

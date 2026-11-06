# Google Play 上架清单

按顺序执行。带 ⚠️ 的是**会导致审核被拒**的硬性项。

---

## 0. 账号与前提（一次性）

- [ ] 注册 Google Play 开发者账号并完成**身份验证**（个人账号 $25，需身份证件 + 地址证明，审核 1–3 天）
- [ ] ⚠️ 若你的账号是 **2023 年 11 月之后注册的个人账号**：上架前必须先用**封闭测试**跑满
      **至少 12 名测试者连续 14 天**，之后才能申请正式发布。这是最常见的卡点，建议今天就先建封闭测试轨道。
- [ ] 准备一个能收信的邮箱（Play 审核沟通、用户申诉都用它）

---

## 1. 发布前必须替换的占位内容

| 位置 | 状态 | 值 / 说明 |
| --- | --- | --- |
| `app/src/main/res/values/strings.xml` → `privacy_policy_url` | ✅ 已填 | `https://openwit.net/posts/pdisk-privacy` |
| `app/src/main/res/values/strings.xml` → `terms_url` | ✅ 已填 | `https://openwit.net/posts/pdisk-service` |
| `app/src/main/res/values/strings.xml` → `support_email` | ⚠️ 待填 | 目前仍是 `support@example.com`，需替换为你的联系邮箱 |
| `store/privacy-policy-zh.html` / `-en.html` | ⚠️ 待填 | 【开发者名称】【support@example.com】【Cloudflare 全球边缘网络】 |
| `store/terms-zh.html` / `-en.html` | ⚠️ 待填 | 同上 |
| `store/privacy-policy.md` / `terms-of-service.md` | ⚠️ 待填 | 同上（Markdown 版，英文在前、中文在后） |
| Play 管理中心 → 应用内容 → 隐私政策 | ⚠️ 待填 | 填 `https://openwit.net/posts/pdisk-privacy` 即可，与站内页面同源 |

已在 `openwit.net` 上线的页面：

- 隐私政策：<https://openwit.net/posts/pdisk-privacy>
- 服务条款：<https://openwit.net/posts/pdisk-service>

这两行 URL 同时被应用内「设置 → 隐私与数据」与启动页的法律提示引用，改动只需动 `strings.xml` 一处。

Markdown 版（`store/privacy-policy.md`、`store/terms-of-service.md`）内容与 HTML 版一致，一份文件同时包含中英两版：
英文在上、中文在下，中间用水平分割线隔开。适合仓库内直接阅读，或交给支持 Markdown 的站点/文档平台渲染——
`openwit.net` 上的两篇文章即由这两个文件发布而来，后续更新时记得同步回仓库。

---

## 2. 构建正式产物

```bash
# 完整 release 构建（AAB 上传用 + APK 真机自测用）
/Users/apple/.gradle/wrapper/dists/gradle-8.11.1-bin/bpt9gzteqjrbo1mjrsomdt32c/gradle-8.11.1/bin/gradle bundleRelease assembleRelease
```

产物：

| 文件 | 用途 |
| --- | --- |
| `app/build/outputs/bundle/release/app-release.aab` | **上传到 Play 的文件** |
| `app/build/outputs/apk/release/app-release.apk` | 真机安装自测（需先卸载 debug 包，签名不同） |

当前配置（已核对）：

| 项 | 值 |
| --- | --- |
| applicationId | `com.passworddisk.app` |
| versionCode / versionName | `1` / `1.0.0` |
| minSdk / targetSdk / compileSdk | 26 / **36** / 36 |
| 代码混淆 | 已开启（R8 + 资源压缩），APK 约 2.1 MB |
| 签名 | `keystore/passworddisk-release.jks`，别名 `passworddisk`，口令见 `keystore.properties` |
| 防截屏 | 正式包开启 `FLAG_SECURE` |

> ⚠️ **`keystore.properties` 与 `keystore/*.jks` 已加入 `.gitignore`。**
> 请立刻把这两个文件备份到密码管理器或加密网盘。**密钥丢失 = 永远无法更新这个应用**（只能换包名重新上架）。

每次发版记得同时递增 `versionCode`（整数，必须比上一版大）与 `versionName`。

---

## 3. Play 管理中心配置

### 3.1 创建应用

- 应用名称：`密码保险库`（默认语言可设为「中文（简体）」，英文列表另建）
- 类型：应用；免费
- 声明：遵守开发者计划政策、美国出口法

### 3.2 商店设置

- [ ] 类别：**工具**
- [ ] 联系邮箱：你的邮箱
- [ ] 网站（可选）：隐私政策站点
- [ ] 外部营销：否（如勾选"是"，需额外提供营销opt-out 说明）

### 3.3 主要商品详情

按 `store/listing-zh.md` / `listing-en.md` 填写：应用名称、简短说明、完整说明。

图形素材：

- [ ] 应用图标：`store/assets/icon-512.png`（512×512，无透明）
- [ ] 特色图片：`store/assets/feature-1024x500-zh.jpg`（中文列表）/ `-en.jpg`（英文列表）
- [ ] 手机截图：至少 2 张，建议 4–6 张，见 `store/screenshots/README.md`

### 3.4 应用内容（逐项都要填完，缺一项就不能提交）

| 项目 | 填什么 |
| --- | --- |
| 隐私政策 | ⚠️ 必填，粘贴你托管后的链接 |
| 应用访问权限 | ⚠️ 见下方 3.5 |
| 广告 | 否 |
| 内容分级 | 按 `store/content-rating.md` 填问卷 |
| 目标受众 | 13 岁以上，不面向儿童 |
| 新闻应用 | 否 |
| 新冠追踪应用 | 否 |
| 数据安全 | 按 `store/data-safety.md` 填 |
| 政府应用 | 否 |
| 金融功能 | 否（不属于金融产品，不涉及支付/借贷/加密资产） |
| 健康应用 | 否 |

### 3.5 应用访问权限（App access）⚠️

审核员必须能完整体验应用。本应用的主密码是本地生成的，**不需要你提供账号**，但要写清说明，否则容易被判"无法访问"：

```
This app is a local password manager and works without any account.

To review:
1. Launch the app. On first launch you will see the "Create master password" screen.
2. Enter any password of 6+ characters (e.g. Review2026) and tap "Create vault".
3. You are now in the vault. Tap the "+" button at the bottom right to add an entry.
4. The "2FA" tab at the bottom generates TOTP codes; camera permission is optional
   (you can paste a Base32 secret such as JBSWY3DPEHPK3PXP instead of scanning).
5. Cloud backup (Settings → Cloud sync) is optional and requires registration.
   It is not needed to review the core features.

No credentials are pre-provisioned because the vault key is derived entirely on-device.
```

---

## 4. 上传与发布

1. [ ] 发布 → 正式版 → 创建新版本 → 上传 `app-release.aab`
2. [ ] ⚠️ 首次上传会要求选择 **Play 应用签名**：选择「让 Google 管理并保护我的应用签名密钥」（推荐），
       之后你本地这个 jks 会变成"上传密钥"
3. [ ] 填写版本说明（Release notes）：
   ```
   首个版本：本地加密密码库、分类管理、密码生成器、2FA 验证码（支持扫码导入）、可选云端备份。
   ```
4. [ ] 先发**内部测试**轨道自测 → 再发**封闭测试**（个人账号需 12 人 × 14 天）→ 最后提审正式版
5. [ ] 审核通常 1–7 天；被拒时按邮件里的政策条款逐条回复申诉，不要重复提交相同包

---

## 5. 审核常见拒审原因（已在本项目规避）

| 风险点 | 当前状态 |
| --- | --- |
| targetSdk 过低 | ✅ 已升到 36（2026-08-31 起强制） |
| 缺少隐私政策 | ✅ 应用内两个入口 + 待你填 Play 链接 |
| 有账号但无删除入口 | ✅ 设置页「删除数据」单一入口，本机与云端一并删除；用过云端只需补密码，未用过不显示任何输入框，见第 6.1 节 |
| 未声明相机权限用途 | ✅ 应用内首次点击时申请，拒绝后给提示，不影响主功能 |
| 明文 HTTP 传输 | ✅ `network_security_config.xml` 全局禁用 cleartext |
| 敏感数据被备份到 Google 云 | ✅ `allowBackup=false`，备份与换机直传均排除 |
| 正式包带调试日志 | ✅ 网络日志仅在 debug 输出，`println` 已清除 |
| 缺少自适应图标 / 主题图标 | ✅ 已补 `monochrome` 层 |

---

## 6. 服务端接口：注销云端账号

设置页「删除数据」在检测到本机记录过云端账号时会调用（接口已按 `API.md` 对齐）：

```
DELETE https://password-disk.ilei.workers.dev/api/delete-account
Content-Type: application/json

{ "username": "<云端用户名>", "password": "<云端密码明文>" }
```

期望响应 `{ "success": true }`；服务端在事务内依次删除
`passwords`（该用户的密码记录）→ `password`（密码库备份）→ `users`（账号）。

客户端对返回码的处理：

| 状态码 | 应用内表现 |
| --- | --- |
| `200` | 提示「云端账号已注销」 |
| `400` | 提示「注销失败，请检查网络后重试」 |
| `401` | 提示「用户名或密码错误」，对话框不关闭，可重试 |
| `500` / 网络异常 | 提示「注销失败，请检查网络后重试」 |

> 注意：旧版本曾按 `POST /api/delete_account`（下划线）实现，与 `API.md` 的连字符路径不一致，已修正。
> 若服务端保留旧路径，两套都能用；只保留新路径时旧版客户端会拿到 404，建议服务端对新路径优先。

---

## 6.1 删除入口的设计

Google Play 的账号删除政策要求「允许创建账号的应用必须提供应用内删除账号入口」。
应用内只保留一个「设置 → 删除数据」入口，一次删干净：本机密码库 + 云端账号。

| 本机是否记录过云端账号 | 对话框 | 实际动作 |
| --- | --- | --- |
| 是（注册 / 同步成功时记住用户名） | 显示「云端账号：xxx」+ 密码输入框 | 校验密码 → `DELETE /api/delete-account` → 成功后 `wipeAll()` |
| 否 | 无任何输入框 | 直接 `wipeAll()`，不联网 |

实现要点：

- 用户名与最近同步时间存在 `CloudAccountStore`（SharedPreferences），**不存密码**。
- 顺序是先删云端、成功后才清本机：401 提示「用户名或密码错误」，其他失败提示检查网络，本机数据保持完好可重试。
- 同步页读取同一份记录：隐藏用户名输入框、只留密码框，并显示「最近同步：…」；需要换账号时点「切换账号」。
- 云端账号删除成功后 `CloudAccountStore.clear()`，避免界面仍提示已绑定某账号。

已知限制（审核时如实说明）：

- 删除入口在设置页，需先解锁保险库。若用户已卸载应用或忘记主密码，应用内无法进入设置页，此时通过隐私政策页面公布的邮箱申请删除（7 个工作日内完成）。隐私政策「账号注销说明」已写明这条路径。

---

## 7. 发布后

- [ ] 在 Play 管理中心开启 **Android Vitals** 监控崩溃与 ANR
- [ ] 保留一份 `app-release.aab` + 对应 `mapping.txt`（`app/build/outputs/mapping/release/`），用于还原崩溃堆栈
- [ ] 每次改版前检查：`versionCode` 递增、隐私政策与实际行为是否仍一致

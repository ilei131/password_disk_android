# Privacy Policy · Password Disk

*This document contains two language versions: English first, then 简体中文.*

Effective: November 4, 2026 · Last updated: November 4, 2026

> **In one sentence:** your vault lives on your phone by default — we cannot see it and cannot decrypt it. Data only leaves the device when you explicitly turn on cloud backup. The app contains no ads, analytics or profiling SDKs.

This policy applies to **Password Disk** (Android package name `com.passworddisk.app`, "the App"), developed and operated by [Developer name] ("we", "us"). We process data in accordance with the Google Play User Data Policy, the EU GDPR, the Personal Information Protection Law of the People's Republic of China (PIPL) and other applicable data-protection laws.

## 1. Information we process

### 1.1 Stored only on your device

| Data | Purpose and handling |
| --- | --- |
| Master password | Unlocks your vault. **We never store the master password in plaintext.** Only a bcrypt hash (cost 12) is kept on the device for verification. |
| Vault entries | Title, username, password, URL, notes and category. All fields are encrypted with AES before being written to the local database; the key is derived from your master password. |
| 2FA secrets and codes | Secrets stay on the device and codes are computed locally in real time. **Nothing is uploaded.** |

### 1.2 Uploaded only if you enable cloud backup

- **Account username** — identifies your backup space.
- **Password hash** — the server stores only a bcrypt hash, from which the plaintext cannot be recovered.
- **Encrypted vault backup** — encrypted before it leaves the device, so the server cannot read the passwords inside.

This data is transmitted over HTTPS (TLS) and stored on [Cloudflare global edge network]. If you never enable cloud backup, this section does not apply.

### 1.3 Camera permission

Requested only when you tap "Scan QR code", to read 2FA QR codes (`otpauth://`). The viewfinder is decoded on-device; **no image is stored or uploaded**. You can revoke the permission at any time in Android Settings; only QR scanning stops working, every other feature keeps working.

### 1.4 What we do NOT collect

- Contacts, SMS, call logs, or location (precise or approximate)
- Device identifiers (IMEI, Android ID, advertising ID)
- Usage analytics, click tracking, or crash reports (unless you email one to us voluntarily)
- Clipboard content unrelated to the App

## 2. Permissions

| Permission | Purpose | Required? |
| --- | --- | --- |
| INTERNET | Only for cloud backup you explicitly start | No — the App works fully offline |
| ACCESS_NETWORK_STATE | Shows a friendly message when offline instead of failing silently | No |
| CAMERA | Scanning 2FA QR codes | No — you can type the secret manually |

We do not request storage, contacts, location, phone or SMS permissions.

## 3. Storage and retention

- **On-device data** — kept until you delete an entry, use "Settings → Delete data", or uninstall the App.
- **Cloud backup** — kept until you delete your account.
- **After deletion** — backups are permanently removed from production and rolling backups within 30 days.

## 4. Sharing and disclosure

We **do not sell, rent or share** your personal information for advertising or marketing. We disclose data only (a) with your explicit consent, or (b) when required by law or a binding order from a competent authority.

Apart from the hosting required for opt-in cloud backup, the App integrates no third-party advertising, analytics or attribution SDKs.

## 5. Your rights

- **Access** — view every entry and category in the App at any time.
- **Rectify** — edit any entry.
- **Delete data** — "Settings → Delete data" removes every password, category and your master password from this device, together with the linked cloud account and its backup. Only the account password is required, because the username is remembered on this device; on-device data is cleared only after the server confirms deletion.
- **Withdraw consent** — uninstall the App to stop all processing, or email us.
- **Complain** — you may lodge a complaint with your local data-protection authority.

## 6. Account deletion

Your cloud account and local data are deleted together from a single entry:

- **If you have used cloud backup** — Settings → Delete data. The dialog shows the saved account name; enter that account's password to confirm. After verification the server immediately deletes the account, its vault backup and all password records, then the on-device data is cleared.
- **If you never used cloud backup** — the same entry shows no input fields; confirming deletes this device's passwords, categories and master password.
- **After uninstalling or forgetting the master password** — if you cannot reach Settings, email us to delete the cloud data; we verify and complete within 7 business days.

No support request is required for in-app deletion and it takes effect immediately and cannot be undone.

If you already uninstalled the App but want your cloud backup removed, email [support@example.com](mailto:support@example.com) with the subject "Delete my account" and your cloud username. We verify and delete within 7 business days and confirm by reply.

## 7. Children

The App is not directed to children under 13 (under 16 in the EEA). We do not knowingly collect personal data from children. If you are a guardian and believe a child used the App without consent, contact us and we will help delete the data.

## 8. Security measures

- **In transit** — HTTPS everywhere; cleartext HTTP is blocked at the platform level.
- **At rest** — AES encryption for entries, key material protected by the Android Keystore.
- **Screenshots** — production builds set `FLAG_SECURE`, blocking screenshots, screen recording and recent-task thumbnails.
- **Clipboard** — copied passwords are flagged as sensitive on Android 13+ and cleared after 60 seconds.
- **No secret logging** — release builds never log passwords or backup content.

Please keep your master password safe. **If you lose it, we cannot recover your vault** — that is the unavoidable cost of end-to-end encryption.

## 9. Changes to this policy

When this policy changes we update this page and the in-app link, and revise the "Last updated" date. For material changes to purposes or processing we will notify you prominently in the App and, where required, ask for consent again.

## 10. Contact

Developer: [Developer name]<br>
Email: [support@example.com](mailto:support@example.com)

We usually reply within 7 business days.

---

# 隐私政策 · 密码保险库

*本文档包含两个语言版本：English 在上，简体中文在下。*

生效日期：2026 年 11 月 4 日　·　最后更新：2026 年 11 月 4 日

> **一句话概括：** 你的密码库默认只存在于你的手机上，我们看不到、也解不开。只有当你主动开启「云端备份」时，才会有加密后的数据离开设备；不开启则完全离线运行。本应用不含任何广告、统计或用户画像 SDK。

本政策适用于「密码保险库」（英文名 Password Disk，Android 应用包名 `com.passworddisk.app`，以下简称"本应用"）。本应用由【开发者名称】开发并运营（以下简称"我们"）。我们依据《中华人民共和国个人信息保护法》《网络安全法》、Google Play 用户数据政策，以及欧盟 GDPR 等适用的数据保护法律处理你的信息。

## 一、我们处理哪些信息

### 1. 仅保存在你设备上的信息

| 数据 | 用途与处理方式 |
| --- | --- |
| 主密码 | 用于解锁密码库。**我们从不保存明文主密码**，仅在本机保存经 bcrypt（cost 12）计算的哈希值用于校验。 |
| 密码条目 | 标题、账号、密码、网址、备注、分类。全部使用 AES 加密后写入本机数据库，密钥由你的主密码派生。 |
| 2FA 密钥与验证码 | 用于生成动态验证码。密钥保存在本机，验证码实时在设备上计算，**不会上传**。 |

### 2. 你主动开启「云端备份」时上传的信息

- **账号用户名**：用于标识你的备份空间。
- **账号密码的哈希值**：服务端仅保存 bcrypt 哈希，无法反推出明文。
- **加密后的密码库备份文件**：内容在离开设备前已加密，服务器无法读取其中的密码明文。

上述数据通过 HTTPS（TLS）加密传输，存放于【Cloudflare 全球边缘网络】。若你不使用云端备份，本节不适用。

### 3. 相机权限

仅在你点击「扫描二维码」时申请与调用，用于识别 2FA 二维码（`otpauth://`）。取景画面在设备本地实时解析，**不保存、不上传任何图像**。你可以随时在系统「设置 → 应用 → 密码保险库 → 权限」中撤销该权限；撤销后仅扫码功能不可用，其余功能不受影响。

### 4. 我们不会收集的信息

- 通讯录、短信、通话记录、精确或粗略位置
- 设备唯一标识（IMEI、Android ID、广告 ID 等）
- 应用使用行为、点击埋点、崩溃日志（除非你主动通过邮件发送给我们）
- 剪贴板中与本应用无关的内容

## 二、权限说明

| 权限 | 用途 | 是否必需 |
| --- | --- | --- |
| 网络访问（INTERNET） | 仅用于你主动发起的云端备份与同步 | 否，可完全离线使用 |
| 查看网络状态（ACCESS_NETWORK_STATE） | 在无网络时给出友好提示，避免无意义的请求 | 否 |
| 相机（CAMERA） | 扫描 2FA 二维码 | 否，也可手动输入密钥 |

本应用不申请存储、通讯录、位置、电话、短信等任何其他敏感权限。

## 三、数据存储与保留期限

- **本机数据**：保存在你的设备上，直到你删除条目、执行「设置 → 删除数据」，或卸载应用。
- **云端备份**：保存在上述服务器上，直到你注销账号。
- **注销后**：云端备份将在 30 日内从生产环境及滚动备份中彻底删除，且不可恢复。

## 四、信息的共享与披露

我们**不出售、不出租、不以广告或营销为目的**共享你的个人信息。仅在下列情形下披露：

- 取得你的明确同意；
- 依据法律法规，或应司法机关、行政机关依法定程序提出的强制性要求。

除云端备份所必需的服务端存储外，本应用未集成任何第三方广告、统计或数据分析 SDK。

## 五、你的权利

- **访问**：在应用内随时查看全部密码条目与分类。
- **更正**：随时编辑任意条目。
- **删除数据**：进入「设置 → 删除数据」即可一次性删除设备上的全部密码、分类、主密码，以及已绑定的云端账号与其备份。云端账号只需输入密码确认（用户名已保存在本机），云端删除成功后才清空本机。若已卸载应用或忘记主密码，可用下方邮箱申请删除云端数据。
- **撤回同意**：卸载应用即可终止全部数据处理；或通过下方邮箱联系我们。
- **投诉**：如对我们的处理方式有异议，可向所在地网信或市场监管部门投诉。

## 六、账号注销说明

云端账号与本地数据在同一个入口里一并删除，不需要分别操作：

- **用过的云端账号**：设置 → 删除数据，对话框会显示该账号的用户名，只需再输入一次密码即可确认。服务端校验通过后立即删除账号及其密码库备份、密码记录，随后清空本机数据。
- **从未用过云端账号**：同一入口不会出现任何输入框，确认后直接删除本机密码、分类与主密码。
- **已卸载 / 忘记主密码**：无法进入设置页时，请用下方邮箱申请删除云端数据，我们核验后在 7 个工作日内完成。

应用内删除不需要额外申请或联系客服，操作完成后立即生效且不可恢复。

若你已卸载应用但仍希望删除云端备份，请发送邮件至 [support@example.com](mailto:support@example.com)，邮件标题注明「注销账号」，正文提供你的云端用户名。我们将在收到请求后 7 个工作日内完成核验与删除，并回复确认。

## 七、未成年人保护

本应用不面向 13 周岁以下的儿童（欧盟经济区为 16 周岁以下）。我们不会有意收集儿童的个人信息。若你是未成年人的监护人，发现其未经同意使用了本应用，请联系我们，我们会协助删除相关数据。

## 八、我们采取的安全措施

- **传输加密**：全站 HTTPS，禁用明文 HTTP 请求。
- **存储加密**：密码条目使用 AES 加密，密钥材料受 Android Keystore 保护。
- **防截屏**：正式版本禁止截屏与录屏，避免密码在屏幕共享或后台缩略图中泄露。
- **剪贴板保护**：复制的密码在 Android 13 及以上标记为敏感内容，60 秒后自动清空。
- **无日志承诺**：正式版本不输出包含密码或备份内容的调试日志。

请务必妥善保管你的主密码。**主密码一旦遗失，我们无法为你恢复密码库中的数据**，这是端到端加密的必然代价。

## 九、政策更新

本政策更新时，我们会同步修改本页面与应用内入口，并更新页面顶部的「最后更新」日期。若涉及处理目的、处理方式的重大变更，我们会在应用内以显著方式提示，必要时重新征得你的同意。

## 十、联系我们

开发者：【开发者名称】<br>
联系邮箱：[support@example.com](mailto:support@example.com)

我们通常在 7 个工作日内回复。

---

© 2026 Password Disk. This page may be saved or printed for your records. 本页面可离线保存或打印留存。

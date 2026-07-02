# AGENT.md

本文件为 AI 智能体在本项目工作提供权威参考。开始任何修改前，请先通读本文档。

---

## 1. 项目概述

**AppMarketBlog**：Android 应用市场与博客客户端，采用 Apple Today 风格黑白配色。用户以设备指纹自动登录（无注册），首个设备自动成为站长。支持应用上传（GitHub 仓库抓取 / 本地 APK 直传）、三级审核、镜像站下载加速、信息流首页、文章/主题/推荐内容、Java 插件与 XML 数据源插件扩展。

- 当前版本：`0.5.1`（versionCode=11）
- 单 Gradle 模块：`:app`
- 后端：Supabase（PostgreSQL + Edge Functions + Storage），客户端仅持 Publishable key

---

## 2. 技术栈

| 项 | 版本 | 来源 |
|----|------|------|
| Kotlin | 2.0.20 | `gradle/libs.versions.toml` |
| AGP | 8.5.2 | 同上 |
| Gradle | 8.9 | `gradle/wrapper/gradle-wrapper.properties` |
| Java | 17（必须显式指定，沙箱默认 25 不兼容） | `gradle.properties:3` |
| compileSdk / targetSdk / minSdk | 34 / 34 / 26 | `app/build.gradle.kts` |
| Compose BOM | 2024.09.02 | 使用 Kotlin 2.0 Compose Compiler 插件 |
| Hilt | 2.52 | KSP 处理注解 |
| Supabase Kotlin SDK | 3.0.0 | Postgrest + Functions + Storage + Realtime |
| Ktor | 3.0.0 | |
| kotlinx-serialization | 1.7.3 | |
| Coil | 2.7.0 | 图像加载 |
| Markwon | 4.2.2 | Markdown 渲染（core + tables + tasklist + strikethrough） |
| Navigation Compose | 2.8.1 | |
| security-crypto | 1.1.0-alpha06 | EncryptedSharedPreferences |
| WorkManager | 2.9.1 | |

---

## 3. 构建与运行命令

### 3.1 环境变量（必须）

```bash
export JAVA_HOME=<java17路径>
export ANDROID_SDK_ROOT=<android-sdk路径>
export PATH="$JAVA_HOME/bin:$PATH"
```

### 3.2 编译与构建

所有命令在项目根下执行：

```bash
# 仅编译 Kotlin（快速验证语法）
gradle :app:compileDebugKotlin -x lint --console=plain --no-daemon

# 构建 debug APK
gradle :app:assembleDebug -x lint --console=plain --no-daemon
# 产物：app/build/outputs/apk/debug/app-debug.apk

# 构建 release APK（启用 minify + shrinkResources，复用 debug keystore 签名）
gradle :app:assembleRelease -x lint --console=plain --no-daemon
# 产物：app/build/outputs/apk/release/app-release.apk
```

**注意**：
- 沙箱环境下务必加 `-x lint` 与 `--no-daemon`，否则会卡住或失败
- `gradle.properties` 已显式禁用并行与 worker（`org.gradle.parallel=false`、`org.gradle.workers.max=1`），不要改回
- debug keystore 缺失时用 `keytool` 生成（口令均为 `android`，alias 为 `androiddebugkey`）

### 3.3 测试与 Lint

当前项目无单元测试与 androidTest 用例。如需运行：

```bash
gradle :app:testDebugUnitTest
gradle :app:lintDebug
```

---

## 4. 强制规则（必须遵守）

以下规则来自用户明确要求，违反会导致返工：

### 4.1 严禁使用表情包

代码、注释、文档中**不得出现任何 emoji**。包括提交信息、TODO 注释、错误提示文案。

### 4.2 不修改项目语言

项目 UI 与注释均为中文，添加功能时保持中文，不要引入英文文案或英文注释。

### 4.3 中文注释

所有 KDoc 与行内注释用中文描述意图。复杂逻辑需注释「为什么这么做」，而不只是「做了什么」。

### 4.4 每次任务交付 APK

完成修改后必须构建 debug APK 并确认 `app/build/outputs/apk/debug/app-debug.apk` 存在。

### 4.5 「自然」分类禁用「破解」字样

`AppCategory.NATURAL` 在 UI、注释、文档中统一描述为「自然」或「对正版的 APK 进行的自然化」。**严禁**出现「破解」「crack」「Crack」等词汇。

### 4.6 客户端不持有敏感密钥

客户端**仅**持有 Supabase Publishable key（公开 key，前端可见）。数据库密码与 service_role key **绝不能**出现在客户端代码、资源、BuildConfig 或文档中（仅后端持有，本文档不列出具体值）。敏感写操作必须经 Edge Function。

---

## 5. 项目结构

### 5.1 源码包（`app/src/main/java/com/appmarket/blog/`）

```
根包       AppMarketApplication / MainActivity
core/      Anim / Format / Shape / Theme / Type（UI 基础常量）
core/download/  ApkInstaller / MirrorDownloader / PluginGitHubClient
core/security/  AppSecurity（签名校验 + 反调试）
data/cache/      DataCache（文件缓存）
data/model/      Models（全部领域模型）
data/plugin/     插件宿主侧 9 个文件（见第 8 节）
data/remote/     DeviceSession / SupabaseProvider
data/repo/       AppRepository（核心仓库，1423 行）
di/              AppModule（Hilt 模块）
plugin/sdk/      插件 SDK 4 个文件（对外暴露给插件作者）
ui/<功能>/        Screen + ViewModel 配对（16 个子目录）
ui/navigation/   AppNavGraph + Destinations（路由表）
```

### 5.2 UI 子包（每个含 Screen + ViewModel 配对）

`admin` `article` `components` `detail` `download` `feed` `manage` `messages` `navigation` `plugin` `publish` `review` `search` `settings` `theme_detail` `upload`

### 5.3 res 目录（极简）

- `values/strings.xml`：38 条中文文案
- `values/colors.xml`：Apple Today 黑白配色
- `values/themes.xml` + `values-night/themes.xml`：XML 主题（仅启动屏用）
- `xml/`：backup_rules / data_extraction_rules / file_paths
- **无 layout/ 目录**（纯 Compose）
- **无多语言变体**（仅中文）

### 5.4 文档

- `docs/JavaPlugin开发指南.md`：Java 插件开发权威文档
- `docs/XmlPlugin定时卡片.md`：XML 数据源插件 schedule 字段文档

---

## 6. 架构约定

### 6.1 整体架构：MVVM + 单向数据流

- **View**：Compose `@Composable`，无业务逻辑，仅订阅 StateFlow 与回调上行
- **ViewModel**：`@HiltViewModel`，注入 `AppRepository`，`viewModelScope.launch` 触发 suspend
- **Repository**：`AppRepository` 单例，封装 Supabase + 文件缓存
- **数据流**：`MutableStateFlow`（私有）→ `asStateFlow()`（只读）→ UI `collectAsStateWithLifecycle()` 订阅

### 6.2 ViewModel 约定

```kotlin
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {
    // 转发型：直接暴露 repo StateFlow
    val feed = repo.feed

    // 派生型：用 stateIn + WhileSubscribed(5000)
    val profileName = repo.profile.map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // 本地状态：MutableStateFlow + asStateFlow
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    fun doSomething() = viewModelScope.launch {
        runCatching { repo.something() }
            .onFailure { Log.e("XxxVM", "操作失败", it) }
    }
}
```

### 6.3 Hilt DI（`di/AppModule.kt`）

提供 6 个 `@Singleton`：`DeviceSession`、`AppRepository`、`PluginEntryRegistrar`、`PluginViewExtensionRegistrar`、`MirrorDownloader`、`PluginManager`。

**循环依赖打破**：`AppRepository` 与 `PluginManager` 互相调用，用 `javax.inject.Provider` 延迟解析。新增依赖时注意此模式。

### 6.4 导航路由（`ui/navigation/Destinations.kt`）

`object Routes` 含 10 个无参路由常量 + 4 个带参路由（`SEARCH`/`APP_DETAIL`/`ARTICLE`/`THEME_DETAIL`）+ 4 个工厂方法。启动目的地为 `Routes.FEED`。新增页面时：
1. 在 `Routes` 加常量（带参路由用 `{xxx}` 占位符）
2. 在 `AppNavGraph` 加 `composable(route) { ... }` 节点
3. 在对应 Screen 加 `onXxxClick` 回调参数

### 6.5 缓存策略

- 详情缓存 TTL 30 分钟（`DETAIL_TTL_MS`）
- 镜像站缓存 TTL 7 天（`MIRROR_TTL_MS`），网络失败时回退过期缓存
- stale-while-revalidate：先回填本地缓存，再异步拉取最新

---

## 7. 代码风格

### 7.1 命名

- **Screen**：`XxxScreen`（`@Composable`，无 ViewModel 后缀）
- **ViewModel**：`XxxViewModel`（`@HiltViewModel`）
- **Repository**：`AppRepository`（单数，不写 Repository 后缀）
- **数据类**：`XxxRow`（DTO）、`XxxEntry`（XML 条目）、`XxxDraft`（表单草稿）、`XxxResult`（sealed class）

### 7.2 import

**全限定导入，不使用通配符 `*`**。即使同一包下多个类也逐个 import。`kotlin.code.style=official`。

### 7.3 未配置代码风格工具

无 `.editorconfig`、无 ktlint、无 detekt。遵循 Kotlin 官方风格与现有代码惯例。

### 7.4 注解常用

`@HiltAndroidApp` / `@AndroidEntryPoint` / `@HiltViewModel` / `@Inject` / `@Module @InstallIn(SingletonComponent::class)` / `@Provides @Singleton` / `@ApplicationContext` / `@Composable` / `@Serializable` / `@Volatile` / `@OptIn`

---

## 8. 插件系统（核心扩展机制）

### 8.1 两类插件

- **XML_DATA_SOURCE**：纯数据源（应用/镜像站/文章/主题），无代码逻辑，由 `XmlDataSourcePlugin` 解析
- **JAVA**：基于 JAR 的代码插件，由 `DexClassLoader` 加载，实现 `JavaPlugin` 接口

### 8.2 插件 SDK（`plugin/sdk/`，对外暴露给插件作者）

- `JavaPlugin`：入口接口，`onCreate(context)` / `onDestroy()`
- `PluginContext`：5 组能力（本地存储 / 注册入口 / 公开应用快照 / 打开页面 / 视图扩展）
- `PluginPageDestination`：9 个白名单页面枚举
- `ViewExtensionSlot`：7 个容器插槽常量
- `ViewExtensionContext`：只读上下文（按插槽填充 appId/appPackageName/appCategory/articleId/articleType/themeId/slot）

### 8.3 插件权限（`PluginManifest.kt`）

7 项权限，5 项已实现（`LOCAL_STORAGE` / `REGISTER_ENTRY` / `READ_PUBLIC_APPS` / `OPEN_PAGE` / `REGISTER_VIEW_EXTENSION`），2 项预留（`NETWORK` / `NOTIFICATION`，当前申请也不授予）。**每个 API 调用前由 `PluginHostContext.ensurePermission` 校验**，未申请抛 `SecurityException`。

### 8.4 插件安全边界（修改插件代码必读）

- **disposed 保护**：`PluginHostContext.cleanup()` 后所有 API 抛 `IllegalStateException`
- **openPage 白名单**：9 个固定枚举，无法打开任意 Activity；`APP_DETAIL`/`ARTICLE`/`THEME_DETAIL` 必须传非空 id
- **registerViewExtension 上限**：单插件单插槽 4 个，卸载时自动清理
- **registerEntry 上限**：单插件 16 个
- **zip bomb 防御**：64 条目 / 2MB 单文件 / 16MB 总解压
- **packageName 校验**：仅字母数字点下划线，防路径穿越
- **GitHub 仓库地址校验**：`PluginGitHubClient.parseRepo` 严格校验 `https://github.com/owner/repo`
- **镜像站 https 强制**：`XmlMirrorEntry.toMirrorSite` 与所有 `MirrorSite.resolve` 调用点均校验解析后 URL 必须 https

### 8.5 7 个视图扩展插槽接线位置

| 插槽 | 位置 |
|------|------|
| `FEED_TOP` | `FeedScreen.kt` |
| `APP_DETAIL_HEADER` | `AppDetailScreen.kt`（header 下方） |
| `APP_DETAIL_INTRO_BOTTOM` | `AppDetailScreen.kt`（简介区底部） |
| `APP_DETAIL_BOTTOM` | `AppDetailScreen.kt`（下载按钮上方） |
| `ARTICLE_TOP` / `ARTICLE_BOTTOM` | `ArticleScreen.kt` |
| `THEME_DETAIL_HEADER` | `ThemeDetailScreen.kt` |

新增插槽时：在 `ViewExtensionSlot` 加常量 → 在对应 Screen 加 `PluginExtensionSlot` 组件 → 更新 `JavaPlugin开发指南.md`。

### 8.6 镜像站机制（`MirrorSite.resolve`）

两种 `siteType`：
- `"prefix"`：模板含 `{github_url}` 占位符，整体替换（如 `https://ghproxy.com{github_url}`）
- `"replace"`（默认）：模板含 `{path}` 占位符，仅取 GitHub URL 的 path 拼接（如 `https://mirror.com{path}`）

镜像站列表由后端 `mirror_sites` 表预置，XML 插件可通过 `<mirrors>` 节点追加。下载流程：优先站直连 → 测速排序候选 → 轮询下载，支持续传与停滞检测。

---

## 9. 后端集成

### 9.1 Supabase 配置

- 客户端安装 4 插件：Postgrest / Functions / Storage / Realtime
- `DeviceTokenPlugin` 自动为每个请求附加 `device_token` header

### 9.2 Edge Function 清单（12 个）

| Function | 用途 |
|---------|------|
| `create-or-get-profile` | 设备指纹登录 |
| `get-feed` | 获取信息流（公开接口） |
| `save-github-pat` | PAT 加密存入 Vault |
| `list-profiles` | 站长列出用户 |
| `assign-role` | 站长指派/撤销管理员 |
| `list-pending-reviews` | 待审核列表 |
| `manual-review` | 审核决策/下架/重新上架 |
| `submit-app-by-repo` | GitHub 仓库上传（开源） |
| `upload-apk-direct` | APK 直传（正版/自然，需 PAT） |
| `publish-content` | 发布文章/推荐/主题（仅站长/管理员） |
| `generate-inherit-code` | 生成继承码（10 分钟有效） |
| `execute-inherit` | 执行账号继承 |

### 9.3 device_token 与 RLS

- 设备指纹 = `ANDROID_ID + 厂商 + 型号` 的 SHA-256 取前 16 位
- `create-or-get-profile` 返回 device_token，注入 `SupabaseProvider.deviceToken`
- 所有请求由 `DeviceTokenPlugin` 自动携带 header，后端 `get_current_profile_id` 据此识别用户
- RLS 自动按 device_token 过滤为本人数据
- 写操作前 `ensureAuth()`：device_token 为空时重新登录补齐

---

## 10. 安全相关

### 10.1 签名校验（`core/security/AppSecurity.kt`）

- debug 构建直接放行
- release 构建校验 SHA-256 指纹，占位符 `RELEASE_SIGNATURE_PLACEHOLDER` 未替换时也放行（**正式发布前必须替换为真实指纹**）
- 反调试：`Debug.isDebuggerConnected()` + `/proc/self/status` 的 `TracerPid`
- 防截屏：`FLAG_SECURE`，仅在敏感界面（PAT 输入、继承码弹窗）按需设置

### 10.2 ProGuard 规则（`app/proguard-rules.pro`）

- 保留 `@Serializable` 的 `$$serializer` 与 `@SerialName` 字段
- 保留 `com.appmarket.blog.data.model.**`（JSON 解析需字段名）
- 保留 Hilt / Compose / Supabase / Ktor 生成类
- release 移除 `Log.v/d/i` 与 `println`
- 保留 `BuildConfig`（供运行期读取 publishable key）

### 10.3 网络安全

- `AndroidManifest` 未配置 `usesCleartextTraffic`，平台默认禁止明文 HTTP（targetSdk=34）
- 所有网络请求走 https
- 镜像站 URL 解析后双重校验 https（防 SSRF）
- `allowBackup="false"` 显式禁用备份

---

## 11. 调试与排错

### 11.1 常见构建问题

| 问题 | 解决 |
|------|------|
| `SDK location not found` | 确认 Android SDK 存在，含 `platforms/android-34` 与 `build-tools/34.0.0` |
| `Keystore file not found` | 用 `keytool` 生成 `~/.android/debug.keystore`（口令 `android`，alias `androiddebugkey`） |
| Kotlin 编译报 Java 版本错误 | 确认 `JAVA_HOME` 指向 Java 17，不是沙箱默认的 25 |
| 构建卡住 | 加 `-x lint --no-daemon`，确认 `gradle.properties` 的 `parallel=false` |

### 11.2 编译验证流程

修改代码后建议按此顺序验证：

1. `gradle :app:compileDebugKotlin -x lint --console=plain --no-daemon`（快速验证语法）
2. `gradle :app:assembleDebug -x lint --console=plain --no-daemon`（完整构建 APK）
3. 确认 `app/build/outputs/apk/debug/app-debug.apk` 存在且非空

### 11.3 审计约定

用户要求对新功能及全库进行审计时，建议流程：

1. 并行启动多个 `search` 子代理，按领域切分（安全 / 插件系统 / 下载镜像站 / 核心功能 UI）
2. 每个子代理逐文件审计，附文件路径与行号证据
3. 汇总发现，按优先级修复
4. 修复后重新审计验证
5. 构建最终 APK 交付

---

## 12. 修改清单参考

常见修改场景的注意事项：

### 12.1 新增页面

1. `ui/<功能>/` 下加 `XxxScreen.kt` + `XxxViewModel.kt`
2. `Destinations.kt` 加 `Routes.XXX` 常量
3. `AppNavGraph.kt` 加 `composable(Routes.XXX) { XxxScreen(...) }` 节点
4. 如需插件可跳转：在 `PluginPageDestination` 加枚举 + `AppNavGraph` 的 PluginNavigator 实现加 `when` 分支
5. 更新 `JavaPlugin开发指南.md` 第 7 章

### 12.2 新增插件视图插槽

1. `ViewExtensionContext.kt` 的 `ViewExtensionSlot` 加常量
2. 对应 Screen 加 `PluginExtensionSlot(slot) { ViewExtensionContext(...) }`
3. 更新 `JavaPlugin开发指南.md` 第 8.1 节插槽列表

### 12.3 新增 Edge Function 调用

1. `AppRepository` 加 suspend 方法，用 `SupabaseProvider.client.functions.invoke("fn-name")`
2. 写操作前调 `ensureAuth()`
3. 在 `proguard-rules.pro` 确认 Supabase 保留规则覆盖

### 12.4 修改镜像站逻辑

所有 `MirrorSite.resolve()` 调用点（共 5 处：`MirrorDownloader`×3、`DownloadViewModel`×1、`AppRepository`×1）解析后必须校验 https。新增调用点时务必加此校验。

---

## 13. 关键文件索引

| 文件 | 行数 | 职责 |
|------|------|------|
| `data/repo/AppRepository.kt` | 1423 | 核心仓库，Supabase + 缓存 |
| `data/model/Models.kt` | ~260 | 全部领域模型 |
| `data/plugin/PluginManager.kt` | ~480 | 插件管理器 |
| `data/plugin/PluginHostContext.kt` | ~160 | 插件权限边界收口 |
| `data/plugin/PluginLoader.kt` | ~280 | ZIP 解析与安装 |
| `data/plugin/JavaPluginLoader.kt` | ~230 | DexClassLoader 加载 |
| `core/download/MirrorDownloader.kt` | ~310 | 通用镜像站下载器 |
| `core/download/PluginGitHubClient.kt` | ~90 | GitHub release 解析 |
| `di/AppModule.kt` | ~90 | Hilt 模块 |
| `ui/navigation/AppNavGraph.kt` | ~155 | NavHost + 插件导航注入 |
| `ui/navigation/Destinations.kt` | ~25 | 路由表 |

---

## 14. 参考资料

- Java 插件开发指南：`docs/JAVA_PLUGIN.md`
- XML 数据源规范：`docs/XML_DATA_SOURCE.md`
- XML 定时卡片文档：`docs/XML_SCHEDULE.md`

遇到不确定的实现细节时，优先查阅上述文档与现有代码，不要凭猜测修改。

---

## 15. 全仓审计流程（强制执行）

当用户要求「全仓审计」「对全仓进行审计」时，必须严格按以下流程执行，不得跳过任何环节。

### 15.1 流程总览

```
第一轮：文件级审计（每个文件对应一个子智能体）
    ↓
第二轮：功能闭环审计（每个功能闭环对应一个子智能体）
    ↓
修复所有发现的问题
    ↓
重新审计（重复第一轮 + 第二轮）
    ↓
仍有问题？—— 是 → 修复 → 重新审计（循环）
                否 → 审计通过，构建 APK 交付
```

**核心原则**：审计必须循环进行，直到两轮审计均无问题为止，不得在仍有未修复问题时提前结束。

### 15.2 第一轮：文件级审计

**目标**：对全仓每个 `.kt` 文件进行逐文件审计，每个文件对应一个子智能体。

**执行方式**：
- 全仓约 68 个 `.kt` 文件，按功能模块分组（功能紧密耦合的小文件可合并到同一子智能体，但每个文件都必须被独立审计覆盖）
- 每个子智能体负责 1-5 个相关文件，逐文件给出结论
- 单次最多并行 5 个子智能体，分批执行
- 子智能体类型：`search`（只研究不修改）

**审计维度**（每个文件都需覆盖）：
1. **正确性**：逻辑是否正确，参数传递，返回值，错误处理
2. **安全性**：密钥泄露、SSRF、注入、权限边界、明文传输
3. **空安全**：空指针、越界、未处理异常
4. **资源管理**：文件句柄、协程作用域、内存泄漏
5. **并发安全**：`@Volatile`、synchronized、竞态条件
6. **代码风格**：命名、注释语言（中文）、import 风格（无通配符）、无表情包
7. **一致性**：与文档、与其他文件、与 checklist 要求一致

**输出格式**：每个文件给出结论（通过/不通过/需关注）+ 文件路径与行号证据 + 修复建议（不修改代码）。

### 15.3 第二轮：功能闭环审计

**目标**：按功能闭环审计，验证端到端流程完整性。每个功能闭环对应一个子智能体。

**功能闭环清单**（约 7-8 个）：
1. **账号与权限闭环**：设备指纹生成 → 登录 → 角色判定 → 站长/管理员操作 → 账号继承
2. **应用上传闭环**：GitHub 仓库上传 / APK 直传 → PAT 管理 → 自动审核 / 人工审核
3. **下载与镜像站闭环**：详情页下载 → 镜像站测速 → 续传 → 安装 → 报毒提示
4. **信息流首页闭环**：bootstrap → feed 拉取 → 五种卡片渲染 → 插件卡片合并
5. **应用详情页闭环**：详情加载 → 简介/教程 Tab → 多 APK 选择 → 插件插槽
6. **插件系统闭环**：安装（本地/GitHub）→ 加载 → 权限校验 → 视图扩展 → 导航 → 卸载 → 自动更新
7. **内容发布闭环**：文章/推荐/主题发布 → 权限校验 → 列表展示
8. **导航路由闭环**：所有 Routes 注册 → 参数传递 → 插件导航白名单

**审计维度**：
1. 端到端流程是否闭合（每一步衔接、参数传递、错误处理）
2. 各文件之间的数据流是否正确
3. ViewModel ↔ Repository ↔ UI 的 StateFlow 收集是否正确
4. 错误分支是否覆盖完整
5. 与文档/checklist 的一致性

**输出格式**：每个闭环给出结论 + 链路验证表（步骤→文件:行号→衔接验证）+ 问题清单。

### 15.4 修复阶段

**执行方式**：汇总两轮审计发现的所有问题，按优先级修复：
1. **高优先级**：安全漏洞、功能 bug、崩溃风险
2. **中优先级**：资源泄漏、并发问题、一致性
3. **低优先级**：代码风格、文档措辞、注释

修复后必须重新编译验证：`gradle :app:compileDebugKotlin -x lint --console=plain --no-daemon`。

### 15.5 重新审计（循环）

修复完成后，**必须重新执行第一轮 + 第二轮审计**，验证：
- 修复是否正确（未引入新问题）
- 原有问题是否已解决
- 是否有遗漏问题

**循环终止条件**：两轮审计均无「不通过」项。「需关注」项需评估是否必须修复（安全相关的需关注必须修复）。

**循环次数**：无上限，直到通过为止。每次循环需记录：
- 第 N 轮审计发现的问题
- 修复内容
- 第 N+1 轮审计验证结果

### 15.6 最终交付

审计全部通过后：
1. 构建 debug APK：`gradle :app:assembleDebug -x lint --console=plain --no-daemon`
2. 确认 `app/build/outputs/apk/debug/app-debug.apk` 存在
3. 汇总审计报告（各轮发现的问题、修复内容、最终结论）

### 15.7 子智能体使用约定

- **文件级审计**：子智能体类型 `search`，只研究不修改
- **功能闭环审计**：子智能体类型 `search`，只研究不修改
- **修复**：由主智能体直接修改（不委托子智能体），便于跟踪
- **并行限制**：单次最多 5 个子智能体并行
- **任务描述**：必须包含完整文件路径、审计维度、输出格式要求

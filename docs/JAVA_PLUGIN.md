# Java 插件开发指南

## 1. 定位

Java 插件用于为应用补充**便捷的小功能**，例如：

- 收藏夹（本地保存喜欢的应用 / 文章）
- 本地笔记
- 快捷入口聚合
- 离线便签

它运行在宿主进程内，通过受限于权限边界的 SDK 接口访问能力。Java 插件**不能**：

- 直接访问宿主数据库、用户凭据、BuildConfig
- 发起网络请求（`NETWORK` 权限为预留字段，当前申请也不会授予）
- 弹通知（`NOTIFICATION` 权限为预留字段，当前申请也不会授予）
- 修改宿主状态或其它插件的数据

如需添加数据（应用列表 / 文章 / 主题 / 镜像站）而非代码逻辑，请使用 XML 数据源插件，参见 [XML_SCHEDULE.md](./XML_SCHEDULE.md)。

## 2. 包结构

Java 插件以 ZIP 包形式分发（后缀可自定义，如 `.plugin`），根目录必须包含 `plugin.json` 与一个 JAR 文件：

```
my-favorites.plugin (ZIP)
├── plugin.json        # 清单
├── plugin.jar         # 编译后的 JAR，含入口类
└── (可选资源文件)
```

约束：

- 包内文件数上限 64 个
- 单文件大小上限 2 MB
- 解压总大小上限 16 MB
- 文件名仅允许字母、数字、点、下划线（不允许 `/`、`..` 等路径穿越字符）

## 3. plugin.json 清单

```json
{
  "name": "我的收藏夹",
  "packageName": "com.example.favorites",
  "version": "1.0.0",
  "minAppVersion": "1.0.0",
  "type": "JAVA",
  "entry": "plugin.jar",
  "entryClass": "com.example.favorites.FavoritesPlugin",
  "permissions": ["LOCAL_STORAGE", "REGISTER_ENTRY"],
  "description": "本地保存喜欢的应用",
  "author": "your-name",
  "repository": "https://github.com/your-name/my-favorites-plugin",
  "autoUpdate": true
}
```

字段说明：

| 字段 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 用户可见名称 |
| `packageName` | 是 | 唯一标识，反向域名形式，仅允许字母、数字、点、下划线 |
| `version` | 是 | 语义化版本 |
| `minAppVersion` | 是 | 所需最低应用版本 |
| `type` | 是 | 固定 `JAVA` |
| `entry` | 是 | JAR 文件名（如 `plugin.jar`） |
| `entryClass` | 是 | JAR 内入口类全限定名，需实现 `JavaPlugin` 接口 |
| `permissions` | 否 | 申请的权限列表，默认空 |
| `description` | 否 | 插件描述 |
| `author` | 否 | 作者 |
| `repository` | 否 | GitHub 仓库地址（如 `https://github.com/owner/repo`），用于 GitHub 分发与自动更新 |
| `autoUpdate` | 否 | 是否启用自动更新检查（默认 false，需插件显式声明）；仅当 `repository` 非空时有效 |

## 4. SDK 接口

SDK 包含两个核心接口与若干数据类（`com.appmarket.blog.plugin.sdk` 包）：

### 4.1 JavaPlugin

插件入口接口，宿主通过 `DexClassLoader` 反射实例化后调用：

```kotlin
interface JavaPlugin {
    fun onCreate(context: PluginContext)
    fun onDestroy()
}
```

- 入口类必须有无参构造函数
- `onCreate` / `onDestroy` 在 IO 线程调用，**禁止在其中直接操作 UI**
- 如需更新 UI，通过 `registerEntry` 注册入口（宿主保证 `onClick` 在主线程），或通过 `registerViewExtension` 在容器插槽内渲染 Composable（宿主保证 content 在主线程执行）

### 4.2 PluginContext

受限上下文，暴露五组能力，每组对应一个权限：

```kotlin
interface PluginContext {
    val packageName: String

    // 1. 本地存储：仅访问 prefs_<packageName>，命名空间隔离（需 LOCAL_STORAGE）
    fun getString(key: String, default: String = ""): String
    fun putString(key: String, value: String)
    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String>
    fun putStringSet(key: String, value: Set<String>)

    // 2. 注册设置页入口（需 REGISTER_ENTRY）
    fun registerEntry(label: String, onClick: () -> Unit)

    // 3. 读取应用市场公开应用列表只读快照（需 READ_PUBLIC_APPS）
    fun getPublicApps(): List<PublicApp>

    // 4. 打开宿主指定页面（需 OPEN_PAGE）
    fun openPage(request: PluginPageRequest)

    // 5. 在宿主指定容器插槽注册自定义 UI 组件（需 REGISTER_VIEW_EXTENSION）
    fun registerViewExtension(slot: String, content: @Composable (ViewExtensionContext) -> Unit)
}
```

`PublicApp` 仅含 `id / name / packageName / categoryLabel` 四个只读字段。

## 5. 权限边界

权限在 `plugin.json` 的 `permissions` 中声明，运行时由宿主在每个 API 调用前校验。未声明权限的 API 调用会抛 `SecurityException`，并触发插件加载回滚。

| 权限 | 授予能力 | 当前状态 |
|------|----------|----------|
| `LOCAL_STORAGE` | 读写 `prefs_<packageName>` | 已实现 |
| `REGISTER_ENTRY` | 在插件管理页注册入口按钮 | 已实现 |
| `READ_PUBLIC_APPS` | 读取公开应用只读快照 | 已实现 |
| `OPEN_PAGE` | 通过宿主导航打开白名单页面（应用详情/文章/主题/搜索等） | 已实现 |
| `REGISTER_VIEW_EXTENSION` | 在宿主指定容器插槽注册自定义 UI 组件 | 已实现 |
| `NETWORK` | 发起网络请求 | 预留，当前不授予 |
| `NOTIFICATION` | 显示通知 | 预留，当前不授予 |

权限边界设计要点：

- **最小权限原则**：仅声明插件实际需要的能力，用户安装前可看到权限清单
- **命名空间隔离**：`prefs_<packageName>` 仅当前插件可访问，无法跨插件或读取宿主 prefs
- **只读快照**：`getPublicApps()` 返回不可变副本，插件无法修改宿主状态
- **失效保护**：插件卸载后宿主上下文置 `disposed`，残留回调（如 `onClick`）调用任何 API 会抛 `IllegalStateException`

## 6. 完整示例：收藏夹插件

实现一个本地收藏夹：用户在入口点击时切换收藏状态，重启后保留。

### 6.1 入口类（Kotlin）

```kotlin
package com.example.favorites

import com.appmarket.blog.plugin.sdk.JavaPlugin
import com.appmarket.blog.plugin.sdk.PluginContext

class FavoritesPlugin : JavaPlugin {

    private lateinit var ctx: PluginContext

    override fun onCreate(context: PluginContext) {
        ctx = context
        // 注册入口：宿主保证 onClick 在主线程执行
        context.registerEntry("我的收藏") {
            toggleFavorite()
        }
    }

    private fun toggleFavorite() {
        val key = "favorites"
        val current = ctx.getStringSet(key).toMutableSet()
        // 演示：切换一个固定 id 的收藏状态
        val demoId = "app:demo"
        if (demoId in current) current.remove(demoId) else current.add(demoId)
        ctx.putStringSet(key, current)
    }

    override fun onDestroy() {
        // 无需手动清理 prefs / 入口，宿主会清理
    }
}
```

### 6.2 plugin.json

```json
{
  "name": "我的收藏夹",
  "packageName": "com.example.favorites",
  "version": "1.0.0",
  "minAppVersion": "1.0.0",
  "type": "JAVA",
  "entry": "plugin.jar",
  "entryClass": "com.example.favorites.FavoritesPlugin",
  "permissions": ["LOCAL_STORAGE", "REGISTER_ENTRY"],
  "description": "本地收藏夹，离线可用"
}
```

### 6.3 打包

1. 编译入口类为 JAR（仅含插件代码，不要把 SDK 接口打包进去——SDK 由宿主提供）
2. 将 `plugin.json` 与 `plugin.jar` 放入同一目录
3. 用 ZIP 工具打包为 `favorites.plugin`

打包命令示例：

```bash
zip favorites.plugin plugin.json plugin.jar
```

## 7. 打开页面（openPage）

申请 `OPEN_PAGE` 权限后，插件可通过 `openPage` 跳转宿主白名单页面。

### 7.1 目的地与参数

```kotlin
enum class PluginPageDestination {
    APP_DETAIL,      // 应用详情页，需传 id = 应用 ID
    ARTICLE,         // 文章页，需传 id = 文章 ID
    THEME_DETAIL,    // 主题详情页，需传 id = 主题 ID
    SEARCH,          // 搜索页，可选传 query（预留字段，当前打开空白搜索页，暂不预填搜索词）
    FEED,            // 信息流页（首页），无参数
    SETTINGS,        // 设置页（我的），无参数
    PLUGINS,         // 插件管理页，无参数
    UPLOAD,          // 上传应用页，无参数
    PUBLISH          // 发布内容页，无参数
}

data class PluginPageRequest(
    val destination: PluginPageDestination,
    val id: String? = null,
    val query: String? = null
)
```

### 7.2 用法

```kotlin
override fun onCreate(context: PluginContext) {
    ctx = context
    context.registerEntry("查看应用详情") {
        // 跳转到指定应用详情页（id 为应用 ID）
        context.openPage(PluginPageRequest(PluginPageDestination.APP_DETAIL, id = "app-123"))
    }
    context.registerEntry("搜索") {
        // 跳转搜索页并预填搜索词
        context.openPage(PluginPageRequest(PluginPageDestination.SEARCH, query = "工具"))
    }
    context.registerEntry("返回首页") {
        // 跳转信息流首页
        context.openPage(PluginPageRequest(PluginPageDestination.FEED))
    }
}
```

### 7.3 安全行为

- 目的地限定白名单枚举，无法打开任意 Activity 或携带任意 Intent
- `APP_DETAIL` / `ARTICLE` / `THEME_DETAIL` 必须传非空 `id`，缺失时宿主忽略并记日志（不抛异常）
- 导航由宿主保证在主线程执行；插件调用后可继续后续逻辑
- 不携带用户凭据或敏感上下文

## 8. 视图扩展（registerViewExtension）

申请 `REGISTER_VIEW_EXTENSION` 权限后，插件可在宿主指定容器插槽内渲染自定义 Composable 组件。**插件只能在指定插槽内添加自己的 UI，不能修改宿主已有组件。**

### 8.1 可用插槽

```kotlin
object ViewExtensionSlot {
    const val APP_DETAIL_HEADER = "app_detail_header"          // 应用详情页 header 下方（全局可见）
    const val APP_DETAIL_INTRO_BOTTOM = "app_detail_intro_bottom" // 应用详情页简介区底部
    const val APP_DETAIL_BOTTOM = "app_detail_bottom"           // 应用详情页底部下载按钮上方
    const val ARTICLE_TOP = "article_top"                       // 文章页正文上方
    const val ARTICLE_BOTTOM = "article_bottom"                 // 文章页正文末尾
    const val THEME_DETAIL_HEADER = "theme_detail_header"      // 主题详情页封面下方
    const val FEED_TOP = "feed_top"                             // 信息流页顶部栏下方
}
```

### 8.2 上下文（ViewExtensionContext）

宿主渲染容器时构造只读上下文传给插件，按插槽填充对应字段：

```kotlin
data class ViewExtensionContext(
    val appId: String? = null,          // 应用详情页：当前应用 ID
    val appPackageName: String? = null, // 应用详情页：当前应用包名
    val appCategory: String? = null,    // 应用详情页：当前应用分类标签
    val articleId: String? = null,       // 文章页：当前文章 ID
    val articleType: String? = null,     // 文章页：当前文章类型（ARTICLE/RECOMMEND/THEME）
    val themeId: String? = null,        // 主题详情页：当前主题 ID
    val slot: String                    // 当前渲染的插槽标识
)
```

未涉及的插槽对应字段为 null。所有字段只读、不可变，不含敏感信息。

### 8.3 用法

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appmarket.blog.plugin.sdk.ViewExtensionContext
import com.appmarket.blog.plugin.sdk.ViewExtensionSlot

class MyPlugin : JavaPlugin {
    private lateinit var ctx: PluginContext

    override fun onCreate(context: PluginContext) {
        ctx = context
        // 在应用详情页 header 下方渲染一个跳转按钮
        context.registerViewExtension(ViewExtensionSlot.APP_DETAIL_HEADER) { c ->
            // c 为 ViewExtensionContext，含当前 appId / appPackageName / appCategory
            Surface(
                onClick = {
                    // 点击跳转到搜索页
                    context.openPage(PluginPageRequest(PluginPageDestination.SEARCH, query = c.appCategory))
                },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    "查看同类应用（${c.appCategory ?: "全部"}）",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        // 在文章末尾渲染一个相关推荐块
        context.registerViewExtension(ViewExtensionSlot.ARTICLE_BOTTOM) { c ->
            Text("插件为本文附加的内容（文章 ${c.articleId}）", modifier = Modifier.padding(8.dp))
        }
    }

    override fun onDestroy() {}
}
```

### 8.4 安全与限制

- 单个插件单个插槽注册数上限 4，防止异常插件注册海量组件导致 UI 卡顿
- 插件 content 调用由宿主 `runCatching` 包裹，单个插件异常不会崩溃整页
- 插件卸载时宿主自动清理其所有注册
- content 在宿主主线程、宿主 Compose 树中执行，可直接使用 Compose API（Material3 / 布局 / 文本等）

## 9. GitHub 分发

插件支持通过 GitHub 仓库分发，用户粘贴仓库地址即可安装，并支持自动更新检查。**下载与更新查询均走镜像站下载逻辑**（与 APK 下载相同的镜像站列表 + 优先站 + 测速 + 轮询机制）。

### 9.1 仓库要求

1. 仓库根目录无需特殊文件，但必须有 **release 资产**（asset），资产后缀为 `.plugin`
2. release 的 tag 用作版本号比较（去掉前导 `v`，如 `v1.2.0` → `1.2.0`），与 `plugin.json` 的 `version` 字段语义化比较
3. `plugin.json` 中声明 `repository`（仓库地址）与 `autoUpdate`（是否启用自动更新）

### 9.2 plugin.json 声明

```json
{
  "name": "我的收藏夹",
  "packageName": "com.example.favorites",
  "version": "1.0.0",
  "minAppVersion": "1.0.0",
  "type": "JAVA",
  "entry": "plugin.jar",
  "entryClass": "com.example.favorites.FavoritesPlugin",
  "permissions": ["LOCAL_STORAGE", "REGISTER_ENTRY"],
  "repository": "https://github.com/your-name/my-favorites-plugin",
  "autoUpdate": true
}
```

- `repository`：必须以 `https://github.com/` 开头，格式为 `https://github.com/owner/repo`（可带 `.git` 后缀或尾部 `/`）
- `autoUpdate`：默认 false。设为 true 后，用户在插件管理页点击「检查更新」会自动查询 latest release 并安装新版本

### 9.3 通过仓库安装

在「插件管理」页：

1. 在「GitHub 仓库地址」输入框粘贴仓库地址（如 `https://github.com/your-name/my-favorites-plugin`）
2. 点击「从仓库安装」
3. 宿主流程：
   - 校验仓库地址格式，提取 owner/repo
   - 通过镜像站下载 `https://api.github.com/repos/owner/repo/releases/latest` 的 JSON
   - 解析 JSON 定位 `.plugin` 资产及其 GitHub 下载地址
   - 通过镜像站下载该资产到临时文件
   - 校验并安装（复用本地文件安装逻辑，含旧版本卸载）
4. 安装成功后显示插件名称与版本

### 9.4 自动更新

在「插件管理」页点击「检查更新」：

1. 宿主遍历所有已安装插件，对 `repository` 非空且 `autoUpdate=true` 的：
   - 通过镜像站下载 latest release JSON
   - 比较 tag（去掉前导 `v`）与已安装 version
   - 有新版则通过镜像站下载资产并安装（覆盖旧版本，先卸载旧实例再装新的）
2. 结果显示：更新了哪些插件、失败原因、已是最新版

### 9.5 镜像站下载逻辑

GitHub 分发与更新检查全部复用应用 APK 下载相同的镜像站机制：

- 取镜像站列表（已在应用启动时加载并缓存）
- 优先站直连，失败则测速排序候选站轮询
- 资产 URL 经 `MirrorSite.resolve(githubUrl)` 转为镜像地址
- **双重 https 校验**：输入 URL 与镜像解析后的 URL 均必须 `https://`，防止插件注入 http 镜像模板导致明文传输或 SSRF
- 测速阶段读取 256KB 计算真实吞吐量，按吞吐降序排序候选站
- 支持续传（`.part` 文件）、停滞检测、速度感知切源
- 单文件大小上限 32MB（plugin 包），JSON 响应上限 2MB
- 下载失败/取消后清理 `.part` 续传残留，避免缓存泄漏

## 10. 卸载

在「插件管理」页点击插件项的删除按钮：

- 调用 `onDestroy`（异常容错，仍继续清理）
- 清空 `prefs_<packageName>` 本地存储
- 清空该插件注册的所有入口
- 清空该插件注册的所有视图扩展（容器插槽内的 Composable）
- 删除提取的 JAR 与 dex 优化目录

## 11. 限制与注意事项

- 插件运行在同进程，理论上能反射访问宿主对象。宿主通过**文档约定 + 受限 API + 用户安装前确认权限清单**避免恶意插件，不暴露 `BuildConfig` 中的凭据
- 入口类必须来自插件 JAR（`classLoader` 校验），禁止加载宿主类
- 入口类必须实现 `JavaPlugin` 接口，否则拒绝加载
- 单个插件注册入口数上限 16，防止异常插件注册海量入口导致 UI OOM
- `onCreate` 中调用未申请权限的 API 会触发加载失败回滚
- 插件 JAR 中不要包含 SDK 接口的实现（`JavaPlugin` / `PluginContext` / `PublicApp`），由宿主 ClassLoader 提供

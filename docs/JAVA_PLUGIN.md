# Java 插件开发指南

Java 插件用于实现轻量级小功能，例如「收藏夹」「本地笔记」「应用使用统计」等。插件以 JAR 形式提供，由应用通过 `DexClassLoader` 加载，与宿主同进程运行。

> **重要：权限边界**
> Java 插件运行在应用同进程内，**理论上**可反射访问宿主任意对象。但应用通过受限的 `PluginContext` API 暴露能力，并约定插件不得通过反射访问 `AppRepository`、`DeviceSession`、`BuildConfig` 中的 Supabase / GitHub 凭据等内部对象。违反此约定的插件不会被审核通过；用户在安装前会看到权限清单。

## 1. 定位

| 适合 Java 插件 | 不适合 Java 插件 |
|---|---|
| 收藏夹（本地存储） | 替换下载器（应用核心流程） |
| 本地笔记 / 标签 | 接入新数据源（请用 XML_DATA_SOURCE） |
| 应用使用统计 | 修改审核流程 |
| 自定义计算（如 APK 哈希校验） | 访问用户凭据 / PAT |
| 简单 UI 扩展（设置页入口） | 网络服务（请走宿主后端） |

> 数据源扩展请使用 `XML_DATA_SOURCE` 类型，无需写代码。Java 插件只用于需要**本地逻辑**的场景。

## 2. plugin.json 字段（Java 类型）

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 用户可见名称 |
| `packageName` | string | 是 | 反向域名唯一标识（如 `com.example.favorites`） |
| `version` | string | 是 | 语义化版本 |
| `minAppVersion` | string | 是 | 所需最低应用版本 |
| `type` | string | 是 | 固定 `JAVA` |
| `entry` | string | 是 | JAR 文件名（如 `plugin.jar`） |
| `entryClass` | string | 是 | JAR 内入口类全限定名（实现 `JavaPlugin` 接口，如 `com.example.favorites.FavoritesPlugin`） |
| `permissions` | string[] | 否 | 申请的权限，默认空。取值见下方「权限清单」 |
| `description` / `author` | string | 否 | 描述与作者 |

### 示例

```json
{
  "name": "收藏夹",
  "packageName": "com.example.favorites",
  "version": "1.0.0",
  "minAppVersion": "0.5.0",
  "type": "JAVA",
  "entry": "plugin.jar",
  "entryClass": "com.example.favorites.FavoritesPlugin",
  "permissions": ["LOCAL_STORAGE", "REGISTER_ENTRY"],
  "description": "本地收藏应用，无网络",
  "author": "example"
}
```

## 3. 权限清单

| 权限 | 说明 | 是否默认授予 |
|---|---|---|
| `LOCAL_STORAGE` | 读写插件私有 SharedPreferences（`prefs_<packageName>`） | 是 |
| `REGISTER_ENTRY` | 在设置页注册一个入口项，点击跳转插件 UI | 是 |
| `READ_PUBLIC_APPS` | 读取应用市场公开的应用列表（只读快照） | 是 |
| `NETWORK` | 发起网络请求（**默认拒绝**，需用户在安装时确认） | 否 |
| `NOTIFICATION` | 显示通知 | 否 |

> 应用当前实现 `LOCAL_STORAGE` / `REGISTER_ENTRY` / `READ_PUBLIC_APPS`。`NETWORK` / `NOTIFICATION` 为预留，未来按需启用；当前申请也不会授予。

## 4. 插件 SDK 接口

插件作者需依赖以下接口（接口由宿主应用提供，JAR 编译时需把接口作为 `compileOnly` 依赖，运行时由宿主注入实现）。

### 4.1 JavaPlugin（入口）

```kotlin
package com.appmarket.blog.plugin.sdk

interface JavaPlugin {
    /** 插件加载时调用一次，传入受限上下文 */
    fun onCreate(context: PluginContext)
    /** 插件卸载时调用一次，用于释放资源 */
    fun onDestroy()
}
```

### 4.2 PluginContext（受限 API）

```kotlin
package com.appmarket.blog.plugin.sdk

interface PluginContext {
    /** 当前插件包名 */
    val packageName: String

    /** 本地存储：仅可访问当前插件私有命名空间 prefs_<packageName> */
    fun getString(key: String, default: String = ""): String
    fun putString(key: String, value: String)
    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String>
    fun putStringSet(key: String, value: Set<String>)

    /** 注册设置页入口：label 为按钮文案，onClick 为点击回调 */
    fun registerEntry(label: String, onClick: () -> Unit)

    /** 读取应用市场公开应用列表（只读快照） */
    fun getPublicApps(): List<PublicApp>
}

/** 应用市场公开应用（只读视图，不含内部字段） */
data class PublicApp(
    val id: String,
    val name: String,
    val packageName: String,
    val categoryLabel: String
)
```

## 5. 加载流程

```
用户在「设置 > 插件管理」选择 .plugin 文件
  → PluginManager.installFromUri(uri)
  → PluginLoader.loadFromStream：
      1. 解析 plugin.json manifest
      2. 校验 minAppVersion
      3. 提取 entry 指定的 JAR 文件到 filesDir/plugins/<packageName>.jar
  → DexClassLoader 加载 JAR
  → 反射实例化 entryClass（要求有无参构造）
  → 校验 entryClass 实现 JavaPlugin 接口
  → 注入 PluginContext 实现，调用 onCreate()
  → 插件可通过 registerEntry 在设置页注册入口
```

卸载时：
```
PluginManager.uninstall(packageName)
  → 调用 JavaPlugin.onDestroy()
  → 删除 JAR 与 .plugin 文件
  → 移除注册的入口
```

## 6. 收藏夹示例

### 6.1 plugin.json

```json
{
  "name": "收藏夹",
  "packageName": "com.example.favorites",
  "version": "1.0.0",
  "minAppVersion": "0.5.0",
  "type": "JAVA",
  "entry": "plugin.jar",
  "entryClass": "com.example.favorites.FavoritesPlugin",
  "permissions": ["LOCAL_STORAGE", "REGISTER_ENTRY", "READ_PUBLIC_APPS"],
  "description": "收藏应用市场内的应用，仅本地存储"
}
```

### 6.2 FavoritesPlugin.kt

```kotlin
package com.example.favorites

import com.appmarket.blog.plugin.sdk.JavaPlugin
import com.appmarket.blog.plugin.sdk.PluginContext

class FavoritesPlugin : JavaPlugin {
    private lateinit var ctx: PluginContext

    override fun onCreate(context: PluginContext) {
        ctx = context
        ctx.registerEntry("我的收藏") {
            // 跳转到收藏 UI（这里仅打印，实际可启动 Activity 或显示对话框）
            val favorites = ctx.getStringSet("favorites").toList()
            android.util.Log.i("Favorites", "收藏 ${favorites.size} 个应用")
        }
    }

    override fun onDestroy() {
        // 释放资源
    }

    /** 业务方法：收藏一个应用 */
    fun favorite(appId: String) {
        val current = ctx.getStringSet("favorites").toMutableSet()
        current.add(appId)
        ctx.putStringSet("favorites", current)
    }
}
```

## 7. 编译打包

插件 JAR 必须包含 `dex` 字节码（Android 不支持普通 JVM 字节码）。建议使用 `d8` 工具转换：

```bash
# 编译 Kotlin/Java 源码（compileOnly 依赖宿主 SDK 接口 JAR）
kotlinc -cp plugin-sdk.jar src/ -d build/classes

# 转 dex
d8 --release --output build/ build/classes/com/example/favorites/FavoritesPlugin.class

# 打包 JAR（注意是 dex 而非 class）
cd build && jar cf plugin.jar classes.dex
```

或使用 Android Gradle Plugin 的 `library` 模块 + `assembleRelease` 生成 AAR，再从中提取 `classes.jar`（其中已是 dex 格式）。

## 8. 安全约束

插件不得：
- 通过反射访问 `AppRepository` / `DeviceSession` / `MainActivity` 等内部对象
- 读取 `BuildConfig` 中的 `SUPABASE_URL` / `SUPABASE_ANON_KEY` / GitHub PAT
- 直接调用 `OkHttp` / `Retrofit` 发起未授权的网络请求（默认无 `NETWORK` 权限）
- 修改应用 `filesDir` 下非自身命名空间的文件

应用侧保障措施：
- `PluginContext` 是受限接口，仅暴露上述方法
- 插件 SharedPreferences 命名空间隔离（`prefs_<packageName>`）
- 安装 Java 插件前，UI 显示 `permissions` 清单，用户须确认
- DexClassLoader 的 parent 用宿主 ClassLoader，限制访问敏感类（实施中）

## 9. 与 XML 数据源插件的区别

| 维度 | XML_DATA_SOURCE | JAVA |
|---|---|---|
| 形式 | XML 文件 | JAR（dex） |
| 功能 | 添加数据（应用/镜像站/文章/主题） | 实现小功能（收藏夹等） |
| 代码 | 无 | 有 |
| 权限 | 无需声明 | 需声明 permissions |
| 风险 | 极低（纯数据） | 中（需用户确认权限） |
| 适用 | 数据扩展 | 本地功能扩展 |

建议优先使用 XML 数据源；只有当需要本地逻辑时才用 Java 插件。

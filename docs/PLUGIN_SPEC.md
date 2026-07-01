# 插件规范

## 1. plugin.json 字段

插件根目录下 `plugin.json` 是必需的清单文件。

### 1.1 通用字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 用户可见的插件名称 |
| `packageName` | string | 是 | 插件唯一标识，反向域名形式（如 `com.example.mysource`） |
| `version` | string | 是 | 语义化版本号（如 `1.0.0`） |
| `minAppVersion` | string | 是 | 所需最低应用版本，按语义化版本分段比较 |
| `type` | string | 是 | 插件类型，取值 `XML_DATA_SOURCE` 或 `JAVA` |
| `entry` | string | 否 | 入口文件名。XML 类型缺省 `apps.xml`；JAVA 类型指向 JAR |
| `description` | string | 否 | 插件描述 |
| `author` | string | 否 | 作者 |

### 1.2 Java 类型额外字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `entryClass` | string | 是（JAVA） | JAR 内入口类全限定名，需实现 `JavaPlugin` 接口 |
| `permissions` | string[] | 否 | 申请的权限，默认空。详见 [JAVA_PLUGIN.md](JAVA_PLUGIN.md) |

## 2. 加载流程

1. 应用读取插件包（ZIP 流），收集根目录所有文件（扁平结构）
2. 解析 `plugin.json` 得到 manifest
3. 校验 `minAppVersion`：当前应用版本低于要求则拒绝加载
4. 根据 `type` 分发：
   - `XML_DATA_SOURCE`：读取 `entry` 指定的 XML 文件并解析为数据源条目
   - `JAVA`：提取 JAR 到 `filesDir/plugins/<packageName>.jar`，用 `DexClassLoader` 加载，反射实例化 `entryClass`，注入 `PluginContext`，调用 `onCreate()`
5. 单个数据条目解析失败不影响其他条目

## 3. 版本兼容性校验

`minAppVersion` 按语义化版本分段数字比较，非数字段视为 0。例如：
- 应用版本 `0.5.1` vs `minAppVersion` `0.5.0` → 兼容
- 应用版本 `0.4.9` vs `minAppVersion` `0.5.0` → 不兼容

## 4. 插件安装位置

已安装插件存放于应用 `filesDir/plugins/`：
- `<packageName>.plugin`：原始 ZIP 包
- `<packageName>.jar`：Java 插件提取的 JAR（仅 JAVA 类型）

卸载即删除以上文件。

## 5. 数据源类型

XML 数据源通过根节点 `type` 属性指定数据源类型，决定解析哪个子节点：

| type | 子节点 | 说明 |
|------|--------|------|
| `APPS` | `<apps>` 内的 `<app>` | 应用列表 |
| `MIRRORS` | `<mirrors>` 内的 `<mirror>` | 镜像站列表 |
| `ARTICLES` | `<articles>` 内的 `<article>` | 文章列表 |
| `THEMES` | `<themes>` 内的 `<theme>` | 主题合集列表 |

详细字段见 [XML_DATA_SOURCE.md](XML_DATA_SOURCE.md)。

## 6. 定时出现（schedule）

XML 数据源条目（app / article / theme）可附加 `<schedule>` 子节点，控制卡片何时在信息流出现，实现「每日推送」效果。详见 [XML_DATA_SOURCE.md](XML_DATA_SOURCE.md#定时出现-schedule)。

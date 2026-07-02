# AppMarket 插件开发文档

本仓库为 AppMarket 应用市场的插件开发文档与示例。插件用于在不修改应用主体的前提下，向应用市场添加数据源（应用、镜像站、文章、主题合集等）或扩展本地小功能。

## 插件类型

| 类型 | manifest.type | 后缀 | 是否已实现 | 适用场景 |
|------|---------------|------|----------|--------|
| XML 数据源 | `XML_DATA_SOURCE` | `.plugin`（ZIP 包） | 已实现 | 添加数据：应用 / 镜像站 / 文章 / 主题 |
| Java 插件 | `JAVA` | `.plugin`（ZIP 包） | 已实现 | 实现小功能：收藏夹 / 本地笔记 / 统计 |

## 插件包格式

- 整体为 **ZIP 压缩包**，文件后缀约定为 `.plugin`（非 `.zip`，但解析时按 ZIP 流解析）
- 根目录（扁平结构，子目录暂不处理）下必须包含：
  - `plugin.json`：插件清单（必需）
  - 数据源入口文件（XML 类型必需，缺省 `apps.xml`）
  - JAR 文件（Java 类型必需，由 manifest.entry 指定）

## 快速开始

### XML 数据源插件

1. 编写 `plugin.json`（参考 [docs/PLUGIN_SPEC.md](docs/PLUGIN_SPEC.md)）
2. 编写 XML 数据源文件（参考 [examples/](examples/)）
3. 将两文件打包为 ZIP，重命名为 `.plugin` 后缀
4. 在应用 设置 > 插件管理 > 安装插件 选择该文件

### Java 插件

1. 编写 `plugin.json`，声明 `entry` / `entryClass` / `permissions`
2. 实现 `JavaPlugin` 接口（参考 [docs/JAVA_PLUGIN.md](docs/JAVA_PLUGIN.md)）
3. 编译为 dex 格式的 JAR
4. 与 plugin.json 一起打包为 ZIP，重命名为 `.plugin`
5. 在应用 设置 > 插件管理 > 安装插件；安装前会显示权限清单

## 文档目录

- [docs/PLUGIN_SPEC.md](docs/PLUGIN_SPEC.md) — 完整插件规范（plugin.json 字段、加载流程）
- [docs/XML_DATA_SOURCE.md](docs/XML_DATA_SOURCE.md) — XML 数据源详细规范（节点结构、字段说明）
- [docs/XML_SCHEDULE.md](docs/XML_SCHEDULE.md) — XML 插件定时卡片（schedule 字段：每日推送 / 日期范围 / 周期性出现）
- [docs/JAVA_PLUGIN.md](docs/JAVA_PLUGIN.md) — Java 插件开发指南（权限边界、openPage、视图扩展、GitHub 分发）
- [docs/AGENT.md](docs/AGENT.md) — 项目架构权威参考（技术栈、目录结构、插件系统、安全边界、全仓审计流程）

## 示例

- [examples/plugin.json](examples/plugin.json) — manifest 示例
- [examples/apps.xml](examples/apps.xml) — 应用列表数据源
- [examples/mirrors.xml](examples/mirrors.xml) — 镜像站数据源
- [examples/articles.xml](examples/articles.xml) — 文章数据源
- [examples/themes.xml](examples/themes.xml) — 主题合集数据源

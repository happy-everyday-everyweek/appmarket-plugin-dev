# AppMarket 插件开发文档

本仓库为 AppMarket 应用市场的插件开发文档与示例。插件用于在不修改应用主体的前提下，向应用市场添加数据源（应用、镜像站、文章、主题合集等）或扩展功能。

## 插件类型

| 类型 | manifest.type | 后缀 | 是否已实现 |
|------|---------------|------|----------|
| XML 数据源 | `XML_DATA_SOURCE` | `.plugin`（ZIP 包） | 已实现 |
| Java 插件 | `JAVA` | `.plugin`（ZIP 包） | 暂不兼容（仅解析 manifest，UI 提示不兼容） |

## 插件包格式

- 整体为 **ZIP 压缩包**，文件后缀约定为 `.plugin`（非 `.zip`，但解析时按 ZIP 流解析）
- 根目录（扁平结构，子目录暂不处理）下必须包含：
  - `plugin.json`：插件清单（必需）
  - 数据源入口文件（XML 类型必需，缺省 `apps.xml`）
  - JAR 文件（Java 类型，暂不加载）

## 快速开始

1. 编写 `plugin.json`（参考 [docs/PLUGIN_SPEC.md](docs/PLUGIN_SPEC.md)）
2. 编写 XML 数据源文件（参考 [examples/](examples/)）
3. 将两文件打包为 ZIP，重命名为 `.plugin` 后缀
4. 在应用 设置 > 插件管理 > 安装插件 选择该文件

## 文档目录

- [docs/PLUGIN_SPEC.md](docs/PLUGIN_SPEC.md) — 完整插件规范（plugin.json 字段、加载流程）
- [docs/XML_DATA_SOURCE.md](docs/XML_DATA_SOURCE.md) — XML 数据源详细规范
- [docs/JAVA_PLUGIN.md](docs/JAVA_PLUGIN.md) — Java 插件兼容性说明

## 示例

- [examples/plugin.json](examples/plugin.json) — manifest 示例
- [examples/apps.xml](examples/apps.xml) — 应用列表数据源
- [examples/mirrors.xml](examples/mirrors.xml) — 镜像站数据源
- [examples/articles.xml](examples/articles.xml) — 文章数据源
- [examples/themes.xml](examples/themes.xml) — 主题合集数据源

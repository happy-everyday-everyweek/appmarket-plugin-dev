# XML 数据源规范

XML 数据源插件通过 XML 文件向应用市场添加数据。根节点 `<dataSource>` 的 `type` 属性决定数据源类型，`pluginName` 属性为可选展示名。

## 通用结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dataSource type="MIRRORS" pluginName="我的镜像源">
  <!-- 子节点由 type 决定 -->
</dataSource>
```

## 1. APPS — 应用列表

`type="APPS"`，子节点 `<apps>` 内含多个 `<app>`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dataSource type="APPS" pluginName="开源应用源">
  <apps>
    <app>
      <name>示例应用</name>
      <packageName>com.example.app</packageName>
      <githubRepo>owner/repo</githubRepo>
      <category>OPENSOURCE</category>
      <description>应用描述</description>
      <tags>
        <tag>工具</tag>
        <tag>开源</tag>
      </tags>
    </app>
  </apps>
</dataSource>
```

### app 字段

| 字段 | 必需 | 说明 |
|------|------|------|
| `name` | 是 | 应用名称 |
| `packageName` | 是 | 应用包名 |
| `githubRepo` | 是 | GitHub 仓库（`owner/repo`） |
| `category` | 是 | 分类，取值 `OPENSOURCE` / `GENUINE` / `NATURAL` |
| `description` | 是 | 应用描述 |
| `tag` | 否 | 标签，可多个 |

## 2. MIRRORS — 镜像站列表

`type="MIRRORS"`，子节点 `<mirrors>` 内含多个 `<mirror>`。用于扩展 GitHub 资源下载的镜像站候选。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dataSource type="MIRRORS" pluginName="公共镜像源">
  <mirrors>
    <mirror>
      <name>ghproxy</name>
      <urlTemplate>https://ghproxy.com{path}</urlTemplate>
      <siteType>replace</siteType>
      <priority>10</priority>
      <isOfficial>false</isOfficial>
    </mirror>
  </mirrors>
</dataSource>
```

### mirror 字段

| 字段 | 必需 | 说明 |
|------|------|------|
| `name` | 是 | 镜像站名称 |
| `urlTemplate` | 是 | URL 模板。`replace` 类型用 `{path}` 占位；`prefix` 类型用 `{github_url}` 占位 |
| `siteType` | 否 | 站点类型，取值 `replace`（默认）或 `prefix` |
| `priority` | 否 | 优先级，数字越大越优先，默认 0 |
| `isOfficial` | 否 | 是否官方镜像，默认 false |

### URL 模板示例

- `replace` 类型：
  - 模板 `https://ghproxy.com{path}`
  - GitHub URL `https://github.com/owner/repo/releases/download/v1/app.apk`
  - 解析后 path = `/owner/repo/releases/download/v1/app.apk`
  - 最终 = `https://ghproxy.com/owner/repo/releases/download/v1/app.apk`
- `prefix` 类型：
  - 模板 `https://mirror.example.com/?url={github_url}`
  - 最终 = `https://mirror.example.com/?url=https://github.com/owner/repo/releases/download/v1/app.apk`

## 3. ARTICLES — 文章列表

`type="ARTICLES"`，子节点 `<articles>` 内含多个 `<article>`。`content` 支持 GitHub 风味 Markdown，可用 CDATA 包裹避免 XML 转义问题。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dataSource type="ARTICLES" pluginName="精选文章">
  <articles>
    <article>
      <type>ARTICLE</type>
      <title>开源应用推荐</title>
      <summary>本期推荐 5 款优秀开源应用</summary>
      <content><![CDATA[
# 开源应用推荐

以下是本期推荐...
      ]]></content>
      <cover>https://example.com/cover.jpg</cover>
      <appId>com.example.app1</appId>
      <tag>推荐</tag>
    </article>
  </articles>
</dataSource>
```

### article 字段

| 字段 | 必需 | 说明 |
|------|------|------|
| `type` | 否 | 文章类型，取值 `ARTICLE`（默认）/ `RECOMMEND` / `THEME` |
| `title` | 是 | 标题 |
| `summary` | 是 | 摘要 |
| `content` | 是 | 正文 Markdown（建议 CDATA 包裹） |
| `cover` | 否 | 封面图 URL |
| `appId` | 否 | 关联应用 ID，可多个 |
| `tag` | 否 | 标签，可多个 |

## 4. THEMES — 主题合集

`type="THEMES"`，子节点 `<themes>` 内含多个 `<theme>`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dataSource type="THEMES" pluginName="主题合集">
  <themes>
    <theme>
      <title>效率工具合集</title>
      <subtitle>提升生产力的必备应用</subtitle>
      <cover>https://example.com/theme.jpg</cover>
      <appId>com.example.app1</appId>
      <appId>com.example.app2</appId>
    </theme>
  </themes>
</dataSource>
```

### theme 字段

| 字段 | 必需 | 说明 |
|------|------|------|
| `title` | 是 | 主题标题 |
| `subtitle` | 否 | 副标题 |
| `cover` | 否 | 封面图 URL |
| `appId` | 否 | 包含的应用 ID，可多个 |

## 解析容错

- 单个条目解析失败不影响其他条目，失败会在日志记录
- 缺失字段使用默认值（如 `category` 缺省 `OPENSOURCE`，`siteType` 缺省 `replace`，`priority` 缺省 0）
- 枚举值大小写不敏感（如 `opensource` 与 `OPENSOURCE` 等价）

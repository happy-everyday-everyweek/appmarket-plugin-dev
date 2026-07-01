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
| `schedule` | 否 | 定时出现计划，详见下方「定时出现」 |

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
| `schedule` | 否 | 定时出现计划，详见下方「定时出现」 |

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
| `schedule` | 否 | 定时出现计划，详见下方「定时出现」 |

## 5. 定时出现（schedule）

`app` / `article` / `theme` 均可附加 `<schedule>` 子节点，控制卡片在信息流中何时出现，实现「每日推送」「工作日特推」「限时活动」等效果。

### 5.1 schedule 字段

| 字段 | 必需 | 说明 |
|------|------|------|
| `startDate` | 否 | 起始日期 `YYYY-MM-DD`（含）。未设置则无起始限制 |
| `endDate` | 否 | 结束日期 `YYYY-MM-DD`（含）。未设置则无结束限制 |
| `dayOfWeek` | 否 | 仅在每周指定星期出现，逗号分隔 `1`-`7`，1=周一、7=周日。未设置则不限星期 |
| `featuredDate` | 否 | 「每日推送」指定日期 `YYYY-MM-DD`（含），仅在该天出现。设此项后 startDate / endDate / dayOfWeek 不再生效 |

### 5.2 可见性规则

应用在每次刷新信息流时，对每个带 `schedule` 的条目计算可见性：

1. 若设置 `featuredDate`：仅当 `当前日期 == featuredDate` 时可见，其他日期隐藏
2. 否则：
   - 若设置 `startDate` 且 `当前日期 < startDate` → 隐藏
   - 若设置 `endDate` 且 `当前日期 > endDate` → 隐藏
   - 若设置 `dayOfWeek` 且 `当前星期不在列表` → 隐藏
   - 其余情况可见

未设置 `<schedule>` 的条目始终可见。

> 日期按设备本地时区计算，使用 `LocalDate.now()`。星期映射：周一=1，周二=2，...，周日=7（ISO-8601）。

### 5.3 示例：每日推送

```xml
<dataSource type="ARTICLES" pluginName="每日推送">
  <articles>
    <article>
      <type>RECOMMEND</type>
      <title>2026-07-15 今日推荐</title>
      <summary>本期推荐 3 款应用</summary>
      <content><![CDATA[# 今日推荐...]]></content>
      <appId>com.example.app1</appId>
      <schedule>
        <featuredDate>2026-07-15</featuredDate>
      </schedule>
    </article>
  </articles>
</dataSource>
```

该文章仅在 2026-07-15 当天出现在信息流中。

### 5.4 示例：工作日特推

```xml
<dataSource type="APPS" pluginName="工作日开源应用">
  <apps>
    <app>
      <name>生产力工具</name>
      <packageName>com.example.tool</packageName>
      <githubRepo>example/tool</githubRepo>
      <category>OPENSOURCE</category>
      <description>工作日每天推荐</description>
      <schedule>
        <dayOfWeek>1,2,3,4,5</dayOfWeek>
      </schedule>
    </app>
  </apps>
</dataSource>
```

该应用仅在周一至周五出现。

### 5.5 示例：限时活动

```xml
<theme>
  <title>春节精选</title>
  <cover>https://example.com/spring.jpg</cover>
  <appId>com.example.app1</appId>
  <schedule>
    <startDate>2026-02-08</startDate>
    <endDate>2026-02-22</endDate>
  </schedule>
</theme>
```

该主题仅在 2026-02-08 至 2026-02-22 期间出现。

## 6. 运行时消费

安装插件后，各数据源类型在应用内的运行时行为如下：

| 类型 | 是否被运行时消费 | 说明 |
|------|------------------|------|
| `MIRRORS` | 是 | 已安装插件提供的镜像站会合并进应用运行时镜像站列表，与后端镜像站一起参与下载测速与优选。镜像站 ID 形如 `插件包名:镜像站名`，与后端 ID 不冲突。卸载插件后其镜像站随之移除。 |
| `APPS` / `ARTICLES` / `THEMES` | 否（仅展示计数） | 当前为基础设施阶段，这三类数据源解析后仅在插件管理页展示条目数量，尚未注入信息流 / 详情等业务 UI。后续版本将接入。 |

> schedule 字段已实现解析，待 APPS / ARTICLES / THEMES 数据接入信息流后自动生效。

## 7. 解析容错

- 单个条目解析失败不影响其他条目，失败会在日志记录，解析器会跳到当前节点结束标签恢复状态继续解析后续节点
- 缺失字段使用默认值（如 `category` 缺省 `OPENSOURCE`，`siteType` 缺省 `replace`，`priority` 缺省 0）
- 枚举值大小写不敏感（如 `opensource` 与 `OPENSOURCE` 等价）
- `dayOfWeek` 解析时忽略空格，非法数字忽略（如 `1, ,9` 中的 `9` 被忽略）

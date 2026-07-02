# XML 插件定时卡片

XML 数据源插件可在条目（应用 / 文章 / 主题）内通过 `<schedule>` 子节点控制**何时出现**，实现类似每日推送、限时活动、周期性展示的效果。

## 0. XML 根节点与字段约定

XML 数据源根节点为 `<dataSource>`，可带两个属性：

| 属性 | 取值 | 说明 |
|------|------|------|
| `type` | `APPS` / `MIRRORS` / `ARTICLES` / `THEMES` | 数据源类型，大小写不敏感；缺失或非法值回退 `APPS` |
| `pluginName` | 任意字符串 | 可选，插件展示名，用作卡片作者名与开发者名兜底 |

> 注意：解析器会同时扫描 `<app>` / `<mirror>` / `<article>` / `<theme>` 节点，`type` 仅作分类标识，不限制节点解析。一个 XML 文件可同时声明多类条目。

条目内的枚举字段约定（均大小写不敏感，非法值会降级）：

| 字段 | 合法取值 | 非法值降级 |
|------|---------|-----------|
| `<app>/<category>` | `OPENSOURCE` / `GENUINE` / `NATURAL` | `OPENSOURCE`（如中文「推荐」会被忽略并降级为 `OPENSOURCE`） |
| `<article>/<type>` | `ARTICLE` / `RECOMMEND` / `THEME` | `ARTICLE`（如 `weekly` 会被忽略并降级为 `ARTICLE`） |

应用包名通过 `<packageName>` 子标签声明（不是 `<pkg>`）。

## 1. schedule 字段

`<schedule>` 节点可放在 `<app>`、`<article>`、`<theme>` 三类条目下，含以下可选子节点：

| 字段 | 格式 | 说明 |
|------|------|------|
| `startDate` | `YYYY-MM-DD` | 起始日期（含），未设置则无起始限制 |
| `endDate` | `YYYY-MM-DD` | 结束日期（含），未设置则无结束限制 |
| `dayOfWeek` | 逗号分隔数字 | 仅在每周指定星期出现，1=周一、7=周日（ISO-8601），空表示不限 |
| `featuredDate` | `YYYY-MM-DD` | 每日推送指定日期（含），仅在该天出现；设此项后其他字段失效 |

日期格式必须为 ISO `YYYY-MM-DD`，非法格式会被忽略并记日志（条目视为未设置该字段）。

## 2. 可见性判定规则

`isVisible(today, dayOfWeekValue)` 按以下优先级判断：

1. 若设置 `featuredDate`：仅当 `today == featuredDate` 时可见（每日推送模式，其他字段失效）
2. 否则：满足 `startDate ≤ today ≤ endDate` 范围 **且** `today` 的星期在 `dayOfWeek` 列表中
   - `startDate` / `endDate` 未设置则不限制该侧
   - `dayOfWeek` 为空则不限制星期

`today` 为 `LocalDate.now().toString()` 标准化输出，避免字典序比较出错。

## 3. 三种典型用法

### 3.1 每日推送（仅某天出现）

类似「今日推荐」，仅 2026-07-15 当天出现：

```xml
<article>
  <type>RECOMMEND</type>
  <title>今日推荐：夏日必备应用</title>
  <summary>仅今日限时推荐</summary>
  <schedule>
    <featuredDate>2026-07-15</featuredDate>
  </schedule>
</article>
```

### 3.2 日期范围

活动期内出现，活动结束后自动消失：

```xml
<app>
  <name>夏日活动应用</name>
  <packageName>com.example.summer</packageName>
  <schedule>
    <startDate>2026-07-01</startDate>
    <endDate>2026-08-31</endDate>
  </schedule>
</app>
```

### 3.3 周期性出现

每周一、三、五出现：

```xml
<theme>
  <title>周末特辑</title>
  <schedule>
    <dayOfWeek>1,3,5</dayOfWeek>
  </schedule>
</theme>
```

### 3.4 组合：范围内且限定星期

7 月内每周一、三、五出现：

```xml
<article>
  <type>ARTICLE</type>
  <title>7 月精选</title>
  <schedule>
    <startDate>2026-07-01</startDate>
    <endDate>2026-07-31</endDate>
    <dayOfWeek>1,3,5</dayOfWeek>
  </schedule>
</article>
```

> 说明：`<type>` 只接受合法 ArticleType 枚举（`ARTICLE` / `RECOMMEND` / `THEME`）。早期文档示例中的 `<type>weekly</type>` 为非法值，会被解析器降级为 `ARTICLE`。

## 4. 完整 XML 示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dataSource type="APPS" pluginName="定时卡片示例">
  <apps>
    <app>
      <name>每日推荐应用</name>
      <packageName>com.example.daily</packageName>
      <category>OPENSOURCE</category>
      <schedule>
        <featuredDate>2026-07-01</featuredDate>
      </schedule>
    </app>
  </apps>

  <articles>
    <article>
      <type>ARTICLE</type>
      <title>本周精选</title>
      <summary>每周一三五更新</summary>
      <schedule>
        <dayOfWeek>1,3,5</dayOfWeek>
      </schedule>
    </article>
  </articles>

  <themes>
    <theme>
      <title>暑期主题</title>
      <schedule>
        <startDate>2026-07-01</startDate>
        <endDate>2026-08-31</endDate>
      </schedule>
    </theme>
  </themes>
</dataSource>
```

> 说明：根节点必须是 `<dataSource>`（不是 `<data>`）。`type` 取值为 `APPS` / `MIRRORS` / `ARTICLES` / `THEMES` 枚举，大小写不敏感。`<category>` 取值为 `OPENSOURCE` / `GENUINE` / `NATURAL` 枚举，大小写不敏感；中文值（如「推荐」）非合法枚举，会被降级为 `OPENSOURCE`。

## 5. 与信息流的集成

满足 `schedule` 可见性的条目会被收集为插件卡片，合并进应用信息流：

- 合并时按 `id` 去重，后端卡片优先保留
- 合并后按 `timestamp` 降序排序
- 重启应用后，定时卡片会重新按当天日期判定可见性，自动出现 / 消失
- `<app>` 节点声明的定时应用会被收集为 `SingleApp` 卡片；`XmlAppEntry` 字段不完整（无图标 / 版本 / 下载量 / APK 列表），用合理默认值兜底，`id` 形如 `<packageName>:app:<idx>` 避免与后端数字 app id 冲突

## 6. 注意事项

- 未设置任何 `schedule` 子节点的条目始终可见（向后兼容）
- `dayOfWeek` 中 1-7 之外的非法数字会被忽略，不影响其它合法数字
- 跨午夜时宿主复用单次 `LocalDate.now()` 实例，避免同一批卡片判定基准不一致
- `featuredDate` 适合运营推广场景，设置后建议同时提供活动开始前的预热文案（通过另一条日期范围卡片）
- 根节点必须为 `<dataSource>`，应用包名标签为 `<packageName>`，`category` 与 `type` 必须为合法枚举名，否则会被降级处理

# Java 插件兼容性说明

## 当前状态

Java 插件（`type: "JAVA"`）在当前应用版本中 **暂不兼容**。

应用加载 Java 插件时：
1. 仅解析 `plugin.json` manifest，校验 `minAppVersion`
2. **不加载 JAR 文件**，不执行任何 Java 代码
3. 在插件管理界面显示"该插件类型暂不兼容"提示
4. 不会将插件安装到本地（避免无效文件占用空间）

## 为什么保留 Java 类型

为未来扩展预留：当应用支持 Java 插件时，可通过 `DexClassLoader` / `PathClassLoader` 加载 `entry` 指定的 JAR 文件，实现更复杂的功能扩展（如自定义下载器、自定义审核规则等）。

## manifest 字段（Java 类型）

```json
{
  "name": "我的 Java 插件",
  "packageName": "com.example.javaplugin",
  "version": "1.0.0",
  "minAppVersion": "0.5.0",
  "type": "JAVA",
  "entry": "plugin.jar",
  "description": "未来支持的 Java 插件",
  "author": "example"
}
```

## 给开发者的建议

- 现阶段请优先使用 `XML_DATA_SOURCE` 类型实现数据源扩展
- 如需功能扩展，可先准备好 `plugin.json` 与 JAR，等待应用支持 Java 插件加载后再发布
- 不要在生产环境分发 Java 插件，用户安装后只会看到"暂不兼容"提示

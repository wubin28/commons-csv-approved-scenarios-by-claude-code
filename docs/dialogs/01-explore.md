# 01 源码勘探：requiredHeaders 的落点

> 基线：`master@66a83820`（1.14.2-SNAPSHOT）· 勘探日期 2026-09-22 · 执行者 Claude Code

## 勘探提示词

```text
我要在 commons-csv 上新增 Strict Header Schema Validation Mode 的第一条规则 requiredHeaders：
调用方声明"CSV 必须包含哪些列"，缺列时在解析初始化阶段报错。
请只读不改，回答：
1. header map 在哪里构建？构建完成的那一刻在哪一行？
2. CSVFormat 新增一个配置项，需要同步改哪几处？
3. 现有的"构建期校验"和"解析期校验"各在哪里？报错用什么异常、什么措辞？
4. 有哪些现有测试会因为 CSVFormat 新增字段而受影响？
每条结论附 文件:行号。
```

## 结论

| # | 问题 | 结论 |
| --- | --- | --- |
| 1 | header map 构建点 | `CSVParser.createHeaders()`（`CSVParser.java:601-656`）。被私有构造器 `CSVParser.java:564` 调用：`this.headers = createHeaders();`。这一行之后 header map 已完整，是"解析初始化阶段"校验的天然落点 |
| 1a | 表头来源 | `format.getHeader()` 为空数组 → 读文件首行（`:608-614`）；非空 → 用显式表头（`:615-623`）；`null` → 不建 header map |
| 1b | 大小写 | `createEmptyHeaderMap()`（`:588-592`）：`ignoreHeaderCase=true` 时用 `TreeMap(CASE_INSENSITIVE_ORDER)`，否则 `LinkedHashMap`。**直接用 `headerMap.containsKey()` 判断，大小写语义自动与既有行为一致** |
| 2 | 新增配置项需改 | `Builder` 字段（`CSVFormat.java:232-275`）、`Builder(CSVFormat)` 拷贝构造（`:281-303`）、setter、`CSVFormat` final 字段（`:1551-1614`）、`CSVFormat(Builder)` 构造（`:1616-1640`）、getter、`equals`（`:1677`）、`hashCode`（`:2027`）、`toString`（`:2537`） |
| 3 | 构建期校验 | `CSVFormat.validate()`（`:2606-2640`），在构造器末尾调用；全部抛 `IllegalArgumentException` |
| 3a | 解析期校验 | `createHeaders()` 里空表头、重复表头都抛 `IllegalArgumentException`，消息形如 `The header contains a duplicate name: "a" in [a, a]...` |
| 4 | 受影响测试 | `CSVFormatTest#testEqualsHash`（`CSVFormatTest.java:253`）反射遍历 `with*` 方法；新 API 只加 `Builder#setRequiredHeaders`，不加 `with*`，不受影响 |

## 已知坑

- `mvn test` 在 validate 阶段就跑 `apache-rat:check`（来自 `commons-parent:97`），新增任何无许可证头的文件（包括本文件）都会让构建失败。见 `pom.xml` 的 `apache-rat-plugin` `<inputExcludes>`。

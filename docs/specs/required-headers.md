# Spec: Strict Header Schema Validation Mode — `requiredHeaders` 规则

> 状态：已评审 · 切片 v1 · 2026-09-22
> 落点：`CSVFormat.Builder` / `CSVParser` 构造器
> 基线：`master@66a83820`（1.14.2-SNAPSHOT）

---

## 1. 问题陈述

团队明确知道上游对账文件应包含哪些列时，应在**解析初始化阶段**就显式校验并给出可读错误，
而不是等业务代码调用 `record.get("currency")` 时才抛出晦涩的 `IllegalArgumentException: Mapping for currency not found`。

## 2. 术语

| 术语 | 定义 |
| --- | --- |
| **实际表头** | `CSVParser` 建好的 header map 中的列名（来源可以是文件首行，也可以是 `setHeader("a","b")` 显式给出） |
| **必需列** | 调用方通过 `setRequiredHeaders(...)` 声明的列名 |
| **缺失列** | 声明为必需、但不在实际表头中的列名 |

## 3. API

```java
CSVFormat format = CSVFormat.DEFAULT.builder()
        .setHeader()                 // 从首行读表头
        .setSkipHeaderRecord(true)
        .setRequiredHeaders("order_id", "amount", "currency")
        .get();

String[] required = format.getRequiredHeaders(); // 返回副本；未设置时为空数组
```

- `Builder#setRequiredHeaders(String...)`：保存副本；传 `null` 或空数组 = 功能关闭。
- `CSVFormat#getRequiredHeaders()`：返回副本，永不返回 `null`。
- 参与 `equals` / `hashCode` / `toString`（`toString` 仅在非空时输出 `RequiredHeaders:[...]`）。

## 4. 规则

| 编号 | 规则 | 裁决 |
| --- | --- | --- |
| **R1**（D2） | 校验时机 | `CSVParser` 构造、header map 建好之后立即校验，早于读第一条记录 |
| **R2**（D4） | 匹配 | 默认大小写精确匹配、不 trim；`setIgnoreHeaderCase(true)` 时不区分大小写（复用 header map 语义） |
| **R3**（D5） | 失败方式 | 抛 `IllegalArgumentException`，消息：`Missing required headers [<缺失列，按声明顺序>]; actual header: [<实际表头，按出现顺序>]` |
| **R4**（D6） | 额外列 / 列顺序 | 额外列允许；列顺序不校验 |
| **R5**（D8） | 表头来源 | 显式 `setHeader("a","b")` 给出的表头同样校验 |
| **R6**（D9） | 重复列 | 仍由 `DuplicateHeaderMode` 处理；requiredHeaders 只看"是否出现" |
| **R7**（D11） | 空输入 | 无表头行 → 全部必需列算缺失，`actual header: []` |
| **R8**（D10） | 关闭时 | 未设置 requiredHeaders，行为与基线完全一致 |

## 5. 构建期契约（`CSVFormat.validate()`）

| 编号 | 条件 | 结果 |
| --- | --- | --- |
| **C1**（D3） | 声明了 requiredHeaders 但 `getHeader() == null`（未开启表头） | `IllegalArgumentException: Required headers [...] need a header: call setHeader() ...` |
| **C2**（D7） | requiredHeaders 含 `null` 或空白串 | `IllegalArgumentException` |
| **C3**（D7） | requiredHeaders 含重名（精确比较） | `IllegalArgumentException` |

## 6. 明确不做（非目标）

- `allowUnknownHeaders`、`enforceHeaderOrder`（后续切片）
- header alias / canonical name、列类型校验、行级业务规则
- printer 侧 schema 输出、多套 schema profile
- 新增异常类型（沿用 `IllegalArgumentException`）

## 7. 验收标准

### 7.1 业务事实 → Approved Scenarios 夹具（`src/test/resources/org/apache/commons/csv/approved/`）

| # | 场景 | 期望 |
| --- | --- | --- |
| 01 | 必需列全部存在 | 通过，记录照常读出 |
| 02 | 缺失一列 | 拒绝，`Missing required headers [currency]` |
| 03 | 缺失多列 | 拒绝，缺失列按**声明顺序**列出 |
| 04 | 额外列、列顺序不同 | 通过（R4） |
| 05 | 大小写不一致 | 视为缺失，拒绝（R2） |
| 06 | 未声明必需列、文件缺列 | 通过，与基线一致（R8 回归保险） |

### 7.2 接口契约 → 普通 JUnit 单测（`CSVRequiredHeadersTest`）

C1、C2、C3；`ignoreHeaderCase=true` 时大小写不同也通过（R2）；显式表头缺列被拒（R5）；空输入被拒（R7）；
`getRequiredHeaders()` 返回副本；`equals`/`hashCode` 区分 requiredHeaders。

## 8. 与原大纲的偏离

| 大纲原文 | 实际 | 原因 |
| --- | --- | --- |
| 第一版 3 类规则 | 本切片只做 `requiredHeaders` | 切片纪律：一次只钉一条规则的业务事实 |
| TDD 红-绿 | Approved Scenarios 驱动 + 契约单测 | 业务事实进夹具（产品经理可读），接口契约进单测 |

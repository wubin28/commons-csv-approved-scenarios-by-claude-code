# 02 假设体检 + 追问：requiredHeaders

> 基线：`s1-explore` · 2026-09-22 · AI 方：Claude Code（彩排）· 讲师方：按课前拍板的 D1–D10 作答

## 讲师提示词

```text
基于 docs/dialogs/01-explore.md 的勘探结论，我要实现 requiredHeaders：
调用方声明 CSV 必须包含哪些列，缺列时在解析初始化阶段报错。允许额外列，不管列顺序。
动手前先做 Round 0：
1) 列出你已默认成立、但我没明说的假设（A1、A2…）；
2) 列出你还缺的关键信息，并说明每条会如何改变方案（M1、M2…）；
3) 指出人们做这类需求最常犯的一个错误；
4) 只问一个最关键的问题，附你的建议答案。
然后按"追问"推进：每轮把前沿上的问题全部编号提出、附建议答案，等我回答。不许写代码。
```

## Round 0

### AI

**默认假设**

- A1 API 挂在 `CSVFormat.Builder` 上，形如 `setRequiredHeaders(String...)`，与现有 setter 风格一致。
- A2 校验点在 `CSVParser` 构造器 `createHeaders()` 之后，即拿到 header map 的那一刻。
- A3 列名比较复用 header map 的 `containsKey`，所以 `ignoreHeaderCase` 自动生效。
- A4 报错用 `IllegalArgumentException`，与重复表头报错同类。
- A5 未设置 requiredHeaders 时，行为与基线逐字节一致。

**缺失信息**

- M1 缺多列时报一个还是报全部？→ 决定循环是提前返回还是收集后统一抛出，也决定夹具的预期消息。
- M2 没开启表头解析（`setHeader` 未调用）却声明了 requiredHeaders，怎么办？→ 决定校验放在 `CSVFormat.validate()`（构建期）还是解析期。
- M3 显式 `setHeader("a","b")` 给出的表头也要校验吗？→ 决定校验读 header map 还是只读文件首行。
- M4 requiredHeaders 自身非法（null、空串、重名）怎么处理？
- M5 空输入（连表头行都没有）算缺失还是跳过？

**最常犯的错误**

先让 AI 写出实现，跑一遍，再把实际异常消息粘进测试当"预期"。这样测试只是在复述代码：缺列顺序、大小写语义这些业务裁决从没被人拍板过。

**最关键的问题**

❓ **Q0** - **缺多列时的报告方式**: 上游文件同时漏了 `amount` 和 `currency`，调用方应当看到：A. 只报第一个缺失列；B. 按声明顺序一次报全部；C. 按表头顺序或字母序报全部。

➡️ 选 **B**：一次报全，免得"修一列、再跑、再报下一列"；顺序固定为声明顺序，Approved Scenarios 才能稳定 diff。

### 讲师

Q0 选 B。消息格式定为 `Missing required headers [amount, currency]; actual header: [order_id]`（D5）。

## Round 1

### AI

❓ **Q1** - **未开启表头时声明 requiredHeaders**: A. `build()` 时抛 `IllegalArgumentException`；B. 静默忽略。

➡️ A：声称在校验、实际没校验的守门选项比没有更危险。

❓ **Q2** - **显式表头也校验吗**: A. 校验（只看 header map，不管来源）；B. 只校验从文件首行读到的表头。

➡️ A：规则是"parser 暴露的列必须包含这些名字"，与来源无关。

❓ **Q3** - **requiredHeaders 自身非法**: 含 `null`、空串（含全空白）或重名 → A. `build()` 时抛 `IllegalArgumentException`；B. 自动去重、忽略空值。

➡️ A：配置错误应在构建期暴露。

❓ **Q4** - **大小写**: A. 默认精确匹配；`ignoreHeaderCase=true` 时不区分大小写；B. 永远精确匹配。

➡️ A：与 header map 既有语义一致，调用方不会看到"`get("ID")` 能取到、校验却说缺 `ID`"的矛盾。

❓ **Q5** - **空输入**: 文件为空、连表头行都没有 → A. 所有必需列都算缺失，抛异常，实际表头为 `[]`；B. 跳过校验。

➡️ A：空文件恰是"上游没发数据"的典型故障，必须在初始化阶段拦下。

### 讲师

Q1–Q5 全部同意建议答案（对应 D3、D8、D7、D4、D11）。重复表头仍由 `DuplicateHeaderMode` 处理，requiredHeaders 只看是否出现（D9）；额外列允许、不管顺序（D6）。

## Round 2

### AI

前沿清空。进入 spec：`docs/specs/required-headers.md`。

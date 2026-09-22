# 场景 03：缺失多列时一次全部报出，按声明顺序

上游同时漏发了 `currency` 和 `amount`。一次报全，免得"修一列、重跑、再报下一列"；
缺失列按**声明顺序**（`currency` 在前）列出，既不是字母序，也不是表头顺序。

规格：`docs/specs/required-headers.md` 第 7.1 节 #03、规则 R3

## 输入 `reconciliation.csv`

```csv
order_id,region
A-1001,US
A-1002,EU
```

## 格式配置

```properties
requiredHeaders = currency,order_id,amount
```

## 解析结果

```text
结论: 拒绝
异常: IllegalArgumentException
消息: Missing required headers [currency, amount]; actual header: [order_id, region]
```

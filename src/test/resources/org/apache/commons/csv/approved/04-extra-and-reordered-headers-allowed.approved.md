# 场景 04：额外列与列顺序不同都允许

上游多发了 `region`、`memo` 两列，列顺序也和声明不同。
本规则只管"必需列在不在"，额外列与顺序交给后续的 `allowUnknownHeaders` / `enforceHeaderOrder`。

规格：`docs/specs/required-headers.md` 第 7.1 节 #04、规则 R4

## 输入 `reconciliation.csv`

```csv
region,currency,amount,order_id,memo
US,USD,120.50,A-1001,首单
EU,EUR,80.00,A-1002,补发
```

## 格式配置

```properties
requiredHeaders = order_id,amount,currency
```

## 解析结果

```text
结论: 通过
实际表头: [region, currency, amount, order_id, memo]
记录 1: [US, USD, 120.50, A-1001, 首单]
记录 2: [EU, EUR, 80.00, A-1002, 补发]
```

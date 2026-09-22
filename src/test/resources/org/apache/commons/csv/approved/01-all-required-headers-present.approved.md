# 场景 01：必需列全部存在

对账文件的表头行包含声明的全部必需列，校验通过，记录照常读出。

规格：`docs/specs/required-headers.md` 第 7.1 节 #01

## 输入 `reconciliation.csv`

```csv
order_id,amount,currency
A-1001,120.50,USD
A-1002,80.00,EUR
```

## 格式配置

```properties
requiredHeaders = order_id,amount,currency
```

## 解析结果

```text
结论: 通过
实际表头: [order_id, amount, currency]
记录 1: [A-1001, 120.50, USD]
记录 2: [A-1002, 80.00, EUR]
```

# 场景 02：缺失一列

上游漏发了 `currency` 列。解析器在初始化阶段就拒绝这份文件，
报出缺失的列名和实际收到的表头，**一条记录都不交给业务代码**。

规格：`docs/specs/required-headers.md` 第 7.1 节 #02、规则 R1、R3

## 输入 `reconciliation.csv`

```csv
order_id,amount
A-1001,120.50
A-1002,80.00
```

## 格式配置

```properties
requiredHeaders = order_id,amount,currency
```

## 解析结果

```text
结论: 拒绝
异常: IllegalArgumentException
消息: Missing required headers [currency]; actual header: [order_id, amount]
```

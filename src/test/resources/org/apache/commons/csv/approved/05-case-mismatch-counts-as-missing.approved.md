# 场景 05：大小写不一致视为缺失

上游把 `order_id` 改成了 `Order_ID`。列名是上游契约，默认大小写精确匹配，
这种悄悄的改名必须被拦下，而不是被宽松匹配掩盖。

规格：`docs/specs/required-headers.md` 第 7.1 节 #05、规则 R2

## 输入 `reconciliation.csv`

```csv
Order_ID,amount,currency
A-1001,120.50,USD
```

## 格式配置

```properties
requiredHeaders = order_id,amount,currency
```

## 解析结果

```text
结论: 拒绝
异常: IllegalArgumentException
消息: Missing required headers [order_id]; actual header: [Order_ID, amount, currency]
```

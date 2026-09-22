# 场景 06：未声明必需列时，行为与改动前完全一致

没有调用 `setRequiredHeaders(...)`。即使文件缺了 `currency`，解析器也照旧放行，
和改动前一模一样——这是**回归保险**：新功能不开启，就不能改变任何现有调用方的结果。

规格：`docs/specs/required-headers.md` 第 7.1 节 #06、规则 R8

## 输入 `reconciliation.csv`

```csv
order_id,amount
A-1001,120.50
A-1002,80.00
```

## 格式配置

```properties
# 未声明 requiredHeaders
```

## 解析结果

```text
结论: 通过
实际表头: [order_id, amount]
记录 1: [A-1001, 120.50]
记录 2: [A-1002, 80.00]
```

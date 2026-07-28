# 确定环境交付执行生命周期的职责

Type: grilling
Status: open
Blocked by: 04

## Question

环境交付执行生命周期 module 应负责哪些节点选择、阶段顺序、状态转换、通知、异常优先级和工作区清理语义；哪些变化应留给内部 adapter，才能消除 `globalParameterMap` 的 seam 泄漏并保持既有行为？

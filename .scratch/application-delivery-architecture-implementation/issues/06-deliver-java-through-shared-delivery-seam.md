# 06 — 通过共享交付 seam 完成 Java 应用部署

**What to build:** 让当前统一配置可以通过替代入口完成 Java 应用构建、产物交接、串行节点部署和有序就绪验证，同时让既有 `deployJavaWeb` 入口作为兼容 adapter 委托同一 application delivery implementation。

**Blocked by:** 04 — 存储不可变且命名隔离的构建产物.

**Status:** ready-for-agent

- [ ] 替代入口根据统一配置选择 Java 构建和且仅一个部署策略。
- [ ] 同时启用多个 Java 部署策略或未启用任何部署策略时在预检中失败。
- [ ] 部署使用当前执行可达的构建产物，不隐式使用历史产物。
- [ ] 多个部署节点按确定顺序串行执行。
- [ ] 任一节点部署或就绪验证失败后停止后续节点，已成功节点不自动回滚。
- [ ] TCP、HTTP 和命令就绪验证按配置顺序执行，并在首个失败后停止。
- [ ] 既有 `deployJavaWeb` 合法旧配置在入口 adapter 转换后得到等价可观察结果。
- [ ] 替代入口不接受 `deployJavaWeb` 专属旧结构，兼容基线保持通过。

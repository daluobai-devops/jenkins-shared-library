# 12 — 收缩重复 implementation 并完成本地回归

**What to build:** 在三个既有入口都委托 deep module 后，删除已被取代的配置解析、交付生命周期和仓库分发重复 implementation，并把测试收缩到公开 interface 与仍有独立 depth 的内部 module。

**Blocked by:** 11 — 通过仓库分发执行多个可交付单元.

**Status:** ready-for-agent

- [ ] 三个既有入口仅保留入口契约、旧配置转换和委托职责。
- [ ] 替代入口只接受当前统一配置，不包含旧结构兼容分支。
- [ ] 后续交付 implementation 不再读取可变全局配置。
- [ ] 重复的配置优先级、阶段编排、通知、失败和清理规则只保留一个实现来源。
- [ ] 受限声明 parser、外部 adapter 和确有不同策略的 implementation 保持独立 depth。
- [ ] 被最高 seam 场景测试取代的内部方法测试被删除，不重复分层维护。
- [ ] 仍需局部测试的原子 Web 切换、受限声明解析和外部 adapter 契约继续覆盖。
- [ ] 完整本地测试通过，兼容样本、新替代入口和仓库分发场景全部回归。
- [ ] GitNexus 变更检测只显示预期 module 和执行流程受到影响。

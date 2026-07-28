# 应用交付架构决策地图

Label: wayfinder:map

## Destination

形成一份可交接的应用交付架构规格：确定交付预检、有效交付配置解析、环境交付执行生命周期、仓库分发四个 module 的职责、seam、依赖顺序、迁移约束与测试策略，并决定替代入口的入口契约。

既有 `deployWeb`、`deployJavaWeb`、`dispatchCodeupRepositories` 入口继续兼容旧配置；替代入口只接受当前统一配置结构。本地图只解决决策，不实施代码。

## Notes

- 领域：Jenkins Shared Library 的应用交付。
- 默认把现有 ADR 作为固定约束；仅当源码证据证明无法实施或相互冲突时，新增重开 ADR 的 decision ticket。
- 使用 `wayfinder`、`grilling`、`domain-modeling`、`codebase-design`；探索与变更前遵守仓库 GitNexus 规则。
- 规划采用增量迁移，不一次性切换四个 module。
- 兼容基线覆盖三个既有入口；替代入口不承担旧配置兼容。
- 验收同时包含本地自动化测试和测试 Jenkins Job `test`，不包含生产环境验证。
- 规划优先，除明确的前置 task 外，不在地图内实施 destination。

## Decisions so far

## Not yet specified

- 四个 module 的最终命名、包位置和 CPS/序列化约束，等待入口契约与职责确定后再具体化。
- 替代入口是否以及何时使既有入口进入弃用状态，等待迁移与使用方基线清晰后再判断。
- 规格最终采用单一文档还是文档加补充 ADR，等待各 ticket 暴露决策的可逆性与取舍强度。

## Out of scope

- Git 同步及 `syncGit2Git` 的架构调整；它不属于本次应用交付 destination。
- 历史产物部署、应用运维操作、销毁、多目标产物存储等规划能力。
- 生产环境部署或生产 Jenkins 验证。
- 本轮地图中的代码实现、提交、发布和删除既有入口。

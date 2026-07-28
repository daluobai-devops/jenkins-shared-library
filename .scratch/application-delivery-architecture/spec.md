# 应用交付深模块架构规格

Status: ready-for-agent

## Problem Statement

Jenkins Shared Library 的应用交付能力已经覆盖 Web 应用、Java 应用和仓库分发，但同一领域规则分散在多个 shallow module 中：

- 有效交付配置的读取、旧配置转换、固定优先级和执行覆盖分散在不同入口，维护者必须同时理解配置来源与各入口的局部规则。
- 构建、产物存储和部署约束在执行阶段才由不同策略 implementation 分别校验，直接交付与分发预检运行不能得到一致结论。
- Web 应用和 Java 应用重复节点选择、通知、阶段执行、状态映射、异常优先级和工作区清理等环境交付执行生命周期。
- 部署 implementation 通过可变全局状态取得真实依赖，公开 interface 没有完整表达调用所需知识。
- 仓库分发入口同时承担授权、发现、读取、受限声明解析、安全校验、交付、错误分类和汇总，缺少 locality。
- 测试经常越过入口契约，直接调用脚本内部方法、替换全局元类或构造隐式全局状态；内部重构会迫使测试同步修改。

这些问题降低了 module 的 depth：调用者需要掌握接近 implementation 复杂度的 interface，维护者修改一个规则时要跨多个位置同步，直接交付与分发预检运行也缺少共同的 test surface。

## Solution

将应用交付重构为少量 deep module，并以两个现有的最高 seam 作为调用和测试入口：

1. **应用交付入口契约**负责接收交付意图并返回可观察结果。一个新的替代入口只接受当前统一配置结构；`deployWeb`、`deployJavaWeb` 和 `dispatchCodeupRepositories` 继续保留各自既有入口契约和旧配置兼容。
2. **仓库分发入口契约**负责代码托管空间中的授权、发现、受限声明、预检运行、执行与汇总，并把实际应用交付委托给同一个应用交付 seam。

在这些 seam 后建立四个 deep module：

- **有效交付配置解析 module**：集中配置来源读取、旧配置转换、固定优先级、执行覆盖和配置来源追溯。
- **交付预检 module**：在任何交付副作用前集中验证有效交付配置、交付阶段、策略约束、产物依赖、应用标识、源码引用和目标节点。
- **环境交付执行生命周期 module**：在预检通过后统一管理节点、阶段、通知、状态、异常优先级和工作区清理。
- **仓库分发 module**：集中仓库授权、配置发现、受限声明解析、安全校验、确定性顺序、交付委托和结果汇总。

既有入口只作为兼容 adapter，把旧配置转换为当前统一结构后跨越应用交付 seam。新的替代入口不接受旧结构，不携带历史兼容分支。迁移按依赖顺序逐步完成，每一步都保持三个既有入口可用并可独立回退。

## User Stories

1. As an 应用维护者, I want to invoke one replacement entry with the current unified configuration, so that I do not need to choose between duplicated delivery implementations.
2. As a Web 应用维护者, I want the existing `deployWeb` entry to keep accepting previously valid legacy configuration, so that current Jenkins Jobs do not break during migration.
3. As a Java 应用维护者, I want the existing `deployJavaWeb` entry to keep accepting previously valid legacy configuration, so that current Jenkins Jobs can migrate independently.
4. As a 仓库分发 Job 维护者, I want the existing `dispatchCodeupRepositories` entry contract to remain compatible, so that repository dispatch can move to the new implementation without a coordinated cutover.
5. As a 新接入团队, I want the replacement entry to accept only the current unified structure, so that new usage does not inherit historical configuration branches.
6. As a 共享库维护者, I want all legacy conversion to occur in existing entry adapters, so that downstream implementation consumes one configuration model.
7. As a 共享库维护者, I want default configuration, extension configuration, primary delivery configuration, and execution overrides to use one fixed precedence, so that effective delivery configuration is deterministic.
8. As a Pipeline 使用者, I want missing optional extension configuration to be distinguished from malformed or unreadable configuration, so that optional behavior does not hide real failures.
9. As a Pipeline 使用者, I want configuration errors to identify the invalid delivery concept, so that I can correct the Jenkins Job or repository declaration before execution.
10. As an 运维人员, I want complete delivery preflight before build, artifact storage, deployment, or delivery notification, so that rejected delivery creates no delivery side effects.
11. As an 运维人员, I want invalid delivery-stage combinations rejected consistently, so that direct delivery and dispatch dry run never disagree.
12. As an 运维人员, I want deployment to require a current-execution artifact or a valid direct artifact handoff, so that unsupported historical artifact deployment cannot occur accidentally.
13. As a 安全负责人, I want unsupported operation modes rejected instead of falling back to default delivery, so that explicit operator intent cannot produce the opposite action.
14. As a 安全负责人, I want repository-provided command values checked against environment authorization, so that a restricted delivery Jenkinsfile cannot introduce unauthorized shell behavior.
15. As a 安全负责人, I want repository authorization kept separate from delivery opt-in, so that adding a repository delivery configuration does not grant environment access.
16. As a 仓库维护者, I want restricted delivery Jenkinsfiles parsed as declarations rather than executed as arbitrary Pipeline logic, so that repository dispatch remains safe.
17. As a monorepo 维护者, I want every recursively discovered delivery configuration treated as an independent deliverable unit, so that multiple applications in one repository can be delivered safely.
18. As a monorepo 维护者, I want each deliverable unit to have an explicit application identifier, so that artifact and deployment targets cannot collide.
19. As a 仓库分发 Job 维护者, I want dispatch order to be deterministic, so that repeated runs process the same discovered units in the same order.
20. As a 仓库分发 Job 维护者, I want one rejected or failed unit not to stop unrelated units, so that isolated failures do not block the rest of the dispatch.
21. As a 仓库分发 Job 维护者, I want dispatch to fail at the end when any unit was rejected or failed, so that partial failure is not hidden by a successful Job result.
22. As a Pipeline 使用者, I want a dispatch dry run to perform the same configuration resolution and delivery preflight as real delivery, so that it predicts execution accurately.
23. As a Pipeline 使用者, I want the source reference resolved and fixed during delivery preflight, so that the build uses the version that was validated.
24. As an 审计人员, I want configuration source, resolved configuration revision, source reference, and resolved source revision retained in observable delivery results, so that a delivery can be reconstructed.
25. As an 审计人员, I want stored artifacts to use immutable namespaced identities, so that units with identical source filenames cannot overwrite one another.
26. As an 运维人员, I want serial node deployment to stop on the first deployment or readiness failure, so that failure does not continue spreading to later nodes.
27. As an 运维人员, I want readiness verification to run in its configured order and stop after the first failure, so that later checks do not obscure the initial failure.
28. As a Pipeline 使用者, I want the primary delivery failure to remain the reported failure even when notification or cleanup also fails, so that the original cause is not lost.
29. As a Pipeline 使用者, I want workspace cleanup failure to fail an otherwise successful delivery, so that residual files cannot silently contaminate later work.
30. As a Pipeline 使用者, I want an active environment delivery execution to be superseded according to the existing execution rule, so that the latest delivery intent takes precedence.
31. As a 共享库维护者, I want Web and Java delivery to share one execution lifecycle implementation, so that notification, failure, and cleanup fixes have locality.
32. As a 策略实现维护者, I want effective delivery configuration passed explicitly, so that implementation does not depend on mutable global Pipeline state.
33. As a 测试维护者, I want tests to call the same interface as production callers, so that tests survive internal refactoring.
34. As a 测试维护者, I want an in-memory Codeup adapter for repository dispatch tests, so that tests do not replace global metaClasses or call the network.
35. As a 测试维护者, I want direct delivery and dispatch dry run evaluated with shared scenario fixtures, so that differences are detected immediately.
36. As a 发布负责人, I want migration split into independently verifiable stages, so that each deep module can be adopted or rolled back without a full rewrite.
37. As a 发布负责人, I want the test Jenkins Job `test` to validate shared-library runtime behavior, so that local Groovy tests are not mistaken for CPS and Jenkins compatibility.
38. As a future coding agent, I want module responsibilities, seams, compatibility rules, and acceptance evidence recorded explicitly, so that implementation does not reopen settled architecture decisions.

## Implementation Decisions

- Existing ADRs remain fixed constraints unless implementation evidence exposes an actual contradiction. The work implements ADR-0001, ADR-0012, ADR-0014 and the related repository-dispatch ADRs rather than re-litigating them.
- The architecture exposes two highest external seams: the application delivery entry contract and the repository dispatch entry contract. No additional external seam is introduced for individual build or deployment strategies.
- A single replacement entry is the preferred current entry for both Web applications and Java applications. Application type and strategy selection are configuration semantics hidden behind the entry interface.
- The replacement entry accepts only the current unified configuration structure. It does not contain legacy-layout detection or configuration-version branches.
- The existing `deployWeb`, `deployJavaWeb`, and `dispatchCodeupRepositories` entries remain stable compatibility adapters. Each converts previously valid legacy configuration at its entry before delegating to current implementation.
- Compatibility means preserving the observable semantics of previously valid configurations and calls. Invalid or unsafe input is not made valid merely because an older implementation accepted it accidentally.
- A compatibility corpus must be established from existing tests, examples and known Jenkins usage before migration. That corpus defines the regression contract for existing entries.
- Effective delivery configuration is produced from shared-library defaults, optional extension configuration, primary delivery configuration and execution overrides in the precedence already established by ADR-0001.
- Every source configuration is normalized before it participates in merging. The merged result is copied and treated as the only configuration structure consumed by later implementation.
- Optional configuration absence is handled only at configuration-reading adapters. Invalid type, malformed location, parse failure, transport failure other than a confirmed missing resource, and invalid content remain errors.
- The effective delivery configuration model carries the domain information required for preflight and execution: application identity, target environment, application type, delivery stages, build strategy, artifact-storage target, deployment strategy, source reference, deliverable-unit source directory, deployment nodes, readiness verification, notification preferences and configuration provenance.
- The effective delivery configuration parser is a deep internal module. Its interface hides source-specific reading, legacy conversion, precedence and execution-override mechanics.
- Delivery preflight is a deep internal module used by direct delivery, the replacement entry and dispatch dry run. It is the sole owner of cross-stage and cross-strategy invariants.
- Delivery preflight completes before build, artifact storage, deployment and delivery notifications. A failed preflight produces a structured failure and no delivery side effects.
- Delivery preflight enforces the currently supported stage matrix: build only; build plus artifact storage; build plus deployment when direct artifact handoff is valid; and build plus artifact storage plus deployment. Empty delivery configuration and all other combinations fail.
- Delivery preflight rejects unsupported operation modes, multiple artifact-storage targets, conflicting deployment strategies, missing application identifiers, unresolved source references, invalid source directories, unavailable required nodes and invalid artifact reachability.
- Source-reference resolution and repository facts use external adapters. Production adapters talk to the configured code host; in-memory adapters provide deterministic test facts.
- The environment delivery execution lifecycle begins only with a successful preflight result. It owns node selection, ordered stage execution, observable status, delivery notification, exception precedence and workspace cleanup.
- Build, artifact-storage, deployment and notification variations are internal adapters. A seam is retained only where production and test adapters or multiple real strategies exist.
- Effective delivery configuration and preflight results flow explicitly into execution. Mutable `globalParameterMap` state is not part of the new module interface.
- Existing failure precedence remains: a primary delivery failure is not replaced by later notification or cleanup failure; when no primary failure exists, mandatory cleanup or required completion behavior can fail the delivery.
- Workspace cleanup remains a mandatory success condition, and execution supersession continues to follow the established ordering and cleanup decisions.
- The repository dispatch entry delegates orchestration to a deep repository dispatch module. The entry adapter is responsible only for its stable Jenkins-facing contract.
- The repository dispatch module owns authorization, recursive configuration discovery, one-time configuration reading, restricted declaration parsing, remote command authorization, delivery preflight delegation, dispatch order, execution continuation and final summary.
- The restricted Jenkinsfile parser remains a deep internal module. Its small interface continues to hide AST validation and literal-only declaration rules; it is not folded back into dispatch orchestration.
- Codeup is a true external dependency with a production adapter and an in-memory adapter. The two adapters form a real seam for repository discovery and configuration retrieval.
- Dispatch dry run performs restricted declaration parsing, effective configuration resolution and the same complete delivery preflight as real delivery, but does not execute delivery stages or send delivery notifications.
- Repository dispatch continues processing independent rejected or failed deliverable units and records their individual outcomes; configured fail-at-end behavior determines the final Job result.
- Migration is incremental in dependency order: compatibility baseline; effective delivery configuration parsing; delivery preflight; replacement entry; shared execution lifecycle; repository dispatch delegation; test Jenkins acceptance; then adoption guidance.
- Each migration stage must be independently releasable and reversible while existing entry adapters continue to satisfy their compatibility corpus.
- New implementation must not add current support for historical artifact deployment, application operations, teardown or multi-target artifact storage.

## Testing Decisions

- A good test crosses the same interface as a caller, supplies an input configuration or repository state, and asserts observable configuration results, preflight outcomes, stages, notifications, cleanup, dispatch records or errors. It does not call private compatibility, merge, parser-helper or strategy-helper methods merely because those methods are easy to isolate.
- The primary test surface is the application delivery entry contract. Scenario tests exercise effective configuration parsing, preflight and lifecycle behavior through that interface.
- The three existing entry adapters have compatibility tests driven by the compatibility corpus. Each fixture asserts equivalent effective configuration and externally observable behavior before and after delegation.
- The replacement entry has negative tests proving that legacy-only layouts are rejected rather than silently normalized.
- Effective delivery configuration tests cover every precedence level, false/zero/empty override values, missing optional sources, malformed sources, legacy normalization in existing adapters, immutability and source provenance.
- Delivery preflight tests are table-driven across supported and unsupported stage combinations, build strategies, storage targets, deployment strategies, artifact reachability, application identifiers, source references, deliverable-unit directories and target nodes.
- Every failed-preflight test asserts absence of delivery side effects, including build, storage, deployment and delivery notifications.
- Shared fixtures run through direct delivery and dispatch dry run and must produce equivalent preflight decisions and error classifications.
- Execution lifecycle tests cover ordered stages, serial-node stopping, readiness ordering, primary failure precedence, notification failure, cleanup failure, aborted execution and execution supersession.
- Strategy implementation tests remain only where they verify behavior not observable economically through the highest seam, such as atomic Web directory switching or generated runtime artifacts. These tests do not redefine cross-stage policy.
- Repository dispatch tests cross the repository dispatch entry contract with an in-memory Codeup adapter. They cover repository authorization, recursive discovery, multiple deliverable units, deterministic order, withdrawn configuration, restricted declarations, command authorization, continuation, fail-at-end and summary counts.
- Restricted declaration parser tests remain focused on its stable parse interface because the parser already hides substantial AST security implementation behind a small interface.
- External production adapters have contract tests for request construction, status handling, pagination, encoding and missing-resource semantics. Network access is not required by ordinary orchestration tests.
- Test doubles are passed through seams rather than installed through global metaClass changes. Fake Jenkins steps remain acceptable for local-substitutable Jenkins behavior.
- The test Jenkins Job `test` performs acceptance scenarios through the public shared-library entries. It verifies CPS serialization, Jenkins stage creation, node selection, credential aliases, environment propagation, stash/unstash behavior, interruption handling and workspace cleanup.
- Test Jenkins acceptance includes at least one legacy Web entry scenario, one legacy Java entry scenario, one replacement-entry scenario, one repository dispatch dry run and one rejected-preflight scenario.
- Production environment deployment is not part of acceptance.
- Prior art includes existing entry tests for Web and Java configuration precedence and failure preservation, repository dispatch tests for authorization and continuation, configuration utility tests for optional-source semantics, deployment tests for strategy selection, and Web deployment tests for staged atomic switching and rollback.
- Tests superseded by the deep module interface are removed rather than layered indefinitely. The interface is the test surface.

## Out of Scope

- Architectural changes to Git synchronization or `syncGit2Git`.
- Historical artifact deployment.
- Application operations such as service management.
- Teardown of deployed application instances.
- Multi-target artifact storage.
- Cross-environment artifact promotion.
- A delivery database separate from Jenkins Build Records.
- Production Jenkins deployment or production environment acceptance.
- Immediate removal of `deployWeb`, `deployJavaWeb` or `dispatchCodeupRepositories`.
- Rewriting existing ADRs without a concrete implementation contradiction.
- Adding seams that have only one adapter or exist solely to make an internal method mockable.

## Further Notes

- The corresponding decision map is **应用交付架构决策地图**. It records the original fog and migration questions that led to this specification.
- Domain vocabulary follows the repository glossary: application delivery, effective delivery configuration, delivery preflight, dispatch dry run, deliverable unit, source reference, resolved source revision, delivery record, replacement entry and deprecated entry.
- Architecture vocabulary follows the deep-module model: module, interface, implementation, depth, seam, adapter, leverage and locality.
- Existing entries are not automatically deprecated by introducing the replacement entry. Deprecation requires usage verification, migration and a separate explicit decision.
- If implementation reveals that a settled ADR cannot be satisfied, work stops at that conflict and opens a focused decision rather than silently changing the architecture.

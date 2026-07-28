# 测试 Jenkins 应用交付验收记录

## 验收目标

使用测试 Jenkins Job `test` 和测试 Node `app-jgzly-app02`，通过公开共享库入口验证：

- 既有 Web/Java 入口的合法旧配置；
- 当前统一配置的替代交付路径；
- 仓库分发 dry-run；
- 预检拒绝且无交付副作用；
- CPS 序列化、阶段创建、节点选择、环境变量、源码版本记录、终止与工作区清理。

验收脚本：[application-delivery-acceptance.Jenkinsfile](application-delivery-acceptance.Jenkinsfile)

## 安全边界

- 只使用测试 Jenkins 和 `app-jgzly-app02`。
- 不执行生产环境部署。
- Web/Java 旧入口场景关闭全部阶段，只验证入口兼容和清理。
- 新入口与分发场景使用内存 adapter，不访问真实 Codeup、不部署应用。
- 凭据只使用 Jenkins 管理的别名；验收脚本不包含 Token、密码或私钥。

## 执行结果

状态：已完成（2026-07-28）。

本地前置验证：`gradlew clean test --no-daemon` 通过，26 个测试套件、120 个测试、0 失败。

### 主场景验收

- Build：[test #47](https://jenkins.nanjingzhengshang.com/job/test/47/)
- 结果：`SUCCESS`
- 共享库版本：`a97743283fd13710f1c62ec283d12a55f00a3930`（Gitee `test`）
- 执行节点：外层 Pipeline 使用 `app-jgzly-app02`；旧入口保持历史 `buildNode` 选择行为，本次解析到 `master`。
- 阶段证据：`legacy-web`、`legacy-java`、`replacement-entry`、`dispatch-dry-run`、`rejected-preflight` 全部完成。
- 统一入口断言固定源码版本为 `acceptance-sha`，并通过 `directArtifactHandoff=true` 观察到 `deploy:app-jgzly-app02`；使用内存 adapter，不执行真实部署。
- 分发 dry-run 断言完整预检通过且没有交付副作用；拒绝场景断言 `TEARDOWN` 在预检阶段失败且 stage event 为空。
- Jenkins 成功加载共享库、完成 CPS 执行和公开入口调用；SCM 访问仅显示 Jenkins 凭据别名 `ssh-git`，未输出凭据内容。

### 终止与清理验收

- Build：[test #46](https://jenkins.nanjingzhengshang.com/job/test/46/)
- 结果：`ABORTED`
- 共享库版本：`5375fb15e27be7b2abe4c5b51d3ceb6fa10e26cf`
- 在 `abort-delivery` 的可控 `sleep` 期间从 Jenkins UI 执行 Stop。
- Console 依次记录 `ABORT_ACCEPTANCE_BUILD_STARTED`、`Aborted by admin`、`ABORT_ACCEPTANCE_CLEANUP_EXECUTED` 和 `Finished: ABORTED`，证明中断保留原异常类型且 finally 清理已执行。

验收结束后已恢复 Job 原 Pipeline 脚本，并逐字校验恢复内容一致。

## 已知限制

- 内存 adapter 场景验证交付 seam、CPS 调用和直接产物交接契约，不验证真实 Codeup 网络、真实 Maven/NPM 构建或生产部署。
- 本次未向生产环境部署，也未使用真实应用凭据。
- 并发执行取代的状态转换由本地协调器测试覆盖；测试 Jenkins 覆盖了真实 UI Stop、`FlowInterruptedException`、清理顺序和 `ABORTED` 状态。

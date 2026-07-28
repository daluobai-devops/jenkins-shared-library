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

状态：待执行。

本地前置验证（2026-07-28）：`gradlew clean test --no-daemon` 通过，26 个测试套件、119 个测试、0 失败；该结果覆盖配置契约、预检零副作用、固定源码版本、产物身份、执行取代状态、两阶段仓库分发及旧入口回归，但不替代 Jenkins CPS/插件运行时验收。

当前阻塞：测试 Jenkins 浏览器会话要求登录，项目凭据台账仅登记 API Token，未登记对应用户名。

执行后记录 Job Build URL、结果、时间以及各 stage 的证据；不得复制凭据或完整敏感日志。

## 已知限制

- 本地 Groovy 测试不能替代 CPS 与插件运行时验收。
- 内存 adapter 场景验证 seam 和序列化，不验证真实 Codeup 网络、真实 Maven/NPM 构建或生产部署。
- 中止场景需要在 `replacement-entry` stage 运行时从 Jenkins UI 触发一次 Stop，并确认 `ABORTED` 与清理日志；它不应触发部署。

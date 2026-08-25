# 应用交付入口接入与回退说明

## 入口选择

- 新接入使用 `deliverApplication`。它自动加载 `config/delivery-defaults.json`，再接受调用方的 `defaults`、`extension`、`primary`、`overrides` 四层当前统一配置；固定优先级为“内置默认 < 调用方默认覆盖 < 扩展 < 主配置 < 执行覆盖”。
- 既有 Web Job 继续使用 `deployWeb`；既有 Java Job 继续使用 `deployJavaWeb`。两者长期接受此前合法的旧配置，并在入口一次性转换后委托统一交付实现。
- 既有 Codeup 分发 Job 继续使用 `dispatchCodeupRepositories`。仓库授权、递归发现、受限声明、预检、继续执行和汇总由统一分发模块完成。

## 当前统一配置

```groovy
deliverApplication([
    defaults: [DELIVERY: [
        application: [type: 'JAVA'],
        targetEnvironment: 'test',
        operationMode: 'DEPLOY'
    ]],
    extension: [DELIVERY: [:]],
    primary: [DELIVERY: [
        application: [id: 'orders'],
        source: [
            repository: 'git@example.com:team/orders.git',
            reference: 'main',
            directory: 'services/orders'
        ],
        stages: [
            build: [
                enabled: true,
                strategy: 'MAVEN',
                config: [lifecycle: 'clean package', skipTest: false]
            ],
            storage: [enabled: false],
            deploy: [enabled: false]
        ],
        notification: [enabled: false]
    ]],
    overrides: [DELIVERY: [:]],
    execution: [id: env.BUILD_TAG, number: env.BUILD_NUMBER]
])
```

替代入口拒绝 `DEPLOY_PIPELINE`、旧入口兼容标志以及显式的 `SERVICE`、`TEARDOWN` 等未实现操作模式。

内置默认文件缺失、JSON 格式错误或根节点不是对象时，新入口立即失败。SSH/SCP 风格仓库在没有显式源码凭据时继承内置 `ssh-git`；HTTP/HTTPS 仓库不自动附加 SSH 凭据，HTTPS 私有仓库认证当前不支持。凭据别名字段缺失或为空字符串表示继承，精确值 `-` 表示清除继承值，`null` 属于无效配置。

## 阶段与错误语义

- 合法组合：仅构建、构建加单目标存储、构建加可达产物部署、构建加存储加部署。
- 完整预检先于工作区初始化、构建、存储、部署和交付通知。预检失败不产生这些副作用。
- 源码分支、Tag 或 Commit 在预检中解析为固定 Commit SHA；构建使用该 SHA。
- 新入口只检出一个 `source/` 源码工作副本，Maven/NPM 从 `source/<source.directory>` 构建；旧入口仍保持原有自检出目录和行为。
- 新入口的 Maven `subModule` 相对 `source.directory` 解析；从仓库根聚合构建时使用 `directory: '.'` 和仓库相对模块路径，避免重复目录前缀。
- Web 部署只接受 ZIP/TAR，要求部署节点和目标目录；Java 部署只能选择一个策略。
- Java 节点串行处理；节点部署或按配置顺序执行的就绪验证首次失败后停止。
- 主交付失败优先于通知和清理失败。通知失败只记录告警；没有主失败时，清理失败使交付失败。
- 外部存储身份包含目标环境、应用标识和 Jenkins 执行标识，禁止覆盖并记录内容校验值。
- 分发单元按仓库名、配置相对路径排序；撤回、跳过、拒绝、执行失败和成功分别记录。只要存在拒绝或执行失败，最终 Jenkins Build 标记为失败；`failAtEnd=true` 还会在全部单元处理完成后抛出汇总错误。

## 既有入口兼容

旧配置只在 `deployWeb`、`deployJavaWeb` 的入口 adapter 中归一化。后续配置解析、预检和生命周期不读取 `globalParameterMap`。旧入口保持原通知标题、结束清理、终止异常及此前合法的全阶段关闭行为。

## 增量回退

1. 新入口异常：让新 Job 暂时切回对应的 `deployWeb` 或 `deployJavaWeb`；不删除旧入口。
2. 共享生命周期异常：回退包含 `LegacyApplicationDeliveryEntrypoint` 委托的提交，恢复旧入口实现；新入口停止接入。
3. Codeup 分发异常：回退 `dispatchCodeupRepositories` 的 deep-module 委托提交，保留既有授权配置。
4. 每次回退后运行兼容基线与完整本地测试，并在测试 Jenkins `test` Job 重跑验收脚本。

回退不修改仓库中的交付声明，也不要求删除或重命名既有入口。

## 已知限制

- 不支持历史产物部署、服务管理、销毁、多目标存储或跨环境产物晋级。
- 交付历史依赖 Jenkins Build Record 的保留策略。
- 生产 Git adapter 当前通过 Jenkins 管理的凭据别名访问源码；仓库配置不得携带真实密钥。

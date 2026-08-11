# wuzhao-jenkins-generate-config 设计说明

## 背景

为 Jenkins Shared Library 提供一个个人 skill，用于根据应用仓库信息生成可直接作为 Jenkins Pipeline script 使用的完整 `Jenkinsfile.groovy`。skill 同时支持既有入口 `deployJavaWeb`、`deployWeb` 和替代入口 `deliverApplication`，并遵守共享库的旧配置兼容边界。

## 目标

- 安装到 `C:\Users\wuzhao\.agents\skills\wuzhao-jenkins-generate-config`。
- 每次只生成一个目标 Jenkinsfile，默认文件名为 `Jenkinsfile.groovy`。
- 支持旧 Java、旧 Web、新 Java、新 Web；Java 支持独立 JAR 服务和 Tomcat。
- 优先从当前项目推断参数，仅询问无法可靠推断的必填信息。
- 生成前展示推断结果和来源，确认后写文件。
- 生成后执行静态验证，不输出含占位符、结构冲突或疑似明文密钥的文件。

## 非目标

- 不调用真实 Jenkins，不触发构建或部署。
- 不管理 Jenkins Credentials，只引用 Credentials ID。
- 不为新入口生成未实现的 `SERVICE`、`TEARDOWN` 或历史产物部署配置。
- 不在一次调用中批量生成多个入口或应用类型的 Jenkinsfile。

## Skill 结构

```text
wuzhao-jenkins-generate-config/
├── SKILL.md
├── agents/
│   └── openai.yaml
├── assets/
│   └── templates/
│       ├── legacy-java.groovy.tpl
│       ├── legacy-web.groovy.tpl
│       ├── unified-java.groovy.tpl
│       └── unified-web.groovy.tpl
├── references/
│   ├── entry-schemas.md
│   └── inference-rules.md
└── scripts/
    ├── generate_config.py
    └── test_generate_config.py
```

`SKILL.md` 负责识别请求、探索项目、组织补问、展示推断结果和调用生成器。模板固定入口和配置骨架。参考文件保存字段结构、兼容规则与推断依据。Python 脚本负责确定性渲染和静态校验。

## 输入与选择

skill 必须唯一确定以下维度：

1. 入口：旧入口或新入口。
2. 应用类型：Java 或 Web。
3. Java 部署策略：`JAVA_SERVICE` 或 `TOMCAT`。
4. 交付阶段：构建、存储和部署的合法组合。
5. 输出目录；未指定时使用当前工作目录。

如果一个维度存在多个候选，skill 列出候选让用户选择，不自行猜测。

## 参数推断

参数来源按以下优先级处理：

1. 用户明确提供的值。
2. 现有 Jenkinsfile 或交付配置。
3. `pom.xml`、Gradle 构建文件、`package.json` 等项目描述文件。
4. Git 远程地址、当前分支、仓库名和目录结构。
5. 用户补充的必填信息。

主要规则：

- Maven 项目推断为 Java；前端 `package.json` 项目推断为 Web。
- 只有 Gradle 构建文件且没有 Maven 配置的 Java 项目标记为当前共享库不支持，不伪装成 Maven 配置。
- Maven `war` 包优先推断 `TOMCAT`；JAR 或 Spring Boot 项目优先推断 `JAVA_SERVICE`。
- 应用 ID 依次取现有配置、Maven `artifactId`、`package.json.name`、仓库目录名。
- Git 地址和引用优先取现有配置，其次取 Git 远程地址和当前分支。
- 发布环境、节点标签、目标路径和 Credentials ID 只有在存在明确项目证据时才能推断，否则必须询问。
- 同时存在多个可交付单元时，列出单元路径供用户选择。

生成前必须展示最终值、推断来源和仍由用户提供的值，获得确认后才写文件。

## 入口配置规则

### 旧入口

- Java 使用 `deployJavaWeb(customConfig)`，Web 使用 `deployWeb(customConfig)`。
- 生成 `SHARE_PARAM`、`DEPLOY_PIPELINE`、可选 `DEFAULT_CONFIG` 和 `CONFIG_EXTEND`。
- 新生成的旧 Java 配置使用 `DEPLOY_PIPELINE.stepsBuild.stepsBuildMaven`。
- 读取既有配置时兼容历史 `DEPLOY_PIPELINE.stepsBuildMaven`，但不以该历史布局生成新文件。
- 旧入口自身仍由共享库入口 adapter 转换为统一内部结构；生成器不在实现层复制兼容分支。
- 通知默认关闭并省略机器人密钥字段，不把企业微信 Key 或飞书 Token 写入 Jenkinsfile。

### 新入口

- 使用 `deliverApplication(deliveryConfig)`。
- 只生成 `defaults`、`extension`、`primary`、`overrides` 四层配置，以及执行身份 `execution`。
- `primary.DELIVERY` 描述应用、目标环境、操作模式、源码、阶段和通知。
- 禁止出现 `DEPLOY_PIPELINE`、`SHARE_PARAM`、`CONFIG_EXTEND`、`legacyCompatibility` 或 `legacyNoop`。
- `operationMode` 固定为 `DEPLOY`。
- 通知默认关闭；生成器不接受真实通知密钥作为模板参数。

## 输出与覆盖策略

- 默认输出 `<当前工作目录>\Jenkinsfile.groovy`。
- 用户可以显式指定其他目录或文件名。
- 目标文件已存在时停止，并在获得明确授权后才覆盖。
- 输出必须包含 `@Library('jenkins-shared-library') _`、完整配置 Map 和对应入口调用。
- 一次调用只写一个 Jenkinsfile，不创建额外配置副本。

## 验证与错误处理

生成器在写入前验证：

- 入口和配置结构匹配。
- 应用类型与构建策略匹配：Java 使用 Maven，Web 使用 NPM。
- Java 部署策略为 `JAVA_SERVICE` 或 `TOMCAT`，Web 为 `WEB_STATIC`。
- 存储和部署要求启用构建；部署必须满足产物可达性要求。
- Web 产物使用 ZIP 或 TAR，`JAVA_SERVICE` 使用 JAR，`TOMCAT` 使用 WAR。
- 应用 ID、源码地址、源码引用、源码目录、目标环境等必填字段非空。
- 模板中不存在未替换占位符。
- 内容中不存在私钥标记或疑似密码、Token、API Key 明文；敏感字段只接受 Credentials ID。

无法唯一确定入口、类型或策略，缺少必填信息，检测到结构冲突、安全风险，或目标文件未获覆盖授权时，必须停止且不写半成品。

## 测试策略

- 在无 skill 的独立场景中记录基线失败，重点观察入口混用、必填字段缺失和明文凭据风险。
- 使用 skill 后复测相同场景，确认代理先推断、再补问、确认后只生成一个文件。
- 生成器测试覆盖旧 Java 服务、旧 Java Tomcat、旧 Web、新 Java 服务、新 Java Tomcat和新 Web。
- 测试历史旧 Java 配置可以被识别，但新输出使用规范旧入口布局。
- 在临时目录验证覆盖保护、占位符检查、敏感信息检查和非法阶段组合。
- 运行 skill-creator 的 `quick_validate.py`，验证目录名、YAML frontmatter 和 `agents/openai.yaml`。
- 前向测试不得访问真实 Jenkins 或执行部署。

## 完成标准

- skill 位于约定的 `.agents\skills` 目录并通过结构验证。
- 六个生成场景均产生结构正确的完整 `Jenkinsfile.groovy`。
- 旧配置兼容只体现在读取与入口边界，生成器和后续实现不散落版本判断。
- 已有目标文件不会被静默覆盖。
- 输出不包含未替换占位符和真实密钥。

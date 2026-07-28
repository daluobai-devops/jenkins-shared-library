// 加载 Jenkins Shared Library。
// 测试共享库分支时可写成：@Library('jenkins-shared-library@test') _
@Library('jenkins-shared-library') _

// deliverApplication Java Web（独立 JAR + systemd）发布示例。
// 请按实际环境替换应用名、源码仓库、模块、JDK、发布节点、端口和启动参数。
//
// 配置合并优先级（从低到高）：
// defaults < extension < primary < overrides
// 新入口只接受当前统一配置，不接受 DEPLOY_PIPELINE、SHARE_PARAM、CONFIG_EXTEND 等旧结构。
def deliveryConfig = [
        // 全局默认值，适合放 Jenkins 环境中通用的基础配置。
        defaults : [
                DEFAULT_CONFIG: [
                        docker: [
                                registry: [
                                        // Maven 构建镜像仓库域名。
                                        domain       : 'docker.io',
                                        // 私有镜像仓库的 Jenkins Credentials ID；公开仓库可留空。
                                        credentialsId: ''
                                ]
                        ],
                        git  : [
                                // SSH Git 仓库默认使用的 Jenkins Credentials ID。
                                credentialsId: 'ssh-git'
                        ],
                        agent: [
                                // Jenkins 发布节点使用的 SSH Credentials ID。
                                credentialsId: 'ssh-jenkins'
                        ]
                ]
        ],

        // 扩展层：可放置团队或项目级公共配置，优先级高于 defaults。
        // 不需要扩展配置时保留空 DELIVERY 即可。
        extension: [
                DELIVERY: [:]
        ],

        // 主配置层：描述这一个应用的完整交付流程。
        primary  : [
                DELIVERY: [
                        application      : [
                                // 应用唯一标识；同时用于 systemd 服务名和目标目录名。
                                id  : 'demo-java-api',
                                // Java 应用固定填写 JAVA。
                                type: 'JAVA'
                        ],
                        // 目标环境名称，必填，例如 dev、test、staging、prod。
                        targetEnvironment: 'test',
                        // 新入口目前只支持 DEPLOY。
                        operationMode    : 'DEPLOY',

                        source           : [
                                // 源码仓库地址，支持 HTTPS 或 SSH。
                                repository   : 'git@gitee.com:example/demo-java.git',
                                // 源码分支、Tag 或 Commit；预检阶段会解析并固定为 Commit SHA。
                                reference    : 'main',
                                // 仓库内构建目录，必须是安全的相对路径；仓库根目录填写 "."。
                                directory    : '.',
                                // 私有仓库的 Jenkins Credentials ID。
                                credentialsId: 'ssh-git'
                        ],

                        stages           : [
                                build  : [
                                        // 是否执行构建阶段；启用存储或部署时必须为 true。
                                        enabled : true,
                                        // Java 应用固定使用 MAVEN。
                                        strategy: 'MAVEN',
                                        artifact: [
                                                // Maven 构建后归一化的标准产物路径。
                                                path    : 'package/app.jar',
                                                // JAVA_SERVICE 发布使用 app.jar。
                                                fileName: 'app.jar'
                                        ],
                                        config  : [
                                                // Maven 子模块路径。
                                                // 配置后会按该模块及其依赖构建；单模块项目可删除此项。
                                                subModule        : 'demo-web/demo-admin',
                                                // 是否跳过测试，对应 Maven 跳过测试参数。
                                                skipTest         : true,
                                                // Maven 生命周期命令，必填。
                                                lifecycle        : 'clean package',
                                                // settings.xml 来源，支持 RESOURCES、HOST_PATH、URL。
                                                settingsFullPath : 'RESOURCES:config/settings.xml',
                                                // Maven + JDK 构建容器镜像。
                                                dockerBuildImage : 'registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo/build-maven:3.9.8-jdk21',
                                                // Maven -P Profile；不需要时留空。
                                                activeProfile    : ''
                                        ]
                                ],

                                storage: [
                                        // 是否存储构建产物。跨节点部署时建议使用 Jenkins Stash。
                                        enabled: true,
                                        target : [
                                                // 在构建节点和发布节点间传递 JAR。
                                                type: 'JENKINS_STASH'
                                        ],
                                        config : [
                                                // 独立 Java 服务使用 JAR。
                                                archiveType: 'JAR',
                                                jenkinsStash: [
                                                        // 启用 Jenkins Stash。
                                                        enable: true
                                                ]
                                        ]
                                ],

                                deploy : [
                                        // 是否执行部署阶段。
                                        enabled : true,
                                        // 独立 JAR 服务使用 JAVA_SERVICE。
                                        // WAR 发布到 Tomcat 时改为 TOMCAT，并使用对应 Tomcat 配置。
                                        strategy: 'JAVA_SERVICE',
                                        // Jenkins 发布节点标签；标签必须能解析到在线节点。
                                        nodes   : ['NODE-DEMO'],
                                        config  : [
                                                // 发布节点上的 Java 可执行文件路径；不填时使用默认 java。
                                                javaPath  : '/usr/local/jdk/jdk21/bin/java',
                                                // 服务发布根目录。
                                                // 最终 JAR 路径为 /apps/application/demo-java-api/app.jar。
                                                pathRoot  : '/apps/application/',
                                                // 服务管理方式支持 systemctl、shell；默认 systemctl。
                                                manageBy  : 'systemctl',
                                                // JVM 参数。
                                                runOptions: '-Xms512M -Xmx512M',
                                                // 传给 Spring Boot 应用的启动参数。
                                                runArgs   : '--spring.profiles.active=test'
                                        ],
                                        // 就绪探针按配置顺序执行，任一失败即发布失败。
                                        readiness: [
                                                [
                                                        // 支持 TCP、HTTP、COMMAND。
                                                        type  : 'TCP',
                                                        config: [
                                                                // 服务监听端口。
                                                                port            : 8080,
                                                                // 两次探测的间隔，单位秒。
                                                                period          : 5,
                                                                // 最大失败次数；本例最多等待约 100 秒。
                                                                failureThreshold: 20
                                                        ]
                                                ]
                                        ]
                                ]
                        ],

                        notification     : [
                                // 是否发送交付通知。示例默认关闭。
                                // 通知密钥不要直接写入 Jenkinsfile，应使用 Jenkins Credentials。
                                enabled: false
                        ]
                ]
        ],

        // 覆盖层：Job 临时差异配置，优先级最高。
        // 例如可覆盖 primary.DELIVERY.targetEnvironment、runArgs 或发布节点。
        overrides: [
                DELIVERY: [:]
        ],

        // 本次执行身份，用于产物存储隔离和交付记录。
        execution: [
                // Jenkins BUILD_TAG 可区分 Job 和构建编号。
                id    : env.BUILD_TAG,
                // Jenkins 当前构建编号。
                number: env.BUILD_NUMBER
        ]
]

// deliverApplication 内部默认在 buildNode 节点构建；
// 部署阶段会自动切换到 deploy.nodes 指定的节点。
stage('deliver demo-java-api') {
    deliverApplication(deliveryConfig)
}

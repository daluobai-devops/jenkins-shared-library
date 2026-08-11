// 加载 Jenkins Shared Library。
// 测试共享库分支时可写成：@Library('jenkins-shared-library@test') _
@Library('jenkins-shared-library') _

// deliverApplication 静态 Web 发布示例。
// 请按实际环境替换应用名、源码仓库、分支、构建命令、发布节点和发布目录。
//
// 配置合并优先级（从低到高）：
// 内置 config/delivery-defaults.json < defaults < extension < primary < overrides
// 新入口只接受当前统一配置，不接受 DEPLOY_PIPELINE、SHARE_PARAM、CONFIG_EXTEND 等旧结构。
def deliveryConfig = [
        // 调用方默认覆盖层；共享库会先自动加载内置默认配置。没有覆盖项时可留空。
        defaults : [:],

        // 扩展层：可放置团队或项目级公共配置，优先级高于 defaults。
        // 不需要扩展配置时保留空 DELIVERY 即可。
        extension: [
                DELIVERY: [:]
        ],

        // 主配置层：描述这一个应用的完整交付流程。
        primary  : [
                DELIVERY: [
                        application      : [
                                // 应用唯一标识；同时用于生成目标目录名。
                                id  : 'demo-web',
                                // 静态前端应用固定填写 WEB。
                                type: 'WEB'
                        ],
                        // 目标环境名称，必填，例如 dev、test、staging、prod。
                        targetEnvironment: 'test',
                        // 新入口目前只支持 DEPLOY。
                        operationMode    : 'DEPLOY',

                        source           : [
                                // 源码仓库地址，支持 HTTPS 或 SSH。
                                repository   : 'https://gitee.com/log4j/pig-ui.git',
                                // 源码分支、Tag 或 Commit；预检阶段会解析并固定为 Commit SHA。
                                reference    : 'master',
                                // 仓库内构建目录，必须是安全的相对路径；仓库根目录填写 "."。
                                directory    : '.'
                                // 公开 HTTPS 仓库无需凭据；当前新入口不支持 HTTPS 私有仓库认证。
                        ],

                        stages           : [
                                build  : [
                                        // 是否执行构建阶段；启用存储或部署时必须为 true。
                                        enabled : true,
                                        // WEB 应用固定使用 NPM。
                                        strategy: 'NPM',
                                        artifact: [
                                                // 构建完成后的标准产物路径。
                                                // Web 发布只支持 .zip、.tar 或 .tar.gz。
                                                path    : 'package/app.zip',
                                                // 产物文件名，格式必须与 storage.config.archiveType 一致。
                                                fileName: 'app.zip'
                                        ],
                                        config  : [
                                                // 在 source.directory 中执行的前端构建命令。
                                                // 构建完成后必须产生 dist 目录。
                                                buildCMD        : 'npm install && npm run build',
                                                // NPM 构建容器镜像。
                                                dockerBuildImage: 'registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo/build-npm:10.16.0',
                                                // 是否跨构建缓存 node_modules，默认 true。
                                                cacheNodeModules: true
                                        ]
                                ],

                                storage: [
                                        // 是否存储构建产物。跨节点部署时建议使用 Jenkins Stash。
                                        enabled: true,
                                        target : [
                                                // 当前部署流程使用 JENKINS_STASH 在构建节点和发布节点间传递产物。
                                                type: 'JENKINS_STASH'
                                        ],
                                        config : [
                                                // Web 产物只支持 ZIP 或 TAR，必须与 artifact.fileName 一致。
                                                archiveType: 'ZIP',
                                                jenkinsStash: [
                                                        // 启用 Jenkins Stash。
                                                        enable: true
                                                ]
                                        ]
                                ],

                                deploy : [
                                        // 是否执行部署阶段。
                                        enabled : true,
                                        // 静态 Web 应用固定使用 WEB_STATIC。
                                        strategy: 'WEB_STATIC',
                                        // Jenkins 发布节点标签；标签必须能解析到在线节点。
                                        nodes   : ['NODE-DEMO'],
                                        config  : [
                                                // Web 应用发布根目录，必填。
                                                // 最终生效目录为：
                                                // /apps/application/projectGroup/web/demo-web/app
                                                pathRoot: '/apps/application/projectGroup/web/'
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
        // 例如可覆盖 primary.DELIVERY.targetEnvironment 或发布节点。
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
stage('deliver demo-web') {
    deliverApplication(deliveryConfig)
}

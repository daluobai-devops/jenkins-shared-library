def customConfig = [
        //公共参数
        "SHARE_PARAM"    : [
                //app 名称,如果没填则使用jenkins job名称。可不填
                "appName": "testWeb",
                //消息通知，可不填
                "message": [
                        //企业微信通知 可不填
                        "wecom" : [
                                //企业微信机器人token 必填
                                "key": "",
                                //是否发送完整的消息，如果为true只发送部署成功的消息。默认false
                                "fullMessage": false
                        ],
                        //飞书通知 可不填
                        "feishu": [
                                //飞书机器人token 必填
                                "token"      : "",
                                //是否发送完整的消息，如果为true只发送部署成功的消息。默认false
                                "fullMessage": false
                        ]
                ]
        ],
        //发布流程
        "DEPLOY_PIPELINE": [
                //构建
                "stepsBuildNpm": [
                        //是否激活,默认true
                        "enable": true,
                        //app git url 必填.
                        "gitUrl": "https://gitee.com/log4j/pig-ui.git",
                        //git 分支
                        "gitBranch": "master",
                        //构建命令 必填
                        "buildCMD"   : "npm install && npm run build",
                        //用来打包的镜像 可不填。默认10.16.0,选项(10.16.0,14.21.3,16)
                        "dockerBuildImage"   : "registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo/build-npm:10.16.0",
                        //使用缓存node_modules 可不填，默认true
                        "cacheNodeModules": true
                ],
                //存储
                "stepsStorage"  : [
                        //是否激活,默认true
                        "enable": true,
                        //构建产物类型 JAR,WAR,ZIP,TAR 必填
                        "archiveType":"TAR",
                        //存储构建产物，构建成功后可以在页面下载构建产物，默认false。
                        "archiveArtifacts"   : false,
                        //存储类型 jenkinsStash,dockerRegistry 必填
                        "jenkinsStash"  : [
                                //是否激活,默认false
                                "enable": true,
                        ],
                        //存档
                        "archiveArtifacts"  : false,
                        "dockerRegistry"  : [
                                //是否激活,默认false
                                "enable": false,
                        ],

                ],
                //发布
                "stepsJavaWebDeployToWebServer"  : [
                        //是否激活,默认true
                        "enable": true,
                        //服务发布根目录 必填
                        "pathRoot"  : "/apps/application/projectGroup/web/",
                        //服务发布服务label 必填
                        "labels"  : ["NODE-DEMO"]
                ]
        ],
        //默认配置
        "DEFAULT_CONFIG": [
                "docker": [
                        "registry": [
                                "domain": "docker.io"
                        ]
                ]
        ],
//        //继承配置
        "CONFIG_EXTEND"    : [
                //配置文件完整路径configType:path,支持URL，HOST_PATH，RESOURCES，默认RESOURCES. 必填.
                "configFullPath": "RESOURCES:config/extendConfigWeb.json",
        ]
]
@Library('jenkins-shared-library') _
deployWeb(customConfig)
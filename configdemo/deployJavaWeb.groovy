def customConfig = [
        //公共参数
        "SHARE_PARAM"    : [
                //app 名称,如果没填则使用jenkins job名称。可选
                "appName"      : "test",
                //钉钉 token
                "dingdingToken": "",
                //飞书 token
                "feishuToken"  : ""
        ],
        //发布流程
        "DEPLOY_PIPELINE": [
                //构建
                "stepsBuildMaven"   : [
                        //是否激活,默认true
                        "enable"          : true,
                        //app git url 必填.
                        "gitUrl"          : "https://gitee.com/wuzhaozhongguo/spring-data-examples.git",
                        //git 分支
                        "gitBranch"       : "main",
                        //子模块目录,如果要构建子模块填入子模块目录，如果没有不填 可选
                        "subModule"       : "web/example",
                        //是否跳过测试 可选
                        "skipTest"        : true,
                        //生命周期 必填
                        "lifecycle"       : "clean package",
                        //settings.xml文件路径，支持URL，HOST_PATH，RESOURCES 可选
                        "settingsFullPath": "RESOURCES:config/settings.xml",
                        //用来打包的镜像 可选
                        "dockerBuildImage": "registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo/build-maven:3.9.0-jdk17",
                        //激活的profile,maven -P参数 可选
                        "activeProfile"   : "dev"
                ],
                //存储
                "stepsStorage"      : [
                        //是否激活,默认true
                        "enable"        : true,
                        //构建产物类型 JAR,WAR,ZIP 必填
                        "archiveType"   : "JAR",
                        //存储类型 jenkinsStash,dockerRegistry 必填
                        "jenkinsStash"  : [
                                //是否激活,默认false
                                "enable": true,
                        ],
                        "dockerRegistry": [
                                //是否激活,默认false
                                "enable"       : false,
                                "imagePrefix"       : "registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo-app/",
                                "dockerfile": [
                                        "url" : "git@github.com:daluobai-devops/docker-library.git",
                                        "path": "package-javaweb/openjdk8"
                                ],
                        ],

                ],
                //发布
                "stepsJavaWebDeploy": [
                        //是否激活,默认true
                        "enable"    : true,
                        //服务发布路径 必填
                        "pathRoot"  : "/apps/application/",
                        //启动参数 [-options] 示例(-Dfile.encoding=UTF-8 -Xms128M -Xmx128M -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005)
                        "runOptions": "-Xms128M -Xmx128M",
                        //启动参数 [args...] 示例(-–spring.profiles.active=dev)
                        "runArgs"   : "-–spring.profiles.active=dev",
                        //服务发布服务label 必填
                        "labels"    : ["NODE-DEMO"],
                ]
        ],
        //默认配置
        "DEFAULT_CONFIG" : [
                "docker": [
                        "registry": [
                                "domain": "docker.io"
                        ]
                ]
        ],
//        //继承配置
        "CONFIG_EXTEND"  : [
                //配置文件完整路径configType:path,支持URL，HOST_PATH，RESOURCES，默认RESOURCES. 必填.
                "configFullPath": "RESOURCES:config/config.json",
        ]
]
@Library('jenkins-shared-library') _
deployJavaWeb(customConfig)
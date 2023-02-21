def customConfig = [
        //公共参数
        "SHARE_PARAM"    : [
                //app 名称
                "appName": "test",
                //钉钉 token
                "dingdingToken"  : "",
                //飞书 token
                "feishuToken"  : ""
        ],
        //发布流程
        "DEPLOY_PIPELINE": [
                //构建
                "stepsBuildMaven": [
                        //是否激活,默认true
                        "enable": true,
                        //app git url 必填.
                        "gitUrl": "git@github.com:spring-projects/spring-data-examples.git",
                        //git 分支
                        "gitBranch": "develop",
                        //子模块目录,如果要构建子模块填入子模块目录，如果没有不填 可选
                        "subModule"   : "web/example",
                        //是否跳过测试 可选
                        "skipTest"   : true,
                        //生命周期 必填
                        "lifecycle"   : "clean package",
                        //用来打包的镜像 可选
                        "dockerBootPackageImage"   : "wuzhaozhongguo/build-maven:3.8.5-jdk8",
                        //激活的profile,maven -P参数 可选
                        "activeProfile"   : "dev"
                ],
                //存储
                "stepsStorage"  : [
                        //是否激活,默认true
                        "enable": true,
                        //构建产物类型 JAR,WAR,ZIP 必填
                        "archiveType":"JAR",
                        //存储类型 jenkinsStash,dockerRegistry 必填
                        "jenkinsStash"  : [
                                //是否激活,默认false
                                "enable": true,
                        ],
                        "dockerRegistry"  : [
                                //是否激活,默认false
                                "enable": true,
                        ],

                ],
                //发布
                "stepsJavaWebDeploy"  : [
                        //是否激活,默认true
                        "enable": true,
                        //服务发布路径 必填
                        "pathRoot"  : "/apps/application/",
                        //启动参数 [-options] 示例(-Dfile.encoding=UTF-8 -Xms128M -Xmx128M -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005)
                        "runOptions"  : "-Xms128M -Xmx128M",
                        //启动参数 [args...] 示例(-–spring.profiles.active=dev)
                        "runArgs"  : "-–spring.profiles.active=dev",
                        //服务发布服务label 必填
                        "labels"  : ["VM-dodo-node1-192.168.1.33","VM-dodo-node2-192.168.1.32"],
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
//        "CONFIG_EXTEND"    : [
//                //配置文件读取方式，支持URL，HOST_PATH，RESOURCES，默认HOST_PATH, 必填.
//                "configType"        : "HOST_PATH",
//                //文件路径 必填.
//                "path": "/usr/local/workspace/config/jenkins-pipeline/jenkins-pipeline-config/extendConfig.json",
//        ]
]
@Library('jenkins-shared-library') _
deployJavaWeb(customConfig)
def customConfig = [
        //公共参数
        "SHARE_PARAM"    : [
                //app 名称,如果没填则使用jenkins job名称。可不填
                "appName"      : "test",
                //消息通知，可不填
                "message": [
                        //企业微信通知 可不填
                        "wecom": [
                                //企业微信机器人token 必填
                                "key": ""
                        ],
                        //飞书通知 可不填
                        "feishu": [
                                //飞书机器人token 必填
                                "token": ""
                        ]
                ]
        ],
        //发布流程
        "DEPLOY_PIPELINE": [
                //构建
                "stepsBuildMaven"   : [
                        //是否激活,默认true
                        "enable"          : true,
                        //app git url 必填.
                        "gitUrl"          : "git@gitee.com:renrenio/renren-security.git",
                        //git 分支
                        "gitBranch"       : "master",
                        //子模块目录,如果要构建子模块填入子模块目录。 可不填
                        "subModule"       : "renren-api/",
                        //是否跳过测试 可不填
                        "skipTest"        : true,
                        //生命周期 必填
                        "lifecycle"       : "clean package",
                        //settings.xml文件路径，支持URL，HOST_PATH，RESOURCES 可不填
                        "settingsFullPath": "RESOURCES:config/settings.xml",
                        //用来打包的镜像,可不填.默认3.8.5-jdk8,选项(3.6.1-jdk7,3.8.5-jdk8,3.9.0-jdk17)
                        "dockerBuildImage": "registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo/build-maven:3.8.5-jdk8",
                        //激活的profile,maven -P参数 可不填
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
                                "imagePrefix"       : "registry.cn-hangzhou.aliyuncs.com/wuzhaozhongguo-app",
                                //如果不填则使用appName
                                "imageName"       : "prod-xxx-api",
                                //如果不填则使用日期作为版本号
                                "imageVersion"       : "latest",
                                "dockerfile": [
                                        "url" : "git@github.com:daluobai-devops/docker-library.git",
                                        "gitBranch"       : "main",
                                        "path": "package-javaweb/openjdk8"
                                ],
                                "buildArgs":["runOptions":"-Xms128M -Xmx128M","runArgs":"--spring.profiles.active=dev"]
                        ],
                ],
                //发布
                "stepsDeploy": [
                        //是否激活,默认true
                        "enable"    : true,
                        //服务发布服务label 必填
                        "labels"    : ["NODE-DEMO"],
                        //发布
                        "stepsJavaWebDeployToService": [
                                //是否激活,默认true
                                "enable"    : true,
                                //java路径 可不填
                                "javaPath"  : "/usr/local/jdk/jdk17/bin/java",
                                //服务发布路径 必填
                                "pathRoot"  : "/apps/application/",
                                //JVM参数 [-options] 示例(-Dfile.encoding=UTF-8 -Xms128M -Xmx128M -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005)
                                "runOptions": "-Xms128M -Xmx128M",
                                //启动参数 [args...] 示例(-–spring.profiles.active=dev)
                                "runArgs"   : "--spring.profiles.active=dev",
                                //服务管理方式,支持systemctl,shell,默认systemctl，如果系统没有systemctl命令则使用shell方式
                                "manageBy"   : "shell",
                        ],
                        //发布到tomcat
                        "stepsTomcatDeploy":[
                                //是否激活,默认true
                                "enable"    : false,
                                //工作目录 必选。备份用
                                "tomcatHome"  : "/usr/local/tomcat",
                                //包发布路径 必填
                                "deployPath"  : "/usr/local/tomcat/webapps/",
                                //重启脚本,可不填
                                "command": "cd /usr/local/tomcat/bin/ && ./shutdown.sh && sleep 1000 && ./startup.sh"
                        ],

                        //就绪探针 可不填，检查服务是否启动成功，如果启动成功则认为服务发布成功，如果不填则不检查.探针类型，支持http,tcp,cmd.
                        "readinessProbe"          : [
                                //检查端口是否监听，如果监听则认为发布成功，如果不填则不检查 可不填
                                tcp: [
                                        //是否激活,默认true
                                        "enable"        : true,
                                        //探针端口
                                        "port"   : 8080
                                ],
                                //访问http地址，http状态码返回200则认为发布成功，如果不填则不检查 可不填
                                http: [
                                        //是否激活,默认true
                                        "enable"        : false,
                                        //探针路径， 必填
                                        "path"   : "/actuator/health",
                                        //探针端口， 必填
                                        "port"   : 8080,
                                        //探针超时时间，单位秒，默认5秒 可不填
                                        "timeout": 5
                                ],
                                //执行命令，以退出状态码判断是否成功 可不填
                                cmd: [
                                        //是否激活,默认true
                                        "enable"        : false,
                                        //探针命令，如果type为cmd则必填 必填
                                        "command": "curl -s -o /dev/null -w %{http_code} http://localhost:8080/actuator/health",
                                        //探针超时时间，单位秒，默认5秒 可不填
                                        "timeout": 5
                                ],
                                //探针间隔时间，单位秒，默认5秒 可不填
                                "period" : 5,
                                //探针失败次数，如果失败次数达到该值则认为发布失败，默认3次 可不填
                                "failureThreshold": 20
                        ],
                        //所有部署流程执行完成后运行的命令,centos(firewall-cmd --zone=public --add-port=8080/tcp --permanent && firewall-cmd --reload),ubuntu(ufw allow 8080/tcp && ufw reload)
                        "afterRunCMD":""
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
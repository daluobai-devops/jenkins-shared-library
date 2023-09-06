# jenkins-shared-library
Jenkins Pipeline Extending with Shared Libraries 
# 简介
jenkins pipeline 和拓展库介绍:[https://www.jenkins.io/doc/book/pipeline/]()
jenkins介绍和学习(Jenkins中文社区Rick):https://www.bilibili.com/video/BV1fp4y1r7Dd

加我微信:HELLO-WUZHAO

# 特性

- 可以通过pipeline做任何事情

- 通过封装pipeline，使用一个配置文件即可发布服务。

- 不依赖官方以外的 jenkins 插件。

- 使用 groovy 语法，非常容易上手
- 构建通过 docker，可根据不同工程选择不同的 docker 环境

# 安装 Jenkins

- 需要空白 linux 机器
- 安装 jdk11以上，推荐安装 jdk17,只用于跑 jenkins，创建 jdk 目录 mkdir -p /usr/local/jdk/jdk17下载 jdk17(https://download.oracle.com/java/17/archive/jdk-17.0.6_linux-x64_bin.tar.gz) 到这个目录解压,查看java 目录是否为/usr/local/jdk/jdk17/bin/java
- 安装LTS版本https://www.jenkins.io/download/

安装war启动的jenkins(这里会解压openjdk17到/usr/local/jdk/jdk17下)
```shell
curl -sSL https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/installJenkins.sh | bash
```

安装后操作
1. 访问 http://ip:9000/jenkins
2. 安装插件Docker、Docker Pipeline、Docker Api、Pipeline Utility Steps


# 配置Jenkins Pipeline

密钥管理

系统管理>凭据>系统>全局凭据>新建凭据

新建ssh-jenkins(用来免密登录服务器)和ssh-git(用来 clone 代码) 两个凭据，类型为SSH Username with private key，填入用户名和私钥

新建docker-secret(用来登录 docker 镜像)凭据类型为Username with password，如果没有就随便填一个

共享库配置

系统管理>系统配置> Global Pipeline Libraries

name:jenkins-shared-library

Default version:main

项目仓库:git@gitee.com:daluobai-devops/jenkins-shared-library.git

凭据:ssh-git

节点配置

配置示例:

- 名称：节点的名称，唯一值，可以用来设置到发布节点labels参数

- Number of executors：一般设置为 cpu 数量，不过我一般设置10
- 远程工作目录：/path/jenkins/
- 标签：用来标识节点，多个节点可用同一个标签，比如给节点添加上 docker标签表示这个节点上安装了 docker
- 启动方式：Launch agents via SSH
  - 主机：服务器的 ip
  - Credentials：ssh-jenkins
  - Host Key Verification Strategy：Manually trusted key Verification Strategy
- 节点属性>工具位置(用来设置jenkins agent执行用的jdk)
  - 名称:JDK17(这里在系统设置->全局工具配置.里面配置一个jdk)
  - 目录： /usr/local/jdk/jdk17/

构建节点配置:

- 在用来构建的节点添加buildNode标签，也可以直接把 master 节点作为构建节点

用来测试发布服务的节点：

- 名称：NODE-DEMO
- 环境变量:键:JAVA_HOME 值:/usr/local/jdk/jdk17/

4. 宿主机配置

   构建节点:安装 docker

# 发布第一个服务

新建job>流水线(pipeline)>配置
勾选:

不允许并发构建&&丢弃先前构建

使用 Groovy 沙盒

流水线>定义>Pipeline script填入configdemo/deployJavaWeb.groovy



未完待续。。。

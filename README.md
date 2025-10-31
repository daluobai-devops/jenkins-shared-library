# jenkins-shared-library
Jenkins Pipeline Extending with Shared Libraries 
# 一、简介
jenkins pipeline 和拓展库介绍:[https://www.jenkins.io/doc/book/pipeline/]()
jenkins介绍和学习(Jenkins中文社区Rick):https://www.bilibili.com/video/BV1fp4y1r7Dd

加我微信:HELLO-WUZHAO

# 二、特性

- 可以通过pipeline做任何事情

- 通过封装pipeline，使用一个配置文件即可发布服务。

- 不依赖官方以外的 jenkins 插件。

- 使用 groovy 语法，非常容易上手
- 构建通过 docker，可根据不同工程选择不同的 docker 环境

# 三、安装 Jenkins

- Centos7+
  - 安装
    ```shell
    curl -sSL https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/installJenkins.sh | bash
    ```
  - 卸载(注意：卸载会删除jenkins home目录，所有jenkins数据都会丢失)
    ```shell
    curl -sSL https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/uninstallJenkins.sh | bash
      ```
- Ubuntu
  - 安装
    ```shell
    curl -sSL https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/installJenkinsUbuntu.sh | bash
    ```
  - 卸载(注意：卸载会删除jenkins home目录，所有jenkins数据都会丢失)
    ```shell
    curl -sSL https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/uninstallJenkinsUbuntu.sh | bash
      ```
# 四、安装后操作

## 1. 访问 http://ip:9000/jenkins

## 2. 安装插件Docker、Docker Pipeline、Docker Api、Pipeline Utility Steps、
## 3. 配置Jenkins Pipeline

### a.密钥管理

    系统管理>凭据>系统>全局凭据>新建凭据
    
    新建ssh-jenkins(用来免密登录服务器)和ssh-git(用来 clone 代码) 两个凭据，类型为SSH Username with private key，填入用户名和私钥
    
    新建docker-secret(用来登录 docker 镜像)凭据类型为Username with password，如果用不到，账号密码就随便填一个

### b.共享库配置

    系统管理>系统配置> Global Pipeline Libraries(Global Trusted Pipeline Libraries)
    
    name:jenkins-shared-library
    
    Default version:main
    
    项目仓库:git@gitee.com:daluobai-devops/jenkins-shared-library.git
    
    凭据:无(如果是你公司自己的仓库，这里选ssh-git)

### c.节点配置

配置示例:

- 名称：节点的名称，唯一值，可以用来设置到发布节点labels参数

- Number of executors：一般设置为 cpu 数量，不过我一般设置10
- 远程工作目录：/path/jenkins/
- 标签：用来标识节点，多个节点可用同一个标签，比如给节点添加上 docker标签表示这个节点上安装了 docker
- 启动方式：Launch agents via SSH
  - 主机：服务器的 ip
  - Credentials：ssh-jenkins
  - Host Key Verification Strategy：Manually trusted key Verification Strategy
  - 高级
    - Java 路径：/usr/local/jdk/jdk17/bin/java

构建节点配置:

- 在用来构建的节点添加buildNode标签，也可以直接把 master 节点作为构建节点

用来测试发布服务的节点：

- 名称：NODE-DEMO

## 4. 宿主机配置

   构建节点:安装 docker

# 五、发布第一个服务（可选）

新建job>流水线(pipeline)>配置
勾选:

不允许并发构建&&丢弃先前构建

使用 Groovy 沙盒

流水线>定义>Pipeline script填入configdemo/deployJavaWeb.groovy

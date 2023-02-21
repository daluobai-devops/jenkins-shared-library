# jenkins-shared-library
Jenkins Pipeline Extending with Shared Libraries 
# 简介
jenkins pipeline 和拓展库介绍:[https://www.jenkins.io/doc/book/pipeline/]()

# 特点

- 通过封装pipeline，使用一个配置文件即可发布服务。

- 不依赖官方以外的 jenkins 插件。

- 使用 groovy 语法，对于 java 开发人员改造起来非常容易上手，使用了 hutool包简化代码
- 构建通过 docker，可根据不同工程选择不同的 docker 环境

# 安装 Jenkins

想怎么安装就怎么安装。

# 配置Jenkins Pipeline

1. 系统管理>凭据>系统>全局凭据>新建凭据

   新建ssh-jenkins(用来免密登录服务器)和ssh-git(用来 clone 代码) 两个凭据，类型为SSH Username with private key，填入用户名和私钥

   新建docker-secret(用来登录 docker 镜像)凭据类型为Username with password，如果没有就随便填一个

2. 系统管理>系统配置> Global Pipeline Libraries

   name:jenkins-shared-library

   Default version:main

   项目仓库:git@gitee.com:daluobai-devops/jenkins-shared-library.git

   凭据:ssh-git

   

   未完待续。。。

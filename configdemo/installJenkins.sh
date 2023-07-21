#! /bin/bash
yum install -y wget

# install jdk
JDK_DIR=/usr/local/jdk/jdk17
JDK_VERSION='openjdk-17.0.1_linux-x64_bin'
#删除已安装的
rm -rf ${JDK_DIR} || true
mkdir -p ${JDK_DIR}
cd ${JDK_DIR}
wget https://d6.injdk.cn/openjdk/openjdk/17/${JDK_VERSION}.tar.gz && tar -zxvf ${JDK_VERSION}.tar.gz && mv ./jdk-17.0.1/* ./ && rm -rf ./jdk-17.0.1

# install jenkins
mkdir -p "/usr/local/jenkins/"
wget -O /usr/local/jenkins/jenkins.war https://sg.mirror.servanamanaged.com/jenkins/war-stable/latest/jenkins.war
wget -O /etc/systemd/system/jenkins.service https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/jenkins.service
systemctl enable jenkins.service
systemctl start jenkins.service

firewall-cmd --zone=public --add-port=9000/tcp --permanent && firewall-cmd --reload
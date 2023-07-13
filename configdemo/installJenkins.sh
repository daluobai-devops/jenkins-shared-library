#! /bin/bash
yum install -y wget
mkdir -p "/usr/local/jenkins/"
wget -O /usr/local/jenkins/jenkins.war https://sg.mirror.servanamanaged.com/jenkins/war-stable/latest/jenkins.war
wget -O /etc/systemd/system/jenkins.service https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/jenkins.service
systemctl enable jenkins.service
systemctl start jenkins.service

firewall-cmd --zone=public --add-port=9000/tcp --permanent && firewall-cmd --reload
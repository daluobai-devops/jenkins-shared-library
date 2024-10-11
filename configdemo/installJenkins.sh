#! /bin/bash
yum install -y wget

sudo wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
sudo yum upgrade  -y
# Add required dependencies for the jenkins package
sudo yum install fontconfig java-17-openjdk  -y
sudo yum install jenkins  -y
sudo systemctl daemon-reload
systemctl enable jenkins.service
systemctl start jenkins.service

firewall-cmd --zone=public --add-port=8080/tcp --permanent && firewall-cmd --reload
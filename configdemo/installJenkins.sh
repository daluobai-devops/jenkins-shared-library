#! /bin/bash
# curl -sSL https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/installJenkins.sh | bash
yum install wget -y
sudo wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
sudo yum upgrade  -y
# Add required dependencies for the jenkins package
sudo yum install fontconfig java-21-openjdk -y
sudo yum install jenkins -y
sudo systemctl daemon-reload
systemctl enable jenkins.service
systemctl start jenkins.service
sed -i 's/^Environment=.*/Environment="JENKINS_PORT=9000"/' /lib/systemd/system/jenkins.service
firewall-cmd --zone=public --add-port=9000/tcp --permanent && firewall-cmd --reload
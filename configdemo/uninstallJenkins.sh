#! /bin/bash -e
# curl -sSL https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/uninstallJenkinsUbuntu.sh | bash
systemctl stop jenkins.service
sudo yum remove -y jenkins

sudo rm -rf /var/lib/jenkins /etc/sysconfig/jenkins /var/log/jenkins
sudo yum autoremove -y
sudo yum clean all

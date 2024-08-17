#! /bin/bash -e
# curl -sSL https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/uninstallJenkinsUbuntu.sh | bash
systemctl stop jenkins.service
apt-get remove --purge jenkins -y
rm -rf /var/lib/jenkins
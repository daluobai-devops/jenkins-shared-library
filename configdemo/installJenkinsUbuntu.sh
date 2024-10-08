#! /bin/bash -e
# curl -sSL https://cdn.jsdelivr.net/gh/daluobai-devops/jenkins-shared-library@master/configdemo/installJenkinsUbuntu.sh | bash
sudo apt-get update
sudo apt install fontconfig openjdk-17-jre -y
sudo java -version
sudo wget -O /usr/share/keyrings/jenkins-keyring.asc \
  https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc]" \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get install jenkins -y

#sed -i 's/^Group=.*/Group=root/' /lib/systemd/system/jenkins.service
#sed -i 's/^User=.*/User=root/' /lib/systemd/system/jenkins.service
sed -i 's/^Environment=.*/Environment="JENKINS_PORT=9000"/' /lib/systemd/system/jenkins.service
systemctl daemon-reload
systemctl start jenkins
systemctl enable jenkins

ufw allow 9000/tcp && ufw reload
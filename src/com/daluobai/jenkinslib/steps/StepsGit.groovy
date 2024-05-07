package com.daluobai.jenkinslib.steps
@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.lang.Assert
import com.cloudbees.groovy.cps.NonCPS/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class StepsGit implements Serializable {
    def steps

    StepsGit(steps) { this.steps = steps }

//    git@127.21.8.1:2200/test/test1.git
//    git@github.com:xxxxx/test.git
//    https://github.com/xxxxxx/test.git
//    http://127.21.8.1:80/test/test1.git

    /**
     * ssh-keyscan生成到known_hosts
     * @param url
     * @param path 路径，一般是~/.ssh/known_hosts
     * @return
     */
    @NonCPS
    def sshKeyscan(String gitUrl,String filePath) {
        def domainByUrl = this.getDomainByGitUrl(gitUrl)
        steps.echo "domainByUrl:${domainByUrl}"
        Assert.notBlank(domainByUrl,"链接为空")
        //获取到域名和端口
        def matcher = (domainByUrl =~ /^([a-zA-Z0-9.-]+)(?::([0-9]+))?/)
        if (matcher.matches()) {
            def host = matcher.group(1)
            def port = matcher.group(2)
            def portStr = port > 0 ? "-p ${port}" : ""
            steps.echo "-111111"
            File file = new File(filePath)

            steps.echo "00000"
            steps.echo "0000-1：${filePath}"
            if (!file.exists()) {
                steps.echo "11111"
                File parentDir = file.getParentFile();
                parentDir.mkdirs()
                file.createNewFile()
                steps.echo "222222"
            }
            steps.echo "33333"
            steps.sh "ssh-keyscan ${portStr} ${host} >> ${filePath}"
            steps.echo "44444"
        }else {
            steps.error "链接格式不正确"
        }
    }

    /**
     * 保存ssh-key到~/.ssh/.ssh目录
     * @param credentialsId
     * @return
     */
    def saveJenkinsSSHKey(String credentialsId,String path = '~/.ssh/'){
        Assert.notBlank(credentialsId,"credentialsId为空")
        steps.withCredentials([steps.sshUserPrivateKey(credentialsId: "${credentialsId}", keyFileVariable: 'SSH_KEY_PATH')]) {
            steps.sh "cat /etc/hostname && pwd && mkdir -p ${path} && chmod 700 ${path} && rm -f ${path}/id_rsa && cp \${SSH_KEY_PATH} ${path}/id_rsa || true && chmod 600 ${path}/id_rsa"
        }
    }

    /**
     * 从链接中获取域名
     * @param gitUrl
     * @return
     */
    @NonCPS
    def getDomainByGitUrl(String gitUrl){
        def pattern = /(?<=@|:\/\/)([^\/:]+)/
        def matcher = (gitUrl =~ pattern)
        def domain = ""
        if (matcher.find()) {
            domain = matcher.group(0)
        }
        return domain
    }
}

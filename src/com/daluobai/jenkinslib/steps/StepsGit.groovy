package com.daluobai.jenkinslib.steps

@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.lang.Assert
import com.cloudbees.groovy.cps.NonCPS

/**
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
//    @NonCPS
    def sshKeyscan(String gitUrl, String filePath) {
        def domainByUrl = this.getDomainByGitUrl(gitUrl)
        steps.echo "domainByUrl:${domainByUrl}"
        Assert.notBlank(domainByUrl, "链接为空")
        def domainHostAndPortMap = this.getDomainHostAndPort(domainByUrl)
        //获取到域名和端口
        if (domainHostAndPortMap != null) {
            def host = domainHostAndPortMap.host
            def portStr = domainHostAndPortMap.portStr
            steps.sh """
                        #! /bin/sh -e
                        mkdir -p \$(dirname $filePath) && touch ${filePath}
                        chmod 700 \$(dirname $filePath) && chmod 600 ${filePath}
                        ssh-keyscan ${portStr} ${host} >> ${filePath}
                    """
        } else {
            steps.error "链接格式不正确"
        }
    }

    @NonCPS
    def getDomainHostAndPort(String domain) {
        //获取到域名和端口
        def matcher = (domain =~ /^([a-zA-Z0-9.-]+)(?::([0-9]+))?/)
        if (matcher.matches()) {
            def host = matcher.group(1)
            def port = matcher.group(2)
            steps.echo "===xxxx:${port}---${port}"
            def portStr = port > 0 ? "-p ${port}" : ""
            return ["portStr": portStr, "host": host]
        } else {
            return null
        }
    }

    /**
     * 保存ssh-key到~/.ssh/.ssh目录
     * @param credentialsId
     * @return
     */
    def saveJenkinsSSHKey(String credentialsId, String path = '~/.ssh') {
        Assert.notBlank(credentialsId, "credentialsId为空")
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
    def getDomainByGitUrl(String gitUrl) {
        def pattern = /(?<=@|:\/\/)([^\/:]+)/
        def matcher = (gitUrl =~ pattern)
        def domain = ""
        if (matcher.find()) {
            domain = matcher.group(0)
        }
        return domain
    }

    /**
     * 同步git仓库
     * @param orgGitUrl
     * @param orgCredentialsId
     * @param targetGitUrl
     * @param targetCredentialsId
     * @return
     */
    def syncGit2Git(String orgGitUrl, String orgCredentialsId, String targetGitUrl, String targetCredentialsId) {
        def pathBase = "${steps.env.WORKSPACE}"
        //docker-构建产物目录
        def pathPackage = "package"
        //docker-代码目录
        def pathCode = "code"
        //存放临时sshkey的目录
        def pathSSHKey = "sshkey"
        //从 jenkins 凭据管理中获取密钥文件路径并且拷贝到工作目录下的ssh-git目录，后面clone的时候指定密钥为这个
        this.saveJenkinsSSHKey(orgCredentialsId, "${steps.env.WORKSPACE}/${pathSSHKey}/ssh-org-git")
        this.saveJenkinsSSHKey(targetCredentialsId, "${steps.env.WORKSPACE}/${pathSSHKey}/ssh-target-git")
        //生成known_hosts
        this.sshKeyscan("${orgGitUrl}", "~/.ssh/known_hosts")
        this.sshKeyscan("${targetGitUrl}", "~/.ssh/known_hosts")
        steps.sh """
                        #! /bin/sh -e
                        mkdir -p ${pathBase}/${pathPackage} && mkdir -p ${pathBase}/${pathCode}
                        cd ${pathBase}/${pathCode}
                        git config --global http.version HTTP/1.1
                        GIT_SSH_COMMAND='ssh -i ${steps.env.WORKSPACE}/${pathSSHKey}/ssh-org-git/id_rsa' git clone ${orgGitUrl} --quiet
                        mv ${pathBase}/${pathCode}/\$(ls -A1 ${pathBase}/${pathCode}/) ${pathBase}/${pathCode}/${pathCode}
                        cd ${pathBase}/${pathCode}/${pathCode}
                        git log --pretty=format:"%h -%an,%ar : %s" -1
                        git config core.ignorecase false
                        ls -al ${pathBase}/${pathCode}/${pathCode}/
                    """
    }
}

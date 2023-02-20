package com.daluobai.jenkinslib.steps
@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.lang.Assert
import java.net.URL
class StepsGit implements Serializable {
    def steps

    StepsGit(steps) { this.steps = steps }

//    git@172.21.8.95:2200/test/test1.git
//    git@github.com:xxxxx/test.git
//    https://github.com/xxxxxx/test.git
//    http://172.21.8.95:80/test/test1.git

    /**
     * ssh-keyscan生成到known_hosts
     * @param url
     * @param path 路径，一般是~/.ssh/known_hosts
     * @return
     */
    @NonCPS
    def sshKeyscan(String gitUrl,String path) {
        def domainByUrl = this.getDomainByGitUrl(gitUrl)
        steps.echo "domainByUrl:${domainByUrl}"
        Assert.notBlank(domainByUrl,"链接格式不正确")
        //获取到域名和端口
        def matcher = (domainByUrl =~ /^([a-zA-Z0-9.-]+)(?::([0-9]+))?/)
        if (matcher.matches()) {
            def host = matcher.group(1)
            def port = matcher.group(2)
            def portStr = port > 0 ? "-p ${port}" : ""
            steps.sh "ssh-keyscan ${portStr} ${host} >> ${path}"
        }else {
            steps.error "链接格式不正确"
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

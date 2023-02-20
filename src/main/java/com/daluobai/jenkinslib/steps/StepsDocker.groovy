package com.daluobai.jenkinslib.steps
//@Grab('cn.hutool:hutool-all:5.8.11')

import cn.hutool.core.lang.Assert
class StepsDocker implements Serializable {
    def steps

    StepsDocker(steps) { this.steps = steps }

    def login(String registry,String userName,String password) {
        steps.echo("StepsDocker1:${userName} -p ${password} ${registry}")
        steps.sh "docker login -u ${userName} -p ${password} ${registry}"
        steps.echo("StepsDocker2")
    }

}

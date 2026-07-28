package com.daluobai.jenkinslib.steps

import com.daluobai.jenkinslib.utils.DateUtils
import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.ObjUtils
import com.daluobai.jenkinslib.utils.StrUtils
import com.daluobai.jenkinslib.constant.GlobalShare
import com.daluobai.jenkinslib.utils.TemplateUtils
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class StepsWeb implements Serializable {
    def steps

    StepsWeb(steps) { this.steps = steps }

    /*******************初始化全局对象 开始*****************/
    def stepsJenkins = new StepsJenkins(steps)
    /*******************初始化全局对象 结束*****************/

    //发布
    def deploy(Map parameterMap) {
        return deploy(parameterMap, steps.globalParameterMap as Map)
    }

    // 当前交付 implementation 显式传入有效配置；单参数入口仅供旧调用兼容。
    def deploy(Map parameterMap, Map effectiveConfig) {
        steps.echo '开始部署Web静态资源'
        AssertUtils.notNull(parameterMap, '参数为空')
        AssertUtils.notEmpty(parameterMap, '参数为空')
        def labels = parameterMap.labels
        String pathRoot = parameterMap.pathRoot?.toString()
        Map globalParameterMap = effectiveConfig
        String appName = globalParameterMap?.SHARE_PARAM?.appName?.toString()
        String archiveName = globalParameterMap?.SHARE_PARAM?.archiveName?.toString()
        Map configStepsStorage = globalParameterMap?.DEPLOY_PIPELINE?.stepsStorage as Map
        String archiveType = configStepsStorage?.archiveType?.toString()

        AssertUtils.notEmpty(labels, 'labels为空')
        AssertUtils.notBlank(pathRoot, 'pathRoot为空')
        AssertUtils.notBlank(appName, 'appName为空')
        AssertUtils.notBlank(archiveName, 'archiveName为空')
        AssertUtils.isTrue(archiveType in ['ZIP', 'TAR'], 'archiveType仅支持ZIP或TAR')

        String archiveSuffix = StrUtils.subAfter(archiveName, '.', true)
        String backAppName = "app-${DateUtils.format(new Date(), 'yyyyMMddHHmmss')}.${archiveSuffix}"
        String appRoot = "${pathRoot}/${appName}"

        labels.each { label ->
            steps.echo "发布第一个标签:${label}"
            def nodeDeployNodeList = stepsJenkins.getNodeByLabel(label)
            steps.echo "获取到发布节点:${nodeDeployNodeList}"
            if (ObjUtils.isEmpty(nodeDeployNodeList)) {
                steps.error '没有可用的发布节点'
            }
            nodeDeployNodeList.each { nodeDeployNode ->
                steps.echo "开始发布:${nodeDeployNode}"
                steps.node(nodeDeployNode) {
                    steps.unstash('appPackage')
                    steps.sh 'hostname'
                    steps.sh 'ls -l package'
                    steps.sh "mkdir -p '${appRoot}' '${appRoot}/backup'"
                    steps.sh "mv -f '${appRoot}/${archiveName}' '${appRoot}/backup/${backAppName}' || true"
                    steps.dir("${appRoot}/backup/") {
                        steps.sh 'find . -mtime +3 -delete'
                    }
                    steps.sh "cp 'package/${archiveName}' '${appRoot}/${archiveName}'"

                    steps.dir("${appRoot}/") {
                        steps.sh '''
set -eu
rm -rf .app-next
if [ -e .app-previous ]; then
  if [ ! -e app ]; then
    echo "检测到未恢复的回滚目录: $(pwd)/.app-previous" >&2
    exit 1
  fi
  rm -rf .app-previous
fi
mkdir -p .app-next
'''
                        if (archiveType == 'ZIP') {
                            steps.sh "unzip '${archiveName}' -d .app-next/"
                        } else {
                            steps.sh "tar -zxvf '${archiveName}' -C .app-next/"
                        }
                        steps.sh '''
set -eu
if [ -z "$(find .app-next -mindepth 1 -print -quit)" ]; then
  echo 'Web发布暂存目录为空: .app-next' >&2
  exit 1
fi
'''
                        steps.sh '''
set -eu
had_previous=0
if [ -e app ]; then
  mv app .app-previous
  had_previous=1
fi
if mv .app-next app; then
  rm -rf .app-previous
else
  switch_status=$?
  if [ "$had_previous" -eq 1 ]; then
    if ! mv .app-previous app; then
      echo "发布切换失败，自动恢复也失败；旧版本保留在 $(pwd)/.app-previous" >&2
    fi
  fi
  exit "$switch_status"
fi
'''
                        steps.sh 'ls -l app'
                    }
                }
            }
        }
    }

}

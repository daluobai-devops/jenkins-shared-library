package com.daluobai.jenkinslib.utils

import cn.hutool.core.io.FileUtil
@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.core.lang.Assert
import cn.hutool.core.util.StrUtil
import cn.hutool.http.HttpUtil
import com.daluobai.jenkinslib.api.FeishuApi
import com.daluobai.jenkinslib.api.WecomApi
import com.daluobai.jenkinslib.constant.EFileReadType
import com.daluobai.jenkinslib.steps.StepsJenkins

import java.nio.charset.Charset
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class MessageUtils implements Serializable {

    def steps

    MessageUtils(steps) { this.steps = steps }

    /*******************初始化全局对象 开始*****************/
    def feishuApi = new FeishuApi(steps)
    def wecomApi = new WecomApi(steps)
    /*******************初始化全局对象 结束*****************/

    /**
     * 从完整路径读取文件
     * @param fileFullPath
     *
     * @return
     */
    def sendMessage(messageConfig,String title,String content) {
        Assert.notEmpty(messageConfig, "messageConfig为空");
        Assert.notBlank(content,"content为空")
        //遍历messageConfig
        messageConfig.each { key, value ->
            if (key == "wecom" && StrUtil.isNotBlank(value.key)) {
                //企业微信通知
                wecomApi.sendMsg(value.key, content)
            }
            if (key == "feishu" && StrUtil.isNotBlank(value.token)) {
                //飞书通知
                feishuApi.sendMsg(value.token,title, content)
            }
        }
        return true
    }

}
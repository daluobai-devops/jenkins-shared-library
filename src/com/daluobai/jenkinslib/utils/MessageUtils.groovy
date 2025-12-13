package com.daluobai.jenkinslib.utils

import com.daluobai.jenkinslib.utils.ObjUtils
import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.StrUtils
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
     *
     * @param fileFullPath
     * @param simpleMessage 这条消息是不是精简消息,精简消息每次都会发
     *
     * @return
     */
    def sendMessage(boolean simpleMessage = true, messageConfig, String title, String content) {
        if (ObjUtils.isEmpty(messageConfig)) {
            return false
        }
        AssertUtils.notBlank(content, "content为空")
        //遍历messageConfig
        messageConfig.each { key, value ->
            if (key == "wecom" && StrUtils.isNotBlank(value.key)) {
                if (value.fullMessage || simpleMessage) {
                    //企业微信通知
                    wecomApi.sendMsg(value.key, content)
                }

            }
            if (key == "feishu" && StrUtils.isNotBlank(value.token)) {
                if (value.fullMessage || simpleMessage) {
                    //飞书通知
                    feishuApi.sendMsg(value.token, title, content)
                }
            }
        }
        return true
    }

}
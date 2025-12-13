package com.daluobai.jenkinslib.api

import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.StrUtils
import com.daluobai.jenkinslib.utils.HttpUtils
import com.daluobai.jenkinslib.utils.JsonUtils

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 企业微信API
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class WecomApi implements Serializable {
    def steps

    WecomApi(steps) { this.steps = steps }

    /**
     * 发送消息
     * @param chatToken webhook key
     * @param text 消息内容
     * @return
     */
    def sendMsg(String chatToken,String text) {
        AssertUtils.notBlank(chatToken,"chatToken空的");
        AssertUtils.notBlank(text,"text空的");

        def paramMap = [
            "msgtype": "text",
            "text": [
                "content": text
            ]
        ]

        String paramsStr = JsonUtils.toJsonStr(paramMap);
        String response = ""
        try {
            response = HttpUtils.HttpRequest.post("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key="+chatToken)
                    .contentType("application/json;charset=utf-8").body(paramsStr).execute().body()
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StrUtils.isBlank(response)){
            return false
        }
        def responseJson = JsonUtils.parseObj(response)

        Boolean ok = responseJson.getBool("ok")
        return !(ok == null || !ok)

    }
}
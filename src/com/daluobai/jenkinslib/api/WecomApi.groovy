package com.daluobai.jenkinslib.api

@Grab('cn.hutool:hutool-all:5.8.42')
import cn.hutool.core.lang.Assert
import cn.hutool.core.util.StrUtil
import cn.hutool.http.HttpRequest
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil

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
        Assert.notBlank(chatToken,"chatToken空的");
        Assert.notBlank(text,"text空的");

        def paramMap = [
            "msgtype": "text",
            "text": [
                "content": text
            ]
        ]

        String paramsStr = JSONUtil.toJsonStr(paramMap);
        String response = ""
        try {
            response = HttpRequest.post("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key="+chatToken)
                    .contentType("application/json;charset=utf-8").body(paramsStr).execute().body()
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StrUtil.isBlank(response)){
            return false
        }
        JSONObject responseJson = JSONUtil.parseObj(response)

        Boolean ok = responseJson.getBool("ok")
        return !(ok == null || !ok)

    }
}
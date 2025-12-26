package com.daluobai.jenkinslib.api

import com.daluobai.jenkinslib.utils.HttpUtils
import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.JsonUtils
import com.daluobai.jenkinslib.utils.StrUtils
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class DingDingApi implements Serializable {
    def steps

    DingDingApi(steps) { this.steps = steps }

    /**
     * 发送钉钉消息
     * @param access_token token
     * @param text 消息内容
     * @return
     */
    def sendMsg(String accessToken,String text) {
        AssertUtils.notBlank(accessToken,"accessToken空的");
        AssertUtils.notBlank(text,"text空的");
        Map<String,Object> params = new HashMap<>();
        params.put("msgtype","text");
        Map<String,Object> paramsText = new HashMap<>();
        paramsText.put("content",text);
        params.put("text",paramsText);

        String paramsStr = JsonUtils.toJsonStr(params);
        String response = "";
        try {
            response = HttpUtils.postJson("https://oapi.dingtalk.com/robot/send?access_token="+accessToken,
                    paramsStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StrUtils.isBlank(response)){
            return false
        }
        def responseJson = JsonUtils.parseObj(response);

        def errcode = responseJson.get("errcode") as Integer;
        return errcode != null && errcode == 0;

    }
}
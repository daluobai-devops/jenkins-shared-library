package com.daluobai.jenkinslib.api

@Grab('cn.hutool:hutool-all:5.8.42')
import cn.hutool.http.HttpUtil
import cn.hutool.core.lang.Assert
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import cn.hutool.core.util.StrUtil
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
        Assert.notBlank(accessToken,"accessToken空的");
        Assert.notBlank(text,"text空的");
        Map<String,Object> params = new HashMap<>();
        params.put("msgtype","text");
        Map<String,Object> paramsText = new HashMap<>();
        paramsText.put("content",text);
        params.put("text",paramsText);

        String paramsStr = JSONUtil.toJsonStr(params);
        String response = "";
        try {
            response = HttpUtil.post("https://oapi.dingtalk.com/robot/send?access_token="+accessToken,
                    paramsStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StrUtil.isBlank(response)){
            return false
        }
        cn.hutool.json.JSONObject responseJson = JSONUtil.parseObj(response);

        def errcode = responseJson.getInt("errcode")
        return !(errcode == null || errcode != 0);

    }
}
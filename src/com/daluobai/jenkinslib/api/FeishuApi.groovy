package com.daluobai.jenkinslib.api

//@Grab('cn.hutool:hutool-all:5.8.11')
import cn.hutool.http.HttpUtil
import cn.hutool.core.lang.Assert
import cn.hutool.json.JSONObject
import cn.hutool.json.JSONUtil
import cn.hutool.core.util.StrUtil

class FeishuApi implements Serializable {
    def steps

    FeishuApi(steps) { this.steps = steps }

    /**
     * 发送飞书消息
     * @param chatToken 群组ID
     * @param msg 消息内容
     * @return
     */
    def sendMsg(String chatToken,String title,String text) {
        Assert.notBlank(chatToken,"chatToken空的");
        Assert.notBlank(title,"title空的");
        Assert.notBlank(text,"text空的");

        Map<String,Object> params = new HashMap<>();
        params.put("title",title);
        params.put("text",text);

        String paramsStr = JSONUtil.toJsonStr(params);
        String response = "";
        try {
            response = HttpUtil.post("https://open.feishu.cn/open-apis/bot/hook/"+chatToken,
                    paramsStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StrUtil.isBlank(response)){
            return false
        }
        cn.hutool.json.JSONObject responseJson = JSONUtil.parseObj(response);

        Boolean ok = responseJson.getBool("ok");
        return !(ok == null || !ok);

    }
}
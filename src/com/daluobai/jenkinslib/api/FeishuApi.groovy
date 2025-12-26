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
        AssertUtils.notBlank(chatToken,"chatToken空的");
        AssertUtils.notBlank(title,"title空的");
        AssertUtils.notBlank(text,"text空的");

        Map<String,Object> params = new HashMap<>();
        params.put("title",title);
        params.put("text",text);

        String paramsStr = JsonUtils.toJsonStr(params);
        String response = "";
        try {
            response = HttpUtils.postJson("https://open.feishu.cn/open-apis/bot/hook/"+chatToken,
                    paramsStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StrUtils.isBlank(response)){
            return false
        }
        def responseJson = JsonUtils.parseObj(response);
        this.steps.echo "飞书发送消息结果："+response;

        Boolean ok = responseJson.getBool("ok");
        return !(ok == null || !ok);

    }
}
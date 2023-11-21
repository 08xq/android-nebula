package com.mpaas.demo.utils.nui;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class AsrResultUtil {

    public static String getResult(String asrResult){
        JSONObject asrJSONObject = JSON.parseObject(asrResult);

        return asrJSONObject.getJSONObject("payload").getString("result");
    }
}

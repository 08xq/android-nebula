package com.mpaas.demo.utils;

import com.alibaba.fastjson.JSONObject;

public class JSONObjectUtil {

    public static JSONObject getSuccessResult(JSONObject data){
        return getResultJsonObject(0,"success",data);
    }

    public static JSONObject getSuccessResult(String data){
        return getResultJsonObject(0,"success",data);
    }

    public static JSONObject getFailedResult(JSONObject data){
        return getResultJsonObject(-1,"failed",data);
    }

    public static JSONObject getFailedResult(String msg, JSONObject data){
        return getResultJsonObject(-1,msg,data);
    }

    public static JSONObject getResultJsonObject(int code,String msg, JSONObject data){
        JSONObject result =  new JSONObject();
        result.put("code", code);
        result.put("data", data);
        result.put("msg", msg);
        return result;
    }

    public static JSONObject getResultJsonObject(int code,String msg, String data){
        JSONObject result =  new JSONObject();
        result.put("code", code);
        result.put("data", data);
        result.put("msg", msg);
        return result;
    }
}

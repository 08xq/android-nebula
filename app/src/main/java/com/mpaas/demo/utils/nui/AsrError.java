package com.mpaas.demo.utils.nui;

//识别错误
public class AsrError {

    public int errorCode;
    public String errorResult;

    public AsrError(int resultCode, String asrResult) {
        this.errorCode = resultCode;
        this.errorResult = asrResult;
    }

    @Override
    public String toString() {
        return "{" +
                "errorCode=" + errorCode +
                ", errorResult='" + errorResult + '\'' +
                '}';
    }
}

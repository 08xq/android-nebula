package com.mpaas.demo.utils.nui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.util.Log;


import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.idst.nui.AsrResult;
import com.alibaba.idst.nui.CommonUtils;
import com.alibaba.idst.nui.Constants;
import com.alibaba.idst.nui.INativeNuiCallback;
import com.alibaba.idst.nui.KwsResult;
import com.alibaba.idst.nui.NativeNui;
import com.alipay.mobile.h5container.api.H5BridgeContext;
//import com.boka.brokenai.utils.AudioUtil;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.mpaas.demo.MyApplication;
import com.mpaas.demo.utils.JSONObjectUtil;
import com.mpaas.demo.utils.file.FileUtils;

import java.io.File;
import java.util.List;

//阿里云语音utils
public class NativeNuiUtils {

    private final String TAG = "NativeNuiUtils";

    private boolean mInit = false;
    private AudioRecord mAudioRecorder;
    public final static int SAMPLE_RATE = 16000;
    final static int WAVE_FRAM_SIZE = 20 * 2 * 1 * 16000 / 1000; //20ms audio for 16k/16bit/mono

    private final String debugDir = "asr_debug";
    private String appCachePath = null;
    private String wavFileName = null;

    private static NativeNuiUtils instance = null;

    private AsrError asrError;

    private StringBuilder asrResultBuilder = null;

    public static synchronized NativeNuiUtils getInstance() {
        if (instance == null) {
            instance = new NativeNuiUtils();
        }
        return instance;
    }

    private String getInitParams(String deviceId, String appKey, String token, String workpath, String debugpath) {
        String str = "";
        //获取token方式：

        JSONObject object = new JSONObject();

        //账号和项目创建
        //  ak_id ak_secret app_key如何获得,请查看https://help.aliyun.com/document_detail/72138.html
        object.put("app_key", appKey); // 必填

        //方法1：
        //  首先ak_id ak_secret app_key如何获得,请查看https://help.aliyun.com/document_detail/72138.html
        //  然后请看 https://help.aliyun.com/document_detail/466615.html 使用其中方案一获取临时凭证
        //  此方案简介: 远端服务器运行STS生成具有有效时限的临时凭证, 下发给移动端进行使用, 保证账号信息ak_id和ak_secret不被泄露
        //  获得Token方法(运行在APP服务端): https://help.aliyun.com/document_detail/450255.html?spm=a2c4g.72153.0.0.79176297EyBj4k
        object.put("token", token); // 必填

        //方法2：
        //  STS获取临时凭证方法暂不支持

        object.put("device_id", deviceId); // 必填, 推荐填入具有唯一性的id, 方便定位问题
//        object.put("url", "wss://nls-gateway.cn-shanghai.aliyuncs.com:443/ws/v1"); // 默认
        object.put("url", "wss://nls-gateway-cn-shanghai.aliyuncs.com/ws/v1");
        object.put("workspace", workpath); // 必填, 且需要有读写权限
        object.put("save_wav","true");
        //debug目录，当初始化SDK时的save_log参数取值为true时，该目录用于保存中间音频文件。
        object.put("debug_path", debugpath);


        // FullMix = 0   // 选用此模式开启本地功能并需要进行鉴权注册
        // FullCloud = 1
        // FullLocal = 2 // 选用此模式开启本地功能并需要进行鉴权注册
        // AsrMix = 3    // 选用此模式开启本地功能并需要进行鉴权注册
        // AsrCloud = 4
        // AsrLocal = 5  // 选用此模式开启本地功能并需要进行鉴权注册
        object.put("service_mode", Constants.ModeFullCloud); // 必填
        str = object.toString();

        Log.i("", "InsideUserContext:" + str);
        return str;
    }

    public String getDialogParams(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("service_type",4);
        return jsonObject.toString();
    }

    public void initNui(Context context){
        String appKey = "Evv1sLorgzbE6Kxk";
        String token = "";
        String deviceId = "ZOidHJOn/DkDAKxM7roiPxOb";

        initAndCheckPermission(appKey,token,deviceId,null,context);
    }
    public void initAndCheckPermission(final String appkey, final String token, final String deviceId, final JSONObject nlsConfig, final Context context){
        //1检查录音权限
//        Context context = bridgeContext.getActivity();
        boolean audioPermission = XXPermissions.isGranted(context, Manifest.permission.RECORD_AUDIO);
        if(audioPermission){
            //开始初始化
//            init(appkey,token,deviceId,nlsConfig,bridgeContext);

            init(appkey,token,deviceId,null,context);
            return;
        }

        //没有权限，先申请权限
        XXPermissions.with(context).permission(Permission.RECORD_AUDIO).request(new OnPermissionCallback() {
            @Override
            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                if (!allGranted){
                    //权限没有申请成功,返回错误，结束本次调用
                    try{
                        JSONObject failed = JSONObjectUtil.getFailedResult("录音权限申请失败",new JSONObject());
//                        bridgeContext.sendBridgeResult(failed);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return;
                }
                //开始调用录音
//                init(appkey,token,deviceId,nlsConfig,bridgeContext);

                init(appkey,token,deviceId,null,context);
            }

            @Override
            public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                JSONObject failed = JSONObjectUtil.getFailedResult("录音权限申请失败,请手动授予录音权限",new JSONObject());
//                bridgeContext.sendBridgeResult(failed);
            }
        });
    }

    private void init(String appkey, String token, String deviceId, JSONObject nlsConfig,Context bridgeContext){

        appCachePath = MyApplication.application.getCacheDir().getAbsolutePath();
        String debugPath = appCachePath + File.separator + debugDir;
        //初始化文件目录
        FileUtils.createOrExistsDir(debugPath);

        //主动调用将SDK中的相关配置加载到运行目录
        if (CommonUtils.copyAssetsData(bridgeContext)) {
            Log.i(TAG, "copy assets data done");
        } else {
            Log.e(TAG, "copy assets failed");
            JSONObject resultObject = JSONObjectUtil.getFailedResult("复制配置文件失败",new JSONObject());
//            bridgeContext.sendBridgeResult(resultObject);
            return;
        }

        String asset_path = CommonUtils.getModelPath(bridgeContext);
        String workSpace = asset_path;//workSpace 一定要用这个，否则初始化失败

        initRecode();
        //初始化参数
        String params = getInitParams(deviceId,appkey,token,workSpace,debugPath);

        INativeNuiCallback callback = new INativeNuiCallback() {
            @Override
            public void onNuiEventCallback(Constants.NuiEvent event, int resultCode, int i1, KwsResult kwsResult, AsrResult asrResult) {
                Log.i(TAG, "event=" + event);
                if (event == Constants.NuiEvent.EVENT_WUW_TRUSTED) {
                    printString("kws:" + kwsResult.kws);
                } else if (event == Constants.NuiEvent.EVENT_ASR_RESULT) {
                    //语音识别最终结果
                    printString("EVENT_ASR_RESULT:" + asrResult.asrResult);
                } else if (event == Constants.NuiEvent.EVENT_ASR_PARTIAL_RESULT) {
                    //语音识别中间结果
                    //若enable_intermediate_result设置为true，SDK会持续多次通过onNuiEventCallback回调上报EVENT_ASR_PARTIAL_RESULT事件
                    if(asrResult != null){
                        asrParse(asrResult.asrResult);
                        printString("EVENT_ASR_PARTIAL_RESULT:" + asrResult.asrResult);
                    }
                } else if (event == Constants.NuiEvent.EVENT_ASR_ERROR) {
                    asrError = new AsrError(resultCode,asrResult.asrResult);
                    printString("EVENT_ASR_ERROR:" + asrResult.asrResult);
                } else if (event == Constants.NuiEvent.EVENT_DIALOG_EX) { /* unused */
                    printString("EVENT_DIALOG_EX:" + asrResult.asrResult);
                    //需要在这里修改文件名并移除缓存
                }else if(event == Constants.NuiEvent.EVENT_SENTENCE_END){
                    printString("EVENT_SENTENCE_END:" + asrResult.asrResult);
                }else if(event == Constants.NuiEvent.EVENT_TRANSCRIBER_COMPLETE){
                    if(asrResult != null){
                        printString("EVENT_TRANSCRIBER_COMPLETE:" + asrResult.asrResult);
                    }

                }
            }

            @Override
            public int onNuiNeedAudioData(byte[] buffer, int len) {
                int ret = 0;
                if (mAudioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "audio recorder not init");
                    return -1;
                }
                ret = mAudioRecorder.read(buffer, 0, len);
                return ret;
            }

            @Override
            public void onNuiAudioStateChanged(Constants.AudioState state) {
                Log.i(TAG, "onNuiAudioStateChanged");
                if (state == Constants.AudioState.STATE_OPEN) {
                    Log.i(TAG, "audio recorder start");
                    mAudioRecorder.startRecording();
                    Log.i(TAG, "audio recorder start done");
                } else if (state == Constants.AudioState.STATE_CLOSE) {
                    Log.i(TAG, "audio recorder close");
                    mAudioRecorder.release();
                } else if (state == Constants.AudioState.STATE_PAUSE) {
                    Log.i(TAG, "audio recorder pause");
                    mAudioRecorder.stop();
                }
            }

            @Override
            public void onNuiAudioRMSChanged(float val) {
                Log.i(TAG, "onNuiAudioRMSChanged vol " + val);
            }

            @Override
            public void onNuiVprEventCallback(Constants.NuiVprEvent event) {
                Log.i(TAG, "onNuiVprEventCallback event " + event);
            }
        };

        NativeNui nativeNui = NativeNui.GetInstance();
        int result = nativeNui.initialize(callback,params,Constants.LogLevel.LOG_LEVEL_DEBUG,true);
        nativeNui.setParams(genParams(nlsConfig));

        JSONObject resultObject = null;
        if (result == Constants.NuiResultCode.SUCCESS) {
            mInit = true;
            resultObject = JSONObjectUtil.getSuccessResult(new JSONObject());
        } else {
            resultObject = JSONObjectUtil.getFailedResult(String.valueOf(result),new JSONObject());
        }
//        bridgeContext.sendBridgeResult(resultObject);
    }

    public void startAsr(H5BridgeContext bridgeContext){
        int result = NativeNui.GetInstance().startDialog(Constants.VadMode.TYPE_P2T, getDialogParams());

        JSONObject resultObject = null;
        if (result == Constants.NuiResultCode.SUCCESS) {
            mInit = true;
            resultObject = JSONObjectUtil.getSuccessResult(new JSONObject());
            asrResultBuilder = new StringBuilder();
        } else {
            resultObject = JSONObjectUtil.getFailedResult(String.valueOf(result),new JSONObject());
        }
        bridgeContext.sendBridgeResult(resultObject);

    }

    public  void stopDialog(String fileName, H5BridgeContext bridgeContext){
        try{
            NativeNui nativeNui = NativeNui.GetInstance();
            wavFileName = fileName;

            int result = nativeNui.stopDialog();

            if(appCachePath == null || appCachePath.isEmpty()){
                appCachePath = bridgeContext.getActivity().getCacheDir().getAbsolutePath();
            }

            JSONObject resultObject = null;

            if (result == Constants.NuiResultCode.SUCCESS) {
                resultObject = asrEnd();
                if(resultObject == null){
                    if(asrError != null){
                        resultObject = JSONObjectUtil.getFailedResult(String.valueOf(asrError.errorCode),new JSONObject());
                        bridgeContext.sendBridgeResult(resultObject);
                        asrError = null;
                        return;
                    }else{
                        resultObject = JSONObjectUtil.getFailedResult(new JSONObject());
                    }
                }

            } else {
                resultObject = JSONObjectUtil.getFailedResult(String.valueOf(result),new JSONObject());
            }
            bridgeContext.sendBridgeResult(resultObject);
        }catch (Exception e){
            e.printStackTrace();
            JSONObject resultObject = JSONObjectUtil.getFailedResult(new JSONObject());
            bridgeContext.sendBridgeResult(resultObject);
        }



    }

    public  void cancelDialog(H5BridgeContext bridgeContext){
        NativeNui nativeNui = NativeNui.GetInstance();
        int result = nativeNui.cancelDialog();
        JSONObject resultObject = null;
        if (result == Constants.NuiResultCode.SUCCESS) {
            resultObject = JSONObjectUtil.getSuccessResult(new JSONObject());
        } else {
            resultObject = JSONObjectUtil.getFailedResult(String.valueOf(result),new JSONObject());
        }
        bridgeContext.sendBridgeResult(resultObject);
    }

    public  void releaseDialog(H5BridgeContext bridgeContext){
        NativeNui nativeNui = NativeNui.GetInstance();
        int result = nativeNui.release();
        JSONObject resultObject = null;
        if (result == Constants.NuiResultCode.SUCCESS) {
            resultObject = JSONObjectUtil.getSuccessResult(new JSONObject());
        } else {
            resultObject = JSONObjectUtil.getFailedResult(String.valueOf(result),new JSONObject());
        }
        bridgeContext.sendBridgeResult(resultObject);
    }

    @SuppressLint("MissingPermission")
    private void initRecode(){
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, WAVE_FRAM_SIZE * 4);
    }


    //初始化之后设置参数
    private String genParams(JSONObject nls_configs) {
        String params = "";
        try {
            JSONObject nls_config = new JSONObject();
            //参数可根据实际业务进行配置
            nls_config.put("enable_punctuation_prediction", true);
            nls_config.put("enable_intermediate_result",true);//是否返回中间识别结果
//            nls_config.put("enable_inverse_text_normalization", true);
//            nls_config.put("enable_voice_detection", true);
//            nls_config.put("customization_id", "test_id");
//            nls_config.put("vocabulary_id", "test_id");
//            nls_config.put("max_start_silence", 10000);
//            nls_config.put("max_end_silence", 800);
            nls_config.put("sample_rate", 16000);
            nls_config.put("sr_format","pcm");
            nls_config.put("enable_inverse_text_normalization",true);
//            nls_config.put("sr_format", "opus");
            JSONObject parameters = new JSONObject();

            parameters.put("nls_config", nls_config);
            parameters.put("service_type", Constants.kServiceTypeSpeechTranscriber);

            //如果有HttpDns则可进行设置
//            parameters.put("direct_ip", Utils.getDirectIp());
            params = parameters.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params;
    }


    private void asrParse(String asrResult){
        try{
            if(asrResultBuilder == null){
                return;
            }
            String asrString = AsrResultUtil.getResult(asrResult);
            if(asrString != null && !asrString.isEmpty()){
                asrResultBuilder.append(asrString);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private JSONObject asrEnd(){

        JSONObject resultJSONObject = null;
        //语音的识别结果
        String asrResultStr = asrResultBuilder.toString();
        asrResultBuilder = null;

        if(asrResultStr.isEmpty() || wavFileName.isEmpty() || appCachePath.isEmpty()){
            return null;
        }else{

//            System.out.println("asrResultStr:" + asrResultStr);
//            printString(asrResultStr);
//            String destFilePath = appCachePath + File.separator + AudioUtil.AUDIO_CACHE_DIR + File.separator + wavFileName;
//            String debugPath = appCachePath + File.separator + debugDir;
//            doFile(debugPath,destFilePath);

            JSONObject data = new JSONObject();
            data.put("asrResult", asrResultStr);
            data.put("audioPath",wavFileName);
            resultJSONObject = JSONObjectUtil.getSuccessResult(data);
            wavFileName = null;
        }

        return resultJSONObject;
    }


    //识别结束,处理音频文件,将因音频文件移动到缓存目录,删除debug目录下所有文件
    private void doFile(String debugPath,String destPath){
        File file = new File(debugPath);
        File srcFile = null;

        //获取原文件路径
        if(file.isDirectory()){
            File[] fileList = file.listFiles();
            boolean looping = true;
            for (int i=0; i<fileList.length && looping; i++){
                if(!fileList[i].isDirectory()){
                    continue;
                }

                for(File ttFile : fileList[i].listFiles()){
                    if(ttFile.isFile()){
                        String fileName = ttFile.getName();
                        if(fileName.endsWith(".wav") && fileName.startsWith("origin")){
                            srcFile = ttFile;
                            looping = false;
                            break;
                        }
                    }
                }

            }
        }

        com.alipay.mobile.common.utils.FileUtils.copyFile(srcFile, new File(destPath));
//        FileUtils.deleteFilesInDir(debugPath);
    }

    private void printString(String value){
        Log.i(TAG, value);
    }

}

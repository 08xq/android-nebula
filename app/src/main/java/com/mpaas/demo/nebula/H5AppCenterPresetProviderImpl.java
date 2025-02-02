package com.mpaas.demo.nebula;

import com.alipay.mobile.nebula.appcenter.H5PresetPkg;
import com.alipay.mobile.nebula.provider.H5AppCenterPresetProvider;
import com.mpaas.demo.constants.AppConfig;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by omg on 2018/8/6.
 */

public class H5AppCenterPresetProviderImpl implements H5AppCenterPresetProvider {

    @Override
    public Set<String> getCommonResourceAppList() {
        Set<String> appIdList = new HashSet<String>();
        appIdList.add(AppConfig.APP_ID);
        return appIdList;
    }

    @Override
    public H5PresetPkg getH5PresetPkg() {
        return null;
    }

    @Override
    public Set<String> getEnableDegradeApp() {
        return null;
    }

    @Override
    public String getTinyCommonApp() {
        return null;
    }

    @Override
    public InputStream getPresetAppInfo() {
        return null;
    }

    @Override
    public InputStream getPresetAppInfoObject() {
        return null;
    }
}

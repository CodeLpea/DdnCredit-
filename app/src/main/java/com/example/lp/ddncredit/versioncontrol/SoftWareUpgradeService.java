package com.example.lp.ddncredit.versioncontrol;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.lp.ddncredit.http.CloudClient;
import com.example.lp.ddncredit.http.model.UpgradePackageVersionInfoEntry;
import com.example.lp.ddncredit.utils.AppUtils;
import com.example.lp.ddncredit.utils.FileUtil;
import com.example.lp.ddncredit.utils.NetUtil;
import com.example.lp.ddncredit.utils.TimeUtil;

import java.io.File;

import static com.example.lp.ddncredit.constant.Constants.UPGRADE_PACKAGE_DOWNLOAD_DIR;

/**
 * 远程软件升级
 * Created by Long on 2018/10/8.
 */

public class SoftWareUpgradeService extends IntentService {
    private static final String TAG = "SoftWareUpgradeService";
    private SoftWareUpgradeInstaller softWareUpgradeInstaller = null;
    private static boolean isRunning = false;
    public SoftWareUpgradeService(){
        super(SoftWareUpgradeService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "开启版本控制服务: ");
        if(!FileUtil.isFileExist(UPGRADE_PACKAGE_DOWNLOAD_DIR)){
            FileUtil.createDirs(UPGRADE_PACKAGE_DOWNLOAD_DIR);
        }
        softWareUpgradeInstaller = new SoftWareUpgradeInstaller(this);
        //需要通知栏，初始化通知栏
        softWareUpgradeInstaller.initNotify();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //检查本次是否已经下载好的升级包，如果有则直接安装
        isRunning = true;
        do{
            File file = softWareUpgradeInstaller.getDownloadPackage();
            if(file != null){
                int ret = softWareUpgradeInstaller.silentInstall(getApplicationContext(), file);
                //说明，安装成功之后。APP会立即退出，后面的代码不会执行，因此升级包的删除放在软件重启广播中执行
               /* if(ret == 0){
                    break;
                }*/
            }
            //检查网络是否接通
            while(!NetUtil.isNetworkConnected(getApplicationContext())){
                TimeUtil.delayMs(100);
            }

            //获取当前服务上最新的版本号
            String currentApkVersion = AppUtils.getAppVersionName(getApplicationContext());
            UpgradePackageVersionInfoEntry upgradePackageVersionInfo = CloudClient.getInstance().getUpgradePackageVersionInfo(currentApkVersion);
            if(upgradePackageVersionInfo != null){
                //检查版本是否一致，如果一致则不用下载，直接退出服务
                Log.i(TAG, "currentApkVersion=" + currentApkVersion + " upgradePackageVersion=" + upgradePackageVersionInfo.getVersion());
                if(currentApkVersion.equals(upgradePackageVersionInfo.getVersion())){
                    break;
                }else if(upgradePackageVersionInfo.getVersion()!=null){
                    Log.i(TAG, "获取到新版本"+upgradePackageVersionInfo.getVersion()+"根据url进行升级包下载: ");
                    //根据url进行升级包下载
                    softWareUpgradeInstaller.startDownload(upgradePackageVersionInfo.getVersionUrl());
                    //下载完成之后，回到开头进行安装
                }

            }else{
                //在网络正常的情况下，说明服务器没有查询到该设备在最新的群里，因此不需要在去获取了
                Log.i(TAG, "在网络正常的情况下，说明服务器没有查询到该设备在最新的群里，因此不需要在去获取了: ");
                break;
            }
        }while (true);

    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand thread id is " + Thread.currentThread().getId());
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        Log.i(TAG, "onDestroy...");
    }

    public static boolean isServiceRunning(){
        return isRunning;
    }








}

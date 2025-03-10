package com.example.lp.ddncredit.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.lp.ddncredit.datebase.ParentAttendInfoDb;
import com.example.lp.ddncredit.datebase.StaffAttendInfoDb;
import com.example.lp.ddncredit.http.CloudClient;
import com.example.lp.ddncredit.http.UploadOssTask;
import com.example.lp.ddncredit.http.model.ParentAttendancedInfoEntry;
import com.example.lp.ddncredit.http.model.StaffAttendancedInfoEntry;
import com.example.lp.ddncredit.utils.NetUtil;
import com.example.lp.ddncredit.utils.TimeUtil;

import org.litepal.LitePal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 考勤信息上传服务
 * lp
 * 2019/06/03
 */

public class UploadAttendService extends Service {
    private static final String TAG = "UploadStuAttendancedInf";
    private ExecutorService mExecutorService = null;
    private static boolean isRunning = false;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate...");
        mExecutorService = Executors.newFixedThreadPool(10);
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Log.i(TAG, "onStartCommand...");
        if(mUploadParentWorker == null){
            mUploadParentWorker = new Thread(mUploadParentRunnable);
            mUploadParentWorker.start();
            Log.i(TAG, "开启家长考勤上传服务: ");
        }
        if(mUploadStaffWorker == null){
            mUploadStaffWorker = new Thread(mUploadStaffRunnable);
            mUploadStaffWorker.start();
        }
        int ret = super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "onStartCommand ret = " + ret);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy...");
        isRunning = false;
        if(mExecutorService != null) {
            mExecutorService.shutdown();
        }
        if(mUploadParentWorker != null){
            try{
                mUploadParentWorker.join();
            }catch(InterruptedException e){
                e.printStackTrace();
            }finally {
                mUploadParentWorker = null;
            }
        }
        if(mUploadStaffWorker != null){
            try{
                mUploadStaffWorker.join();
            }catch(InterruptedException e){
                e.printStackTrace();
            }finally {
                mUploadStaffWorker = null;
            }
        }
    }

    public static boolean isServiceRunning(){
        return isRunning;
    }

    private Thread mUploadParentWorker = null;
    private Runnable mUploadParentRunnable =  new Runnable() {
        @Override
        public void run() {
            while(isRunning){
                //先检查网络是否连通
                boolean result=checkNetWork();
                if(!result){
                    TimeUtil.delayMs(2000);
                    continue;
                }
                //从缓存数据库中读取数据
                ParentAttendInfoDb parentAttendInfoDb = LitePal.findFirst(ParentAttendInfoDb.class);
                if(parentAttendInfoDb != null){
                    Log.i(TAG, parentAttendInfoDb.toString());
                    String picOSSPath = null;
                    if(parentAttendInfoDb.getPicPath() != null){
                        //上传考勤照片到阿里云服务器
                        picOSSPath = UploadOssTask.getObjectKey(parentAttendInfoDb.getPicPath(), parentAttendInfoDb.getStudentID());
                        Log.i(TAG, "picOSSPath : " + picOSSPath);
                        //开启照片上传oss线程
                        if(picOSSPath != null) {
                            TimeUtil.delayMs(1000);//主要目的是等待照片保存在磁盘中
                            mExecutorService.submit(new UploadOssTask(getApplicationContext(), picOSSPath, parentAttendInfoDb.getPicPath()));
                        }
                    }
                    //上传考勤记录
                    ParentAttendancedInfoEntry parentAttendancedInfoEntry = new ParentAttendancedInfoEntry();
                    parentAttendancedInfoEntry.setStudentId(parentAttendInfoDb.getStudentID());
                    parentAttendancedInfoEntry.setParentId(parentAttendInfoDb.getParentID());
                    parentAttendancedInfoEntry.setImgUrl(picOSSPath);
                    parentAttendancedInfoEntry.setCreateTime(parentAttendInfoDb.getCreateTime());
                    //上传不成功。每间隔10s重传一次
                    boolean isUploadOk = false;
                    while(true){
                        isUploadOk = CloudClient.getInstance().uploadParentAttendancedInfo(parentAttendancedInfoEntry);
                        if(!isUploadOk){
                            int retry = 20;
                            while(isRunning && retry > 0){
                                TimeUtil.delayMs(500);
                                retry--;
                            }
                            continue;
                        }
                        break;
                    }

                    //上传成功,删除记录
                    if(isUploadOk){
                        Log.i(TAG, "上传成功,删除记录");
                        parentAttendInfoDb.delete();
                    }
                }else {
                    TimeUtil.delayMs(2000);
                }
            }
        }
    };
    private Thread mUploadStaffWorker = null;
    private Runnable mUploadStaffRunnable =  new Runnable() {
        @Override
        public void run() {

            while(isRunning){
                //先检查网络是否连通
                if(!NetUtil.isNetworkConnected(getApplicationContext())){
                    TimeUtil.delayMs(2000);
                    continue;
                }
                //从缓存数据库中读取数据
                StaffAttendInfoDb staffAttendInfoDb = LitePal.findFirst(StaffAttendInfoDb.class);
                if(staffAttendInfoDb != null){
                    Log.i(TAG, staffAttendInfoDb.toString());
                    String picOSSPath = null;
                    if(staffAttendInfoDb.getPicPath() != null){
                        //上传考勤照片到阿里云服务器
                        picOSSPath = UploadOssTask.getObjectKey(staffAttendInfoDb.getPicPath(), staffAttendInfoDb.getStaffID());
                        Log.i(TAG, "picOSSPath : " + picOSSPath);
                        //开启照片上传oss线程
                        if(picOSSPath != null) {
                            TimeUtil.delayMs(1000);//主要目的是等待照片保存在磁盘中
                            mExecutorService.submit(new UploadOssTask(getApplicationContext(), picOSSPath, staffAttendInfoDb.getPicPath()));
                        }
                    }
                    //上传考勤记录
                    StaffAttendancedInfoEntry staffAttendancedInfoEntry = new StaffAttendancedInfoEntry();
                    staffAttendancedInfoEntry.setId(staffAttendInfoDb.getStaffID());
                    staffAttendancedInfoEntry.setImgUrl(picOSSPath);
                    staffAttendancedInfoEntry.setCreateTime(staffAttendInfoDb.getCreateTime());
                    //上传不成功。每间隔10s重传一次
                    boolean isUploadOk = false;
                    while(true){
                        isUploadOk = CloudClient.getInstance().uploadStaffAttendancedInfo(staffAttendancedInfoEntry);
                        if(!isUploadOk){
                            int retry = 20;
                            while(isRunning && retry > 0){
                                TimeUtil.delayMs(500);
                                retry--;
                            }
                            continue;
                        }
                        break;
                    }

                    //上传成功,删除记录
                    if(isUploadOk){
                        Log.i(TAG, "上传成功,删除记录");
                        staffAttendInfoDb.delete();
                    }
                }else {
                    TimeUtil.delayMs(2000);
                }
            }
        }
    };

    private boolean checkNetWork() {
        boolean result=false;
        result=NetUtil.isNetworkConnected(getApplicationContext());
        return result;
    }

}

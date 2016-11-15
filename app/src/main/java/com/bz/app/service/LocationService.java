package com.bz.app.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.bz.app.GlobalContext;
import com.bz.app.IRunning;
import com.bz.app.IRunningCallback;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class LocationService extends Service implements AMapLocationListener {

    private List<LatLng> latLngs = new ArrayList<>();  //跑步轨迹集合

    public AMapLocationClient mLocationClient = null;
    public Context mContext = GlobalContext.getInstance();
    public AMapLocationClientOption mOption;

    private long runningTime; //跑步时间
    private float distance; //跑步距离
    private LatLng startLatLng = null;

    private static final String LOG_TAG = "LocationService";
    private long mStartTime = -1;
    private IRunningCallback mCallback;

    private boolean mIsRunning = false;  //跑步标志位


    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        mLocationClient = new AMapLocationClient(mContext);
        mLocationClient.setLocationListener(this);

        //定位参数
        mOption = new AMapLocationClientOption();
        mOption.setInterval(3000);
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationClient.setLocationOption(mOption);
        mLocationClient.startLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationClient.stopLocation();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {

                Gson gson = new Gson();
                LatLng mLatLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());

                try {
                    //如果是跑步，则把经纬度加入轨迹集合，计算距离
                    if (mIsRunning) {
                        latLngs.add(mLatLng);
                        if (startLatLng != null) {
                            float dis = AMapUtils.calculateLineDistance(startLatLng, mLatLng);
                            distance += dis;
                        }
                        startLatLng = mLatLng;
                    }

                    Log.v(LOG_TAG, "distance---->" + distance);
                    //跑步时的轨迹集合
                    String latLngListStr = gson.toJson(latLngs);

                    //没有跑步，把当前定位回传给客户端
                    String nowLatLngStr = gson.toJson(mLatLng);

                    if (mCallback != null) mCallback.notifyData(distance, latLngListStr, nowLatLngStr);

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 开始跑步
     */
    private void startRunning() {
        //开始跑步时间
        mStartTime = System.currentTimeMillis();
        //跑步标志位置为true
        mIsRunning = true;
        mTimeHandler.sendEmptyMessage(0);
    }


    private Handler mTimeHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mIsRunning){
                long time = System.currentTimeMillis() - mStartTime;
                if (mCallback != null) try {
                    mCallback.timeUpdate(time);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mTimeHandler.sendEmptyMessageDelayed(0,1000);
            }
            return false;
        }
    });

    /**
     * 结束跑步
     */
    private void stopRunning() {
        //跑步总时间
        runningTime = System.currentTimeMillis() - mStartTime;
        //跑步标志位置为false
        mIsRunning = false;
    }

    private IRunning.Stub stub = new IRunning.Stub() {
        @Override
        public void start() throws RemoteException {
            startRunning();
        }

        @Override
        public void stop() throws RemoteException {
            stopRunning();
        }

        @Override
        public void registCallback(IRunningCallback callback) throws RemoteException {
            mCallback = callback;
        }

        @Override
        public void unregistCallback(IRunningCallback callback) throws RemoteException {

        }

        @Override
        public boolean isRunning() throws RemoteException {
            return mIsRunning;
        }
    };
}
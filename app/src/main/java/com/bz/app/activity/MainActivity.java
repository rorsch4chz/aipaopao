package com.bz.app.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.bz.app.IRunning;
import com.bz.app.IRunningCallback;
import com.bz.app.service.LocationService;
import com.bz.app.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private TextureMapView mMapView = null;  //地图view
    private AMap aMap;  //地图对象
    private UiSettings mUiSettings;
    private Marker mLocationMarker = null;

    private ArrayList<LatLng> latLngs = new ArrayList<>(); //经纬度集合

    private static final String LOG_TAG = "MainActivity";

    private TextView mTimeTV;  //时间
    private TextView mDistanceTV; //距离
    private Button mStartRecord;  //开始跑步
    private Button mStopRecord;  //停止跑步

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        mTimeTV = (TextView) findViewById(R.id.activity_main_time_tv);
        mDistanceTV = (TextView) findViewById(R.id.activity_main_distance_tv);
        mStartRecord = (Button) findViewById(R.id.start_record);
        mStartRecord.setOnClickListener(this);
        mStopRecord = (Button) findViewById(R.id.stop_record);
        mStopRecord.setOnClickListener(this);

        mMapView = (TextureMapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        aMap = mMapView.getMap();
        mUiSettings = aMap.getUiSettings();

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_start) {
            // Handle the camera action
        } else if (id == R.id.nav_data) {

        } else if (id == R.id.nav_history) {

            Intent historyIntent = new Intent(this, HistoryActivity.class);
            startActivity(historyIntent);

        } else if (id == R.id.nav_setting) {

        } else if (id == R.id.nav_help) {

        } else if (id == R.id.nav_about) {

        } else if (id == R.id.nav_exit) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage("是否退出应用？");
            builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(MainActivity.this, LocationService.class);
                    stopService(intent);
                    finish();
                }
            });
            builder.setNegativeButton("否", null);
            builder.show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.start_record:
                    mStartRecord.setVisibility(View.GONE);
                    mStopRecord.setVisibility(View.VISIBLE);
                    mRunning.start();
                    break;
                case R.id.stop_record:
                    mStartRecord.setVisibility(View.VISIBLE);
                    mStopRecord.setVisibility(View.GONE);
                    mRunning.stop();
                    break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private IRunning mRunning;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRunning = IRunning.Stub.asInterface(service);
            try {
                mRunning.registCallback(callback);
                if (mRunning.isRunning()){
                    mStartRecord.setVisibility(View.GONE);
                    mStopRecord.setVisibility(View.VISIBLE);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, LocationService.class);
        startService(intent);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }


        @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            finish();
        }
    }

    private IRunningCallback callback = new IRunningCallback.Stub() {
        @Override
        public void notifyData(float distance, String latLngsListStr, String nowLatLngStr) throws RemoteException {
            mDistanceHandler.sendEmptyMessage((int) distance);
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<LatLng>>(){}.getType();
            //经纬度集合，
            latLngs = gson.fromJson(latLngsListStr, type);

            if (latLngs != null && latLngs.size() > 0) {
                //轨迹
                aMap.addPolyline(new PolylineOptions().addAll(latLngs).width(10).color(Color.GREEN));
            }
            //当前位置
            LatLng nowLatLng = gson.fromJson(nowLatLngStr, LatLng.class);
            if (mLocationMarker == null) {
                mLocationMarker = aMap.addMarker(new MarkerOptions()
                        .position(nowLatLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker)));
            } else {
                mLocationMarker.setPosition(nowLatLng);
            }
            //每次定位移动到地图中心
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nowLatLng, 18));
        }

        @Override
        public void timeUpdate(long time) throws RemoteException {
            mTimeHandler.sendEmptyMessage((int)(time / 1000));
        }
    };

    private Handler mDistanceHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            mDistanceTV.setText(msg.what + "m");
            return false;
        }
    });

    private Handler mTimeHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int time = msg.what;
            int hour = time / 3600;
            String hourStr = String.valueOf(hour);
            int min = time % 3600 / 60;
            String minStr = String.valueOf(min);
            int sec = time % 3600 % 60;
            String secStr = String.valueOf(sec);

            if (hour < 10) hourStr = "0" + hourStr;
            if (min < 10) minStr = "0" + minStr;
            if (sec < 10) secStr = "0" + secStr;

            mTimeTV.setText(hourStr + ":" + minStr + ":" + secStr);
            return false;
        }
    });

}
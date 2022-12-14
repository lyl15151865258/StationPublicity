package com.ottsz.stationpublicity.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.ottsz.stationpublicity.R;
import com.ottsz.stationpublicity.bean.EventMsg;
import com.ottsz.stationpublicity.constant.EventTag;
import com.ottsz.stationpublicity.contentprovider.SPHelper;
import com.ottsz.stationpublicity.util.LogUtils;
import com.ottsz.stationpublicity.util.TimeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务服务（定时同步服务器数据，定时重启）
 * Created at 2019/9/12 13:48
 *
 * @author LiYuliang
 * @version 1.0
 */

public class TimeTaskService extends Service {

    private final String TAG = "TimeTaskService";
    private Context mContext;
    private TimeTaskServiceBinder timeTaskServiceBinder;
    private ScheduledExecutorService threadPool;
    private int showTime;
    // 定时检查间隔时间200ms
    private static final int betweenTime = 200;
    private long startTime = 0L;
    private int currentSourceType = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return timeTaskServiceBinder;
    }

    public class TimeTaskServiceBinder extends Binder {
        /**
         * TimeTaskServiceBinder
         *
         * @return SocketService对象
         */
        public TimeTaskService getService() {
            return TimeTaskService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d(TAG, "TimeTaskService:onCreate");
        mContext = this;
        showTime = SPHelper.getInt("SHOWTIME", 10);
        threadPool = Executors.newScheduledThreadPool(1);
        showNotification();
        executeShutDown();
        timeTaskServiceBinder = new TimeTaskServiceBinder();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d(TAG, "TimeTaskService:onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 前台Service
     */
    private void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel Channel = new NotificationChannel("133", getString(R.string.TimeTaskService), NotificationManager.IMPORTANCE_NONE);
            Channel.enableLights(true);                                         //设置提示灯
            Channel.setLightColor(Color.RED);                                   //设置提示灯颜色
            Channel.setShowBadge(true);                                         //显示logo
            Channel.setDescription(getString(R.string.TimeTaskService));        //设置描述
            Channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);    //设置锁屏不可见 VISIBILITY_SECRET=不可见
            manager.createNotificationChannel(Channel);

            NotificationCompat.Builder notification = new NotificationCompat.Builder(mContext, "133");
            notification.setContentTitle(getString(R.string.app_name));
            notification.setContentText(getString(R.string.TimeTaskServiceRunning));
            notification.setWhen(System.currentTimeMillis());
            notification.setSmallIcon(R.mipmap.ic_launcher);
            notification.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
            startForeground(133, notification.build());
        } else {
            Notification notification = new Notification.Builder(mContext)
                    .setContentTitle(getString(R.string.app_name))                                      //设置标题
                    .setContentText(getString(R.string.TimeTaskServiceRunning))                         //设置内容
                    .setWhen(System.currentTimeMillis())                                                //设置创建时间
                    .setSmallIcon(R.mipmap.ic_launcher)                                                 //设置状态栏图标
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))   //设置通知栏图标
                    .build();
            startForeground(133, notification);
        }
    }

    @SuppressLint("WrongConstant")
    public void executeShutDown() {
        threadPool.scheduleAtFixedRate(() -> {
            if (currentSourceType == 1) {
                // 如果当前显示的是图片，则指定时间滚动到下一个资源
                if (TimeUtils.getCurrentTimeMillis() - startTime > showTime * 1000L) {
                    LogUtils.d(TAG, "5秒发送显示图片的消息");
                    EventMsg msg = new EventMsg();
                    msg.setTag(EventTag.NEXT_PAGE);
                    EventBus.getDefault().post(msg);
                }
            }
        }, 0, betweenTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 设置新的图片展示时间
     */
    public void setShowTime(int showTime) {
        this.showTime = showTime;
    }

    /**
     * 收到EventBus发来的消息并处理
     *
     * @param msg 消息对象
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(EventMsg msg) {
        switch (msg.getTag()) {
            case EventTag.START_IMAGE:
                // 开始展示图片
                LogUtils.d(TAG, "开始展示图片");
                currentSourceType = 1;
                startTime = TimeUtils.getCurrentTimeMillis();
                break;
            case EventTag.START_VIDEO:
                // 开始展示视频
                LogUtils.d(TAG, "开始播放视频");
                currentSourceType = 2;
                startTime = Long.MAX_VALUE;
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        threadPool.shutdown();
        threadPool = null;
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
    }

}
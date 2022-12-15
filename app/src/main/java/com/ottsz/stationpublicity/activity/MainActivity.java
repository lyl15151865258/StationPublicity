package com.ottsz.stationpublicity.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.ottsz.stationpublicity.R;
import com.ottsz.stationpublicity.StationPublicityApplication;
import com.ottsz.stationpublicity.adapter.ViewPagerAdapter;
import com.ottsz.stationpublicity.bean.EventMsg;
import com.ottsz.stationpublicity.bean.Resource;
import com.ottsz.stationpublicity.bean.Result;
import com.ottsz.stationpublicity.constant.ApkInfo;
import com.ottsz.stationpublicity.constant.ErrorCode;
import com.ottsz.stationpublicity.constant.EventTag;
import com.ottsz.stationpublicity.contentprovider.SPHelper;
import com.ottsz.stationpublicity.network.ExceptionHandle;
import com.ottsz.stationpublicity.network.NetClient;
import com.ottsz.stationpublicity.network.NetworkObserver;
import com.ottsz.stationpublicity.network.download.DownloadCallBack;
import com.ottsz.stationpublicity.network.download.RetrofitHttp;
import com.ottsz.stationpublicity.service.TimeTaskService;
import com.ottsz.stationpublicity.sqlite.DbHelper;
import com.ottsz.stationpublicity.sqlite.table.Table;
import com.ottsz.stationpublicity.util.FileUtil;
import com.ottsz.stationpublicity.util.GsonUtils;
import com.ottsz.stationpublicity.util.LogUtils;
import com.ottsz.stationpublicity.util.NetworkUtil;
import com.ottsz.stationpublicity.util.Utils;
import com.ottsz.stationpublicity.widget.DownLoadDialog;
import com.ottsz.stationpublicity.widget.SelectDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import xyz.doikki.videoplayer.player.BaseVideoView;
import xyz.doikki.videoplayer.player.VideoView;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private Context mContext;
    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private List<Resource> remoteList, localList;
    private TimeTaskService timeTaskService;
    private int currentPosition, currentDownload;
    private DbHelper dbHelper;
    private DownLoadDialog progressDialog;
    private ProgressBar myProgressBar;
    private AppCompatTextView tvPage, tvPercent, tvProgress;
    private VideoView mVideoView;
    private boolean isFirstIn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);

        mVideoView = new VideoView(this);
        mVideoView.addOnStateChangeListener(simpleOnStateChangeListener);

        initViewPager();
        initTimeTaskService();
        initDownloadDialog();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        dbHelper = ((StationPublicityApplication) getApplication()).getDbHelper();
    }

    private void initViewPager() {
        viewPager = findViewById(R.id.viewpager);
//        viewPager.setOffscreenPageLimit(4);
        tvPage = findViewById(R.id.tvPage);
        remoteList = new ArrayList<>();
        localList = new ArrayList<>();
        viewPagerAdapter = new ViewPagerAdapter(this, localList);
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        viewPager.registerOnPageChangeCallback(onPageChangeCallback);
    }

    private void initDownloadDialog() {
        // 下载对话框
        progressDialog = new DownLoadDialog(mContext);
        myProgressBar = progressDialog.findViewById(R.id.progressBar);
        tvPercent = progressDialog.findViewById(R.id.tvPercent);
        tvProgress = progressDialog.findViewById(R.id.tvProgress);
        progressDialog.setCancelable(false);
    }

    private final BaseVideoView.SimpleOnStateChangeListener simpleOnStateChangeListener = new BaseVideoView.SimpleOnStateChangeListener() {
        @Override
        public void onPlayStateChanged(int playState) {
            if (playState == VideoView.STATE_PLAYBACK_COMPLETED) {
                // 视频播放结束
                if (!timeTaskService.isPause) {
                    // 如果当前没有手动暂停
                    if (localList.size() > 1) {
                        // 资源大于1个时，则播放完毕后，播放下一个资源
                        currentPosition++;
                        viewPager.setCurrentItem(currentPosition, true);
                    } else {
                        // 资源数等于1时，停留在当前位置
                        viewPager.setCurrentItem(currentPosition, true);
                    }
                }
            }
        }
    };

    private final ViewPager2.OnPageChangeCallback onPageChangeCallback = new ViewPager2.OnPageChangeCallback() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            currentPosition = position;
            LogUtils.d(TAG, "当前选中页面：" + currentPosition);
            if (localList != null && localList.size() > 0) {
//                viewPager.post(() -> startPlay(position % localList.size()));
                viewPager.postDelayed(() -> startPlay(position % localList.size()), 100);
                // 显示/隐藏页码
                if (localList.size() > 1) {
                    tvPage.setVisibility(View.VISIBLE);
                    tvPage.setText(String.format(Locale.CHINA, "%d/%d", position % localList.size() + 1, localList.size()));
                } else {
                    tvPage.setVisibility(View.GONE);
                }
            } else {
                tvPage.setVisibility(View.GONE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
            LogUtils.d(TAG, "当前滑动状态：" + state);
        }
    };

    private void startPlay(int position) {
        //ViewPage2内部是通过RecyclerView去实现的，它位于ViewPager2的第0个位置
        RecyclerView mViewPagerImpl = (RecyclerView) viewPager.getChildAt(0);
        int count = mViewPagerImpl.getChildCount();
        // 循环获取ViewHolder并设置
        for (int i = 0; i < count; i++) {
            View itemView = mViewPagerImpl.getChildAt(i);
            if (itemView != null && itemView.getTag() instanceof ViewPagerAdapter.VideoViewHolder) {
                ViewPagerAdapter.VideoViewHolder viewHolder = (ViewPagerAdapter.VideoViewHolder) itemView.getTag();
                // 先移除VideoView
                mVideoView.release();
                Utils.removeViewFormParent(mVideoView);
                // 如果是当前显示的ViewHolder
                if (viewHolder.mPosition == position) {
                    Resource resource = localList.get(position);
                    File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, resource.getLocalName());
                    LogUtils.d(TAG, "当前是VideoViewHolder，position匹配，展示视频：" + localList.get(position).getLocalName());
                    mVideoView.setLooping(localList.size() == 1);
                    mVideoView.setUrl(file.getAbsolutePath());
                    viewHolder.container.addView(mVideoView, 0);
                    mVideoView.start();
                    // 发送播放视频的通知
                    EventMsg msg = new EventMsg();
                    msg.setTag(EventTag.START_VIDEO);
                    EventBus.getDefault().post(msg);
                    break;
                } else {
                    LogUtils.d(TAG, "当前是VideoViewHolder，position不匹配");
                }
            } else if (itemView != null && itemView.getTag() instanceof ViewPagerAdapter.ImageViewHolder) {
                ViewPagerAdapter.ImageViewHolder viewHolder = (ViewPagerAdapter.ImageViewHolder) itemView.getTag();
                if (viewHolder.mPosition == position) {
                    LogUtils.d(TAG, "当前是ImageViewHolder，position匹配，展示图片：" + localList.get(position).getLocalName());
                    // 当资源数量大于1个时，发送播放图片的通知
                    if (localList.size() > 1) {
                        EventMsg msg = new EventMsg();
                        msg.setTag(EventTag.START_IMAGE);
                        EventBus.getDefault().post(msg);
                    }
                    break;
                } else {
                    LogUtils.d(TAG, "当前是ImageViewHolder，position不匹配");
                }
            } else {
                LogUtils.d(TAG, "itemView为null");
            }
        }
    }

    /**
     * 初始化并绑定TimeTaskService
     */
    private void initTimeTaskService() {
        Intent intent = new Intent(mContext, TimeTaskService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);
    }

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            TimeTaskService.TimeTaskServiceBinder timeTaskServiceBinder = (TimeTaskService.TimeTaskServiceBinder) binder;
            timeTaskService = timeTaskServiceBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /**
     * 收到EventBus发来的消息并处理
     *
     * @param msg 消息对象
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(EventMsg msg) {
//        mCurPos = viewPager.getCurrentItem();
        switch (msg.getTag()) {
            case EventTag.NEXT_PAGE:
                // 滚动到下一页
                currentPosition++;
                viewPager.setCurrentItem(currentPosition, true);
                break;
            case EventTag.LAST_PAGE:
                // 滚动到上一页
                currentPosition--;
                viewPager.setCurrentItem(currentPosition, true);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFirstIn) {
            // 查询服务端资源
            searchResource();
            isFirstIn = false;
        }
        if (timeTaskService != null) {
            timeTaskService.isPause = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停定时任务
        if (timeTaskService != null) {
            timeTaskService.isPause = true;
        }
        if (mVideoView != null) {
            if (mVideoView.isPlaying()) {
                mVideoView.pause();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        timeTaskService.onDestroy();
        unbindService(conn);
        super.onDestroy();
    }

    /**
     * 查询资源
     */
    private void searchResource() {
        JsonObject params = new JsonObject();
        params.addProperty("typeId", 0);
        Observable<Result> resultObservable = NetClient.getInstance(NetClient.getBaseUrl(), false).getApi().searchResources(params);
        resultObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new NetworkObserver<Result>(this) {

            @Override
            public void onSubscribe(Disposable d) {
                //接下来可以检查网络连接等操作
                if (!NetworkUtil.isNetworkAvailable(mContext)) {
                    Toast.makeText(mContext, "网络不可用", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(ExceptionHandle.ResponseThrowable responseThrowable) {
                Toast.makeText(mContext, "资源获取失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNext(@NonNull Result result) {
                super.onNext(result);
                if (result.getCode() == ErrorCode.SUCCESS) {
                    remoteList.clear();
                    remoteList.addAll(GsonUtils.string2List(GsonUtils.convertJSON(result.getData()), Resource.class));
                    // 更新本地数据库
                    updateLocalDatabase();
                } else {
                    Toast.makeText(mContext, "资源获取失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void downloadResources() {
        // 筛选需要下载的文件列表
        List<Resource> downloadResource = new ArrayList<>();
        for (Resource resource : localList) {
            if (resource.getLocalName() == null || resource.getLocalName().equals("")) {
                // 如果文件名不存在，则下载文件
                downloadResource.add(resource);
            } else {
                // 如果文件名存在，检查文件是否存在
                File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, resource.getLocalName());
                if (file.exists()) {
                    // 如果文件存在
                    if (!resource.getMd5().equals(FileUtil.getFileMD5(file))) {
                        // 文件MD5不匹配，删除本地文件并重新下载
                        if (file.delete()) {
                            downloadResource.add(resource);
                        }
                    }
                } else {
                    // 如果文件不存在
                    downloadResource.add(resource);
                }
            }
        }
        // 下载文件列表
        if (downloadResource.size() > 0) {
            currentDownload = 0;
            downloadFile(downloadResource);
        } else {
            // 没有要下载的文件，直接展示列表
            if (localList.size() > 1) {
                int round = Integer.MAX_VALUE / 2 / localList.size();
                currentPosition = localList.size() * round;
                viewPager.setCurrentItem(currentPosition, false);
            } else if (localList.size() == 1) {
                currentPosition = 0;
                viewPager.setCurrentItem(currentPosition, false);
            }
            viewPagerAdapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void downloadFile(List<Resource> downloadResource) {
        if (currentDownload >= downloadResource.size()) {
            progressDialog.dismiss();
            return;
        }

        Resource resource = downloadResource.get(currentDownload);
        String downloadUrl = NetClient.getBaseUrl() + resource.getUrl();
        String mDownloadFileName = resource.getUrl().split("/")[resource.getUrl().split("/").length - 1];

        File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, mDownloadFileName);
        long range = 0;
        if (file.exists()) {
            range = SPHelper.getLong(downloadUrl, 0);
            if (range == file.length()) {
                currentDownload++;
                if (currentDownload < downloadResource.size()) {
                    downloadFile(downloadResource);
                    return;
                } else {
                    progressDialog.dismiss();
                    // 重新查询本地资源文件
                    queryAllResource();
                    if (localList.size() > 1) {
                        int round = Integer.MAX_VALUE / 2 / localList.size();
                        currentPosition = localList.size() * round;
                        viewPager.setCurrentItem(currentPosition, false);
                    } else if (localList.size() == 1) {
                        currentPosition = 0;
                        viewPager.setCurrentItem(currentPosition, false);
                    }
                    viewPagerAdapter.notifyDataSetChanged();
                }
                return;
            }
        }
        // 显示下载等待窗口
        progressDialog.show();

        long finalRange = range;
        new Thread(() -> RetrofitHttp.getInstance().downloadFile(finalRange, downloadUrl, mDownloadFileName, new DownloadCallBack() {
            @Override
            public void onProgress(int progress) {
                runOnUiThread(() -> {
                    myProgressBar.setProgress(progress, true);
                    tvPercent.setText(String.format(Locale.CHINA, "%d%%", progress));
                    tvProgress.setText(String.format(Locale.CHINA, "正在下载第%d个资源，共%d个资源", currentDownload + 1, downloadResource.size()));
                });
            }

            @Override
            public void onCompleted() {
                runOnUiThread(() -> {
                    File file1 = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, mDownloadFileName);
                    // 更新本地数据库文件名和文件MD5值
                    resource.setLocalName(mDownloadFileName);
                    resource.setMd5(FileUtil.getFileMD5(file1));
                    updateLocalFile(resource);
                    currentDownload++;
                    if (currentDownload < downloadResource.size()) {
                        downloadFile(downloadResource);
                    } else {
                        progressDialog.dismiss();
                        // 所有文件下载完成，重新查询本地资源文件
                        queryAllResource();
                        if (localList.size() > 1) {
                            int round = Integer.MAX_VALUE / 2 / localList.size();
                            currentPosition = localList.size() * round;
                            viewPager.setCurrentItem(currentPosition, false);
                        } else if (localList.size() == 1) {
                            currentPosition = 0;
                            viewPager.setCurrentItem(currentPosition, false);
                        }
                        viewPagerAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(String msg) {
                progressDialog.dismiss();
                Toast.makeText(mContext, "下载发生错误", Toast.LENGTH_SHORT).show();
            }
        })).start();
    }

    /**
     * 更新本地数据库
     */
    private void updateLocalDatabase() {
        // 查询本地记录
        queryAllResource();
        HashMap<Integer, Resource> localResource = new HashMap<>();
        for (Resource resource : localList) {
            localResource.put(resource.getId(), resource);
        }
        HashMap<Integer, Resource> remoteResource = new HashMap<>();
        for (Resource rResource : remoteList) {
            remoteResource.put(rResource.getId(), rResource);
            // 将本地相同ID的资源进行比较
            Resource lResource = localResource.get(rResource.getId());
            if (lResource == null) {
                // 如果本地没有相同ID的资源，则插入该资源
                insertData(rResource);
            } else {
                // 如果本地有相同ID的资源，则比较详细内容
                if (rResource.getType() == lResource.getType() && rResource.getUrl().equals(lResource.getUrl()) && rResource.getMd5().equals(lResource.getMd5())) {
                    // 资源主要信息（类型、URL、MD5）匹配
                    if (rResource.getSort() != lResource.getSort()) {
                        // 如果排序不相同，更新本地资源记录的排序
                        updateSort(rResource);
                    }
                } else {
                    // 资源主要信息不匹配，删除本地的资源记录
                    deleteResource(lResource);
                    // 删除相应的文件
                    if (lResource.getLocalName() != null) {
                        File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, lResource.getLocalName());
                        file.deleteOnExit();
                    }
                    insertData(rResource);
                }
            }
        }
        // 删除本地存在，但是服务器已经删除的资源记录和文件
        for (Resource lResource : localList) {
            // 将服务器相同ID的资源进行比较
            Resource rResource = remoteResource.get(lResource.getId());
            if (rResource == null) {
                // 服务器已经没有该ID的资源，删除本地的资源记录
                deleteResource(lResource);
                // 删除相应的文件
                if (lResource.getLocalName() != null) {
                    File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, lResource.getLocalName());
                    if (file.exists() && file.delete()) {
                        LogUtils.d(TAG, "服务器已经没有该ID的资源，删除本地记录和文件");
                    }
                }
            }
        }
        // 重新查询本地记录
        queryAllResource();
        // 更新本地文件
        downloadResources();
    }

    /**
     * 查询所有本地记录
     */
    private void queryAllResource() {
        Cursor cursor = dbHelper.findList(Table.SourceTable.TABLE_NAME, null, null, null, null, null, "sort asc");
        // 清空list
        localList.clear();
        // 查询到的数据添加到list集合
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            int type = cursor.getInt(1);
            String url = cursor.getString(2);
            String md5 = cursor.getString(3);
            String local = cursor.getString(4);
            int sort = cursor.getInt(5);
            localList.add(new Resource(id, type, url, md5, local, sort));
        }
        cursor.close();
    }

    /**
     * 插入数据
     */
    private void insertData(Resource resource) {
        ContentValues values = new ContentValues();
        values.put(Table.SourceTable.ID, resource.getId());
        values.put(Table.SourceTable.TYPE, resource.getType());
        values.put(Table.SourceTable.URL, resource.getUrl());
        values.put(Table.SourceTable.MD5, "");
        values.put(Table.SourceTable.LOCAL_NAME, "");
        values.put(Table.SourceTable.SORT, resource.getSort());
        dbHelper.insert(Table.SourceTable.TABLE_NAME, values);
    }

    /**
     * 更新排序
     */
    private void updateSort(Resource resource) {
        ContentValues values = new ContentValues();
        values.put(Table.SourceTable.SORT, resource.getSort());
        dbHelper.update(Table.SourceTable.TABLE_NAME, values, Table.SourceTable.ID + " = " + resource.getId(), null);
    }

    /**
     * 更新本地文件信息
     */
    private void updateLocalFile(Resource resource) {
        ContentValues values = new ContentValues();
        values.put(Table.SourceTable.LOCAL_NAME, resource.getLocalName());
        values.put(Table.SourceTable.MD5, resource.getMd5());
        dbHelper.update(Table.SourceTable.TABLE_NAME, values, Table.SourceTable.ID + " = " + resource.getId(), null);
    }

    /**
     * 删除本地资源
     */
    private void deleteResource(Resource resource) {
        dbHelper.delete(Table.SourceTable.TABLE_NAME, Table.SourceTable.ID + " = " + resource.getId(), null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // 向下键
                int waitTime = SPHelper.getInt("SHOWTIME", 10);
                if (waitTime == 3) {
                    Toast.makeText(mContext, "图片展示时间不得低于3秒", Toast.LENGTH_SHORT).show();
                } else {
                    waitTime--;
                    SPHelper.save("SHOWTIME", waitTime);
                    Toast.makeText(mContext, "当前图片停留时间为" + waitTime + "秒", Toast.LENGTH_SHORT).show();
                }
                timeTaskService.setShowTime(waitTime);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                // 向上键，增加图片停留时间
                int waitTime2 = SPHelper.getInt("SHOWTIME", 10);
                waitTime2++;
                SPHelper.save("SHOWTIME", waitTime2);
                Toast.makeText(mContext, "当前图片展示时间为" + waitTime2 + "秒", Toast.LENGTH_SHORT).show();
                timeTaskService.setShowTime(waitTime2);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // 向左键
                // 滚动到上一页
                currentPosition--;
                viewPager.setCurrentItem(currentPosition, true);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // 向右键
                // 滚动到下一页
                currentPosition++;
                viewPager.setCurrentItem(currentPosition, true);
                break;
            case KeyEvent.KEYCODE_MENU:
                // 菜单键
                startApp();
                break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // 确定键enter
                // 图片暂停滚动（定时任务暂停）
                timeTaskService.isPause = !timeTaskService.isPause;
                if (timeTaskService.isPause) {
                    Toast.makeText(mContext, "暂停自动播放", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "恢复自动播放", Toast.LENGTH_SHORT).show();
                }
                // 视频暂停播放
                if (mVideoView != null) {
                    if (timeTaskService.isPause) {
                        if (mVideoView.isPlaying()) {
                            mVideoView.pause();
                        }
                    } else {
                        mVideoView.start();
                    }
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                // 返回键
                exitApp();
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 根据包名打开APP
     */
    private void startApp() {
        PackageInfo packageinfo = null;
        try {
            packageinfo = getPackageManager().getPackageInfo("com.ott.robottv", 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            Toast.makeText(getApplicationContext(), "没有安装光伏清扫机器人监控系统", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent resolveIntent = getPackageManager().getLaunchIntentForPackage("com.ott.robottv");
        startActivity(resolveIntent);
    }

    /**
     * 显示确认退出的弹窗
     */
    private void exitApp() {
        SelectDialog selectDialog = new SelectDialog(mContext, getString(R.string.warning_to_exit));
        selectDialog.setButtonText(getString(R.string.Cancel), getString(R.string.Continue));
        selectDialog.setCancelable(false);
        selectDialog.setOnDialogClickListener(new SelectDialog.OnDialogClickListener() {
            @Override
            public void onOKClick() {
                System.exit(0);
            }

            @Override
            public void onCancelClick() {

            }
        });
        selectDialog.show();
    }
}
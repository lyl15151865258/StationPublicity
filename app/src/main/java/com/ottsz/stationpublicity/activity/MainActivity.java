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

        initViewPager();
        initVideoView();
        initTimeTaskService();
        initDownloadDialog();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        dbHelper = ((StationPublicityApplication) getApplication()).getDbHelper();
    }

    private void initViewPager() {
        viewPager = findViewById(R.id.viewpager);
        tvPage = findViewById(R.id.tvPage);
        remoteList = new ArrayList<>();
        localList = new ArrayList<>();
        viewPagerAdapter = new ViewPagerAdapter(this, localList);
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        viewPager.registerOnPageChangeCallback(onPageChangeCallback);
    }

    private void initVideoView() {
        mVideoView = new VideoView(this);
        // ????????????????????????
        mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_MATCH_PARENT);
        mVideoView.addOnStateChangeListener(simpleOnStateChangeListener);
    }

    private void initDownloadDialog() {
        // ???????????????
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
                // ??????????????????
                if (!timeTaskService.isPause) {
                    // ??????????????????????????????
                    if (localList.size() > 1) {
                        // ????????????1???????????????????????????????????????????????????
                        currentPosition++;
                        viewPager.setCurrentItem(currentPosition, true);
                    } else {
                        // ???????????????1???????????????????????????
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
            LogUtils.d(TAG, "?????????????????????" + currentPosition);
            if (localList != null && localList.size() > 0) {
//                viewPager.post(() -> startPlay(position % localList.size()));
                viewPager.postDelayed(() -> startPlay(position % localList.size()), 100);
                // ??????/????????????
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
            LogUtils.d(TAG, "?????????????????????" + state);
        }
    };

    private void startPlay(int position) {
        //ViewPage2???????????????RecyclerView????????????????????????ViewPager2??????0?????????
        RecyclerView mViewPagerImpl = (RecyclerView) viewPager.getChildAt(0);
        int count = mViewPagerImpl.getChildCount();
        mVideoView.release();
        // ????????????ViewHolder?????????
        for (int i = 0; i < count; i++) {
            View itemView = mViewPagerImpl.getChildAt(i);
            if (itemView != null && itemView.getTag() instanceof ViewPagerAdapter.VideoViewHolder) {
                ViewPagerAdapter.VideoViewHolder viewHolder = (ViewPagerAdapter.VideoViewHolder) itemView.getTag();
                // ?????????VideoView
                Utils.removeViewFormParent(mVideoView);
                // ????????????????????????ViewHolder
                if (viewHolder.mPosition == position) {
                    Resource resource = localList.get(position);
                    File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, resource.getLocalName());
                    LogUtils.d(TAG, "?????????VideoViewHolder???position????????????????????????" + localList.get(position).getLocalName());
                    mVideoView.setLooping(localList.size() == 1);
                    mVideoView.setUrl(file.getAbsolutePath());
                    viewHolder.container.addView(mVideoView, 0);
                    mVideoView.start();
                    // ???????????????????????????
                    EventMsg msg = new EventMsg();
                    msg.setTag(EventTag.START_VIDEO);
                    EventBus.getDefault().post(msg);
                    break;
                } else {
                    LogUtils.d(TAG, "?????????VideoViewHolder???position?????????");
                }
            } else if (itemView != null && itemView.getTag() instanceof ViewPagerAdapter.ImageViewHolder) {
                ViewPagerAdapter.ImageViewHolder viewHolder = (ViewPagerAdapter.ImageViewHolder) itemView.getTag();
                if (viewHolder.mPosition == position) {
                    LogUtils.d(TAG, "?????????ImageViewHolder???position????????????????????????" + localList.get(position).getLocalName());
                    // ?????????????????????1????????????????????????????????????
                    if (localList.size() > 1) {
                        EventMsg msg = new EventMsg();
                        msg.setTag(EventTag.START_IMAGE);
                        EventBus.getDefault().post(msg);
                    }
                    break;
                } else {
                    LogUtils.d(TAG, "?????????ImageViewHolder???position?????????");
                }
            } else {
                LogUtils.d(TAG, "itemView???null");
            }
        }
    }

    /**
     * ??????????????????TimeTaskService
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
     * ??????EventBus????????????????????????
     *
     * @param msg ????????????
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(EventMsg msg) {
//        mCurPos = viewPager.getCurrentItem();
        switch (msg.getTag()) {
            case EventTag.NEXT_PAGE:
                // ??????????????????
                currentPosition++;
                viewPager.setCurrentItem(currentPosition, true);
                break;
            case EventTag.LAST_PAGE:
                // ??????????????????
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
            // ?????????????????????
            searchResource();
            isFirstIn = false;
        } else {
            if (timeTaskService != null) {
                timeTaskService.isPause = false;
                timeTaskService.resetStartTime();
            }
            // ??????????????????
            if (mVideoView != null) {
                if (localList != null && localList.size() > 0 && localList.get(currentPosition % localList.size()).getType() == 2) {
                    mVideoView.start();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ??????????????????
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
     * ????????????
     */
    private void searchResource() {
        JsonObject params = new JsonObject();
        params.addProperty("typeId", 0);
        Observable<Result> resultObservable = NetClient.getInstance(NetClient.getBaseUrl(), false).getApi().searchResources(params);
        resultObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new NetworkObserver<Result>(this) {

            @Override
            public void onSubscribe(Disposable d) {
                //??????????????????????????????????????????
                if (!NetworkUtil.isNetworkAvailable(mContext)) {
                    Toast.makeText(mContext, "???????????????", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(ExceptionHandle.ResponseThrowable responseThrowable) {
                Toast.makeText(mContext, "??????????????????", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNext(@NonNull Result result) {
                super.onNext(result);
                if (result.getCode() == ErrorCode.SUCCESS) {
                    remoteList.clear();
                    remoteList.addAll(GsonUtils.string2List(GsonUtils.convertJSON(result.getData()), Resource.class));
                    // ?????????????????????
                    updateLocalDatabase();
                } else {
                    Toast.makeText(mContext, "??????????????????", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void downloadResources() {
        // ?????????????????????????????????
        List<Resource> downloadResource = new ArrayList<>();
        for (Resource resource : localList) {
            if (resource.getLocalName() == null || resource.getLocalName().equals("")) {
                // ??????????????????????????????????????????
                downloadResource.add(resource);
            } else {
                // ????????????????????????????????????????????????
                File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, resource.getLocalName());
                if (file.exists()) {
                    // ??????????????????
                    if (!resource.getMd5().equals(FileUtil.getFileMD5(file))) {
                        // ??????MD5?????????????????????????????????????????????
                        if (file.delete()) {
                            downloadResource.add(resource);
                        }
                    }
                } else {
                    // ?????????????????????
                    downloadResource.add(resource);
                }
            }
        }
        // ??????????????????
        if (downloadResource.size() > 0) {
            currentDownload = 0;
            downloadFile(downloadResource);
        } else {
            // ?????????????????????????????????????????????
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
                    // ??????????????????????????????
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
        // ????????????????????????
        progressDialog.show();

        long finalRange = range;
        new Thread(() -> RetrofitHttp.getInstance().downloadFile(finalRange, downloadUrl, mDownloadFileName, new DownloadCallBack() {
            @Override
            public void onProgress(int progress) {
                runOnUiThread(() -> {
                    myProgressBar.setProgress(progress, true);
                    tvPercent.setText(String.format(Locale.CHINA, "%d%%", progress));
                    tvProgress.setText(String.format(Locale.CHINA, "???????????????%d???????????????%d?????????", currentDownload + 1, downloadResource.size()));
                });
            }

            @Override
            public void onCompleted() {
                runOnUiThread(() -> {
                    File file1 = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, mDownloadFileName);
                    // ???????????????????????????????????????MD5???
                    resource.setLocalName(mDownloadFileName);
                    resource.setMd5(FileUtil.getFileMD5(file1));
                    updateLocalFile(resource);
                    currentDownload++;
                    if (currentDownload < downloadResource.size()) {
                        downloadFile(downloadResource);
                    } else {
                        progressDialog.dismiss();
                        // ?????????????????????????????????????????????????????????
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
                Toast.makeText(mContext, "??????????????????", Toast.LENGTH_SHORT).show();
            }
        })).start();
    }

    /**
     * ?????????????????????
     */
    private void updateLocalDatabase() {
        // ??????????????????
        queryAllResource();
        HashMap<Integer, Resource> localResource = new HashMap<>();
        for (Resource resource : localList) {
            localResource.put(resource.getId(), resource);
        }
        HashMap<Integer, Resource> remoteResource = new HashMap<>();
        for (Resource rResource : remoteList) {
            remoteResource.put(rResource.getId(), rResource);
            // ???????????????ID?????????????????????
            Resource lResource = localResource.get(rResource.getId());
            if (lResource == null) {
                // ????????????????????????ID??????????????????????????????
                insertData(rResource);
            } else {
                // ?????????????????????ID?????????????????????????????????
                if (rResource.getType() == lResource.getType() && rResource.getUrl().equals(lResource.getUrl()) && rResource.getMd5().equals(lResource.getMd5())) {
                    // ??????????????????????????????URL???MD5?????????
                    if (rResource.getSort() != lResource.getSort()) {
                        // ?????????????????????????????????????????????????????????
                        updateSort(rResource);
                    }
                } else {
                    // ?????????????????????????????????????????????????????????
                    deleteResource(lResource);
                    // ?????????????????????
                    if (lResource.getLocalName() != null) {
                        File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, lResource.getLocalName());
                        file.deleteOnExit();
                    }
                    insertData(rResource);
                }
            }
        }
        // ????????????????????????????????????????????????????????????????????????
        for (Resource lResource : localList) {
            // ??????????????????ID?????????????????????
            Resource rResource = remoteResource.get(lResource.getId());
            if (rResource == null) {
                // ????????????????????????ID???????????????????????????????????????
                deleteResource(lResource);
                // ?????????????????????
                if (lResource.getLocalName() != null) {
                    File file = new File(ApkInfo.APP_ROOT_PATH + ApkInfo.DOWNLOAD_DIR, lResource.getLocalName());
                    if (file.exists() && file.delete()) {
                        LogUtils.d(TAG, "????????????????????????ID???????????????????????????????????????");
                    }
                }
            }
        }
        // ????????????????????????
        queryAllResource();
        // ??????????????????
        downloadResources();
    }

    /**
     * ????????????????????????
     */
    private void queryAllResource() {
        Cursor cursor = dbHelper.findList(Table.SourceTable.TABLE_NAME, null, null, null, null, null, "sort asc");
        // ??????list
        localList.clear();
        // ???????????????????????????list??????
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
     * ????????????
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
     * ????????????
     */
    private void updateSort(Resource resource) {
        ContentValues values = new ContentValues();
        values.put(Table.SourceTable.SORT, resource.getSort());
        dbHelper.update(Table.SourceTable.TABLE_NAME, values, Table.SourceTable.ID + " = " + resource.getId(), null);
    }

    /**
     * ????????????????????????
     */
    private void updateLocalFile(Resource resource) {
        ContentValues values = new ContentValues();
        values.put(Table.SourceTable.LOCAL_NAME, resource.getLocalName());
        values.put(Table.SourceTable.MD5, resource.getMd5());
        dbHelper.update(Table.SourceTable.TABLE_NAME, values, Table.SourceTable.ID + " = " + resource.getId(), null);
    }

    /**
     * ??????????????????
     */
    private void deleteResource(Resource resource) {
        dbHelper.delete(Table.SourceTable.TABLE_NAME, Table.SourceTable.ID + " = " + resource.getId(), null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // ?????????
                int waitTime = SPHelper.getInt("SHOWTIME", 10);
                if (waitTime == 3) {
                    Toast.makeText(mContext, "??????????????????????????????3???", Toast.LENGTH_SHORT).show();
                } else {
                    waitTime--;
                    SPHelper.save("SHOWTIME", waitTime);
                    Toast.makeText(mContext, "???????????????????????????" + waitTime + "???", Toast.LENGTH_SHORT).show();
                }
                timeTaskService.setShowTime(waitTime);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                // ????????????????????????????????????
                int waitTime2 = SPHelper.getInt("SHOWTIME", 10);
                waitTime2++;
                SPHelper.save("SHOWTIME", waitTime2);
                Toast.makeText(mContext, "???????????????????????????" + waitTime2 + "???", Toast.LENGTH_SHORT).show();
                timeTaskService.setShowTime(waitTime2);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // ?????????
                // ??????????????????
                currentPosition--;
                viewPager.setCurrentItem(currentPosition, true);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // ?????????
                // ??????????????????
                currentPosition++;
                viewPager.setCurrentItem(currentPosition, true);
                break;
            case KeyEvent.KEYCODE_MENU:
                // ?????????
                startApp();
                break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // ?????????enter
                // ???????????????????????????????????????????????????????????????
                timeTaskService.isPause = !timeTaskService.isPause;
                if (timeTaskService.isPause) {
                    Toast.makeText(mContext, "??????????????????", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "??????????????????", Toast.LENGTH_SHORT).show();
                }
                // ??????????????????
                if (mVideoView != null) {
                    if (timeTaskService.isPause) {
                        if (mVideoView.isPlaying()) {
                            mVideoView.pause();
                        }
                    } else {
                        // ????????????????????????????????????????????????
                        if (localList != null && localList.size() > 0 && localList.get(currentPosition % localList.size()).getType() == 2) {
                            mVideoView.start();
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                // ?????????
                exitApp();
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * ??????????????????APP
     */
    private void startApp() {
        PackageInfo packageinfo = null;
        try {
            packageinfo = getPackageManager().getPackageInfo("com.ottsz.robottv", 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            Toast.makeText(getApplicationContext(), "?????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent resolveIntent = getPackageManager().getLaunchIntentForPackage("com.ottsz.robottv");
        startActivity(resolveIntent);
    }

    /**
     * ???????????????????????????
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
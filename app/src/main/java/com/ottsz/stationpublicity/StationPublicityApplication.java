package com.ottsz.stationpublicity;

import android.app.Application;

import com.ottsz.stationpublicity.contentprovider.SPHelper;
import com.ottsz.stationpublicity.sqlite.DbHelper;

import xyz.doikki.videoplayer.exo.ExoMediaPlayerFactory;
import xyz.doikki.videoplayer.ijk.IjkPlayerFactory;
import xyz.doikki.videoplayer.player.AndroidMediaPlayerFactory;
import xyz.doikki.videoplayer.player.VideoViewConfig;
import xyz.doikki.videoplayer.player.VideoViewManager;

/**
 * Application类
 * Created by Li Yuliang on 2022/12/07.
 *
 * @author LiYuliang
 * @version 2022/12/07
 */

public class StationPublicityApplication extends Application {

    private static StationPublicityApplication instance;

    private DbHelper mDbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SPHelper.init(this);
        VideoViewManager.setConfig(VideoViewConfig.newBuilder()
                //使用使用IjkPlayer解码
                .setPlayerFactory(IjkPlayerFactory.create())
                //使用ExoPlayer解码
//                .setPlayerFactory(ExoMediaPlayerFactory.create())
                //使用MediaPlayer解码
//                .setPlayerFactory(AndroidMediaPlayerFactory.create())
                .build());
    }

    /**
     * 单例模式中获取唯一的MyApplication实例
     *
     * @return application实例
     */
    public static StationPublicityApplication getInstance() {
        if (instance == null) {
            instance = new StationPublicityApplication();
        }
        return instance;
    }

    public DbHelper getDbHelper() {
        if (mDbHelper == null) {
            mDbHelper = new DbHelper(this);
            mDbHelper.getDBHelper();
            mDbHelper.open();
        }
        return mDbHelper;
    }

}

package com.ottsz.stationpublicity.network.download;

/**
 * 下载服务接口
 * Created at 2022/9/28 15:20
 *
 * @author LiYuliang
 * @version 1.0
 */

public interface DownloadCallBack {

    void onProgress(int progress);

    void onCompleted();

    void onError(String msg);

}

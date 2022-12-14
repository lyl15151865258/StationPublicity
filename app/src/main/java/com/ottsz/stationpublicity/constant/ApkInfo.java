package com.ottsz.stationpublicity.constant;

import com.ottsz.stationpublicity.StationPublicityApplication;

/**
 * 软件信息类
 * Created at 2022/9/27 19:04
 *
 * @author LiYuliang
 * @version 1.0
 */

public class ApkInfo {

    // 软件类型
    public static final int APK_TYPE_ID_Robot = 3;

    // 文件路径
    public final static String APP_ROOT_PATH = StationPublicityApplication.getInstance().getExternalFilesDir(null).getAbsolutePath();
    public final static String DOWNLOAD_DIR = "/downlaod/";

}

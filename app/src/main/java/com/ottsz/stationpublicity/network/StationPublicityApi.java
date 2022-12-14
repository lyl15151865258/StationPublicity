package com.ottsz.stationpublicity.network;

import com.google.gson.JsonObject;
import com.ottsz.stationpublicity.bean.Result;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Retrofit网络请求构建接口
 * Created at 2018/11/28 13:48
 *
 * @author LiYuliang
 * @version 1.0
 */

public interface StationPublicityApi {

    /**
     * 查询所有展示资源
     */
    @POST("publicity/searchResources")
    Observable<Result> searchResources(@Body JsonObject params);

    /**
     * 查询最新的版本信息
     */
    @POST("version/searchNewVersion")
    Observable<Result> searchNewVersion(@Body JsonObject params);

    /**
     * 下载软件
     *
     * @return 文件
     */
    @Streaming
    @GET
    Observable<ResponseBody> executeDownload(@Header("Range") String range, @Url() String url);

}

package com.ottsz.stationpublicity.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ottsz.stationpublicity.BuildConfig;
import com.ottsz.stationpublicity.StationPublicityApplication;
import com.ottsz.stationpublicity.constant.NetWork;
import com.ottsz.stationpublicity.network.retrofit.SSLSocketFactoryCompat;
import com.ottsz.stationpublicity.util.LogUtils;
import com.ottsz.stationpublicity.util.NetworkUtil;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 请求接口URL
 * Created at 2018/11/28 13:48
 *
 * @author LiYuliang
 * @version 1.0
 */

public class NetClient {

    public static final String TAG = "NetClient";
    // NetClient复用对象（不带加解密、带加解密）
    private static NetClient mNetClient;
    private StationPublicityApi stationPublicityApi;
    private final Retrofit mRetrofit;
    private static String defaultUrl = "";
    private static final String CACHE_NAME = "NetCache";

    private NetClient(String baseUrl, boolean needCache) {

        // log用拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        // 开发模式记录整个body，否则只记录基本信息如返回200，http协议版本等
        if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        }
        //设置缓存目录
        File cacheFile = new File(StationPublicityApplication.getInstance().getExternalCacheDir(), CACHE_NAME);
        //生成缓存，50M
        Cache cache = new Cache(cacheFile, 1024 * 1024 * 50);
        //缓存拦截器
        Interceptor cacheInterceptor = (chain) -> {
            Request request = chain.request();
            if (NetworkUtil.isNetworkAvailable(StationPublicityApplication.getInstance())) {
                //网络可用,强制从网络获取数据
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .build();
            } else {
                //网络不可用,在请求头中加入：强制使用缓存，不访问网络
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();
            }
            Response response = chain.proceed(request);
            //网络可用
            if (NetworkUtil.isNetworkAvailable(StationPublicityApplication.getInstance())) {
                int maxAge = 60 * 60;
                // 有网络时 在响应头中加入：设置缓存超时时间1个小时
                response.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .removeHeader("pragma")
                        .build();
            } else {
                // 无网络时，在响应头中加入：设置超时为1周
                int maxStale = 60 * 60 * 24 * 7;
                response.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .removeHeader("pragma")
                        .build();
            }
            return response;
        };

        // OkHttpClient对象
        OkHttpClient okHttpClient;
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        // Android5.0以下支持SSL
        LogUtils.d(TAG, "设置SSL证书开始");
        setOkHttpSsl(builder);
        LogUtils.d(TAG, "设置SSL证书结束");

        if (needCache) {
            okHttpClient = builder
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(cacheInterceptor)
                    .cache(cache)
                    //设置超时时间
                    .connectTimeout(NetWork.TIME_OUT_HTTP, TimeUnit.SECONDS)
                    .readTimeout(NetWork.TIME_OUT_HTTP, TimeUnit.SECONDS)
                    .writeTimeout(NetWork.TIME_OUT_HTTP, TimeUnit.SECONDS)
                    //错误重连
                    .retryOnConnectionFailure(true)
                    .build();
        } else {
            okHttpClient = builder
                    .addInterceptor(loggingInterceptor)
                    //设置超时时间
                    .connectTimeout(NetWork.TIME_OUT_HTTP, TimeUnit.SECONDS)
                    .readTimeout(NetWork.TIME_OUT_HTTP, TimeUnit.SECONDS)
                    .writeTimeout(NetWork.TIME_OUT_HTTP, TimeUnit.SECONDS)
                    //错误重连
                    .retryOnConnectionFailure(true)
                    .build();
        }

        //设置Gson的非严格模式
        Gson gson = new GsonBuilder().setLenient().create();
        // 初始化Retrofit
        mRetrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(new StringConverterFactory())
                .addConverterFactory(new NullOnEmptyConverterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    /**
     * OkHttp在4.4及以下不支持TLS协议的解决方法
     * javax.net.ssl.SSLHandshakeException: javax.net.ssl.SSLProtocolException
     *
     * @param okhttpBuilder
     */
    private synchronized static void setOkHttpSsl(OkHttpClient.Builder okhttpBuilder) {
        try {
            // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
            final X509TrustManager trustAllCert =
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    };
            final SSLSocketFactory sslSocketFactory = new SSLSocketFactoryCompat(trustAllCert);
            okhttpBuilder.sslSocketFactory(sslSocketFactory, trustAllCert);
            LogUtils.d(TAG, "设置SSL证书成功");
        } catch (Exception e) {
            LogUtils.d(TAG, "设置SSL证书失败");
            throw new RuntimeException(e);
        }
    }

    public static class NullOnEmptyConverterFactory extends Converter.Factory {
        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            final Converter<ResponseBody, ?> delegate = retrofit.nextResponseBodyConverter(this, type, annotations);
            return (Converter<ResponseBody, Object>) body -> {
                if (body.contentLength() == 0) return null;
                return delegate.convert(body);
            };
        }
    }

    /**
     * 获取单例的NetClient对象
     *
     * @param baseUrl   基础Url
     * @param needCache 是否需要缓存
     * @return NetClient对象
     */
    public static synchronized NetClient getInstance(String baseUrl, boolean needCache) {
        if (mNetClient == null || !defaultUrl.equals(baseUrl)) {
            mNetClient = new NetClient(baseUrl, needCache);
            defaultUrl = baseUrl;
        }
        LogUtils.d(TAG, "请求接口：" + baseUrl);
        return mNetClient;
    }

    public StationPublicityApi getApi() {
        if (stationPublicityApi == null) {
            stationPublicityApi = mRetrofit.create(StationPublicityApi.class);
        }
        return stationPublicityApi;
    }

    /**
     * 主账号基础Url不带项目名（用于图像链接中）
     */
    public static String getBaseUrl() {
        return NetWork.SERVER_HOST_MAIN + ":" + NetWork.SERVER_PORT_MAIN + "/";
    }

}
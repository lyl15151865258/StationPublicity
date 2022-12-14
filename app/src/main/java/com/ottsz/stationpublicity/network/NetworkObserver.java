package com.ottsz.stationpublicity.network;

import android.content.Context;

import com.ottsz.stationpublicity.bean.Result;
import com.ottsz.stationpublicity.constant.ErrorCode;
import com.ottsz.stationpublicity.util.LogUtils;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 自定义Observer类,可以在这里处理client网络连接状况（比如没有wifi，没有4g，没有联网等）
 * Created at 2018/11/28 13:48
 *
 * @author LiYuliang
 * @version 1.0
 */

public abstract class NetworkObserver<T> implements Observer<T> {

    private static final String TAG = "NetworkObserver";
    private Context mContext;
    private Disposable disposable;

    public NetworkObserver(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void onSubscribe(Disposable d) {
        disposable = d;
        LogUtils.d(TAG, "NetworkObserver.onSubscribe()");
    }

    @Override
    public void onNext(T t) {
        if (t instanceof Result) {

            LogUtils.d(TAG, "NetworkObserver.onNext(),result:" + t);
            int code = ((Result) t).getCode();
            LogUtils.d(TAG, "本次请求的返回码为：" + code);

            String log = "";
            switch (code) {
                case ErrorCode.SUCCESS:
                    log = "请求成功";
                    break;
                default:
                    break;
            }
            LogUtils.d(TAG, log);
        }
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
        LogUtils.e("NetworkObserver.throwable =" + e.toString());
        LogUtils.e("NetworkObserver.throwable =" + e.getMessage());

        if (e instanceof Exception) {
            //访问获得对应的Exception
            onError(ExceptionHandle.handleException(e));
        } else {
            //将Throwable 和 未知错误的status code返回
            onError(new ExceptionHandle.ResponseThrowable(e, ExceptionHandle.ERROR.UNKNOWN));
        }
    }

    public abstract void onError(ExceptionHandle.ResponseThrowable responseThrowable);

    @Override
    public void onComplete() {
        LogUtils.d(TAG, "NetworkObserver.onComplete()");
    }

}

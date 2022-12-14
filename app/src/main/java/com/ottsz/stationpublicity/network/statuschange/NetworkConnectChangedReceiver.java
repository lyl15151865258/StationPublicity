package com.ottsz.stationpublicity.network.statuschange;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 网络变化广播接收器
 * Created at 2018/11/28 13:47
 *
 * @author LiYuliang
 * @version 1.0
 */

public class NetworkConnectChangedReceiver extends BroadcastReceiver {

    NetworkState networkState = new NetworkState();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager != null) {
                NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    if (activeNetworkInfo.isConnected()) {
                        networkState.setConnected(true);
                        //通知观察者网络状态已改变
                        NetworkChange.getInstance().notifyDataChange(networkState);
                        if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                            networkState.setWifi(true);
                            //通知观察者网络状态已改变
                            NetworkChange.getInstance().notifyDataChange(networkState);
                        } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                            networkState.setMobile(true);
                            //通知观察者网络状态已改变
                            NetworkChange.getInstance().notifyDataChange(networkState);
                        }
                    } else {
                        networkState.setConnected(false);
                        //通知观察者网络状态已改变
                        NetworkChange.getInstance().notifyDataChange(networkState);
                    }
                } else {
                    networkState.setWifi(false);
                    networkState.setMobile(false);
                    networkState.setConnected(false);
                    //通知观察者网络状态已改变
                    NetworkChange.getInstance().notifyDataChange(networkState);
                }
            }
        } else {
            networkState.setWifi(false);
            networkState.setMobile(false);
            networkState.setConnected(false);
            //通知观察者网络状态已改变
            NetworkChange.getInstance().notifyDataChange(networkState);
        }
    }
}

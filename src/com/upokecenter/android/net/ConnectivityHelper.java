package com.upokecenter.android.net;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.upokecenter.android.util.AppManager;

public final class ConnectivityHelper {

  Context application;

  ArrayList<ConnectionReceiver> connListeners=new ArrayList<ConnectionReceiver>();

  private static class ConnectionReceiver {
    WeakReference<Context> context;
    BroadcastReceiver receiver;
    IConnectionListener listener;
    public ConnectionReceiver(Context context,
        BroadcastReceiver receiver, IConnectionListener listener) {
      super();
      this.context = new WeakReference<Context>(context);
      this.receiver = receiver;
      this.listener = listener;
    }
    public boolean matches(Context context, IConnectionListener listener){
      return this.context.get().equals(context) &&
          this.listener==listener;
    }

  }

  public ConnectivityHelper(){
  }

  public void removeAllConnectionListeners(){
    for(int i=0;i<connListeners.size();i++){
      Context ctx=connListeners.get(i).context.get();
      if(ctx!=null) {
        ctx.unregisterReceiver(connListeners.get(i).receiver);
      }
    }
  }

  public void removeConnectionListener(Context ctx, IConnectionListener listener){
    for(int i=0;i<connListeners.size();i++){
      if(connListeners.get(i).matches(ctx,listener)){
        connListeners.remove(i);
        break;
      }
    }
  }

  public void addConnectionListener(
      Context context,
      final IConnectionListener listener){
    BroadcastReceiver receiver=new BroadcastReceiver(){
      @Override
      public void onReceive(Context ctx, Intent arg1) {
        listener.onConnectionChanged(ctx,getConnectedNetworkType());
      }
    };
    context.registerReceiver(receiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    connListeners.add(new ConnectionReceiver(context,receiver,listener));
    listener.onConnectionChanged(context,getConnectedNetworkType());
  }

  public static int getMobileNetworkType(){
    ConnectivityManager mgr=(ConnectivityManager)AppManager.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo network=mgr.getActiveNetworkInfo();
    if(network==null)return 0;
    if(network.isConnected())
      return network.getType();
    if(mgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected()){
      TelephonyManager tmgr=(TelephonyManager)AppManager.getApplication().getSystemService(Context.TELEPHONY_SERVICE);
      return tmgr.getNetworkType();
    }
    return 0;
  }

  public static int getConnectedNetworkType(){
    ConnectivityManager mgr=(ConnectivityManager)AppManager.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo network=mgr.getActiveNetworkInfo();
    if(network==null)return 0;
    if(network.isConnected())
      return network.getType();
    if(mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected())
      return ConnectivityManager.TYPE_WIFI;
    return 0;
  }
}

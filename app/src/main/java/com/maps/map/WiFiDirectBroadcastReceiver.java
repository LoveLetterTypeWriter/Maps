package com.maps.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.util.Log;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 * 
 * Adapted from:
 * https://android.googlesource.com/platform/development/+/master/samples/WiFiDirectServiceDiscovery/
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private Channel channel;
    private WifiBroadcastService service;
    private int oldDeviceStatus;

    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
            WifiBroadcastService service) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.service = service;
        this.oldDeviceStatus = 3;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        Log.d(WifiBroadcastService.TAG, action);
        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP
                Log.d(WifiBroadcastService.TAG, "Connected to p2p network. Requesting network details");
                manager.requestConnectionInfo(channel, (ConnectionInfoListener) service);
            } else {
                // It's a disconnect
            	Log.d(WifiBroadcastService.TAG, "Disconnected from p2p network.");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

        	/*
        	 * This isn't the most elegant way to do this, but if we lose our connection, then we should start trying
        	 * to discover services again.
        	 * 
        	 * See this link for info on what the statuses mean:
        	 * http://developer.android.com/reference/android/net/wifi/p2p/WifiP2pDevice.html
        	 * 
        	 * 0 means it is connected to another device
        	 * 3 means it is available for connection, as in, it is not connected to any device
        	 * Going from 0 to 3 should mean that we got disconnected.
        	 * 
        	 */
            WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            if(oldDeviceStatus == 0 && device.status==3) {
            	service.discoverService();
            }
            oldDeviceStatus = device.status;
            Log.d(WifiBroadcastService.TAG, "Device status -" + device.status);

        }
    }
}

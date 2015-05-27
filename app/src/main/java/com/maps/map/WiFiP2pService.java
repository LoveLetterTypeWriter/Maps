package com.maps.map;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * A structure to hold service information.
 * 
 * Adapted from:
 * https://android.googlesource.com/platform/development/+/master/samples/WiFiDirectServiceDiscovery/
 */
public class WiFiP2pService {
    WifiP2pDevice device;
    String instanceName = null;
    String serviceRegistrationType = null;
}

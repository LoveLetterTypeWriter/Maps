package com.maps.map;

import android.os.Handler;

/**
 * This class passes messages from the WifiBroadcastService to the MessageManager.
 * It is probably not needed, but is left in for simplicity's sake.
 * 
 * Adapted from:
 * https://android.googlesource.com/platform/development/+/master/samples/WiFiDirectServiceDiscovery/
 */
public class WifiMessageWriter {

    private MessageManager messageManager;
    
    public WifiMessageWriter(){}

    public interface MessageTarget {
        public Handler getHandler();
    }

    public void setMessageManager(MessageManager obj) {
        messageManager = obj;
    }
    
    public void write(byte[] message) {
    	messageManager.write(message);
    }
}

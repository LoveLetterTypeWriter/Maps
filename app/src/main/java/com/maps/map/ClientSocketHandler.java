package com.maps.map;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Connects the client to the owner of the Wifi Direct group.
 * 
 * Adapted from:
 * https://android.googlesource.com/platform/development/+/master/samples/WiFiDirectServiceDiscovery/
 * 
 */

public class ClientSocketHandler extends Thread {

    private static final String TAG = "ClientSocketHandler";
    private Handler handler;
    private MessageManager chat;
    private InetAddress mAddress;

    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(), WifiBroadcastService.SERVER_PORT), 5000);
            Log.d(TAG, "Launching the I/O handler");
            chat = new MessageManager(socket, handler);
            new Thread(chat).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }

    public MessageManager getChat() {
        return chat;
    }

}

package com.maps.map;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.maps.map.WiFiDirectBroadcastReceiver;
import com.maps.map.WifiMessageWriter.MessageTarget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This Service registers a local service and performs discovery over a Wi-Fi p2p
 * network. When the app is launched, the device publishes a service and also tries
 * to discover services published by other peers. On selecting a peer's published
 * service, the app initiates a Wi-Fi P2P (Direct) connection with the peer. On a
 * successful connection with a peer advertising the same service, the app opens up
 * sockets to send/receive emergency messages.
 * 
 * Because this is a service, there are no UI elements. It simply runs in the background
 * when the program has been started. You don't need to have the program open for this to work,
 * as long as it's started it will work.
 * 
 * Adapted from:
 * https://android.googlesource.com/platform/development/+/master/samples/WiFiDirectServiceDiscovery/
 */
public class WifiBroadcastService extends Service implements
        Handler.Callback, com.maps.map.WifiMessageWriter.MessageTarget,
        ConnectionInfoListener {

    public static final String TAG = "wifidirect";

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_emergencymap";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private WifiP2pManager manager;

    static final int SERVER_PORT = 4545;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;

    private Handler handler = new Handler(this);
    private WifiMessageWriter messageWriter;
    
    private Context mContext;
    
    private boolean connected = false;
    
    private Bundle queuedMessage;
    private ArrayList<WiFiP2pService> services;

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        
        mContext = getApplicationContext();
        
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        startRegistrationAndDiscovery();

        services = new ArrayList<WiFiP2pService>();
        
        registerReceiver(myReceiver, new IntentFilter("EMERGENCY.SEND"));
        
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);


    }
    
    /**
     * If the MapRouteActivity receives a new emergency, it should send the info
     * to this service using the Intent EMERGENCY.SEND. Once we receive the Intent,
     * we should determine if we are connected to another device or not. If so, we'll
     * pass the message along. If not, then we need to wait to connect and then send
     * the queued message.
     */
    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	
        	Bundle b = intent.getExtras();
    
        	if(b!=null) {
	        	if(!connected && !services.isEmpty()) {
        			connectP2p(services.get(0));
        			if(b.getString("message")!=null) {
        				queuedMessage = b;
        			}
	        	}
	        		
	        	if(connected && b.getString("message")!=null) {
    				String message = b.getString("message");
    				messageWriter.write(message.getBytes());
    				return;
        		}
    		}
        }
    };
    
    /**
     * Once we've connected to another device, we can send our queued message to it.
     */
    private void messageQueued() {
    	if(connected && queuedMessage != null) {
    		String message = queuedMessage.getString("message");
			messageWriter.write(message.getBytes());
			queuedMessage = null;
    	}
    }

    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        Log.d(TAG, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG, "Failed to add a service");
            }
        });

        discoverService();

    }

   void discoverService() {

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
	   
	   connected = false;

        manager.setDnsSdResponseListeners(channel,
                new DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                            String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?

                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                            // update the UI and add the item the discovered
                            // device.
                            WiFiP2pService service = new WiFiP2pService();
                            service.device = srcDevice;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;
                            services.add(service);
                            Log.d(TAG, "onBonjourServiceAvailable "
                                    + instanceName);
                        }

                    }
                }, new DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get(TXTRECORD_PROP_AVAILABLE));
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        
        manager.addServiceRequest(channel, serviceRequest, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Added service discovery request");
            }

            @Override
            public void onFailure(int arg0) {
            	Log.d(TAG, "Failed adding service discovery request");
            }
        });
        
        manager.discoverServices(channel, new ActionListener() {

            @Override
            public void onSuccess() {
            	Log.d(TAG, "Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
            	Log.d(TAG, "Service discovery failed. Reason: " + arg0);

            }
        });
    }
   
   /**
    * Try to connect to a service. The callback for manager.connect only tells you if
    * the initation of the connection was successful or not. It doesn't mean it's connected
    * just yet.
    */

    public void connectP2p(WiFiP2pService service) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if (serviceRequest != null) {
            manager.removeServiceRequest(channel, serviceRequest, new ActionListener() {

	            @Override
	            public void onSuccess() {
	            }
	
	            @Override
	            public void onFailure(int arg0) {
	            }
	        });
        }
        
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
            	Log.d(TAG, "Connecting to service");
            }

            @Override
            public void onFailure(int errorCode) {
            	Log.d(TAG, "Failed connecting to service");
            }
        });
    }
    
    /**
     * The other device will send its handle to you once you've fully connected.
     * At that point, you can send any queued messages you've received.
     */

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, "Received message " + readMessage);
                Intent in = new Intent();
                in.putExtra("message", readMessage);
                //this will open the application if it's not open already
                in.setClass(mContext, TabMainActivity.class);
                try {
    				PendingIntent.getActivity(mContext, (int) System.currentTimeMillis(), in, 0).send();
    			} catch (CanceledException e) {
    				// Auto-generated catch block
    				e.printStackTrace();
    			}
                break;

            case MY_HANDLE:
                Object obj = msg.obj;
                (messageWriter).setMessageManager((MessageManager) obj);
                Log.d(TAG, "Received device handle");
                connected = true;
                messageQueued();

        }
        return true;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */

        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(((MessageTarget) this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            handler = new ClientSocketHandler(((MessageTarget) this).getHandler(), p2pInfo.groupOwnerAddress);
            handler.start();
        }
        messageWriter = new WifiMessageWriter();
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}

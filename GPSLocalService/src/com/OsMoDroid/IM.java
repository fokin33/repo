
package com.OsMoDroid;import java.io.BufferedReader;import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;import java.io.InputStream;import java.io.InputStreamReader;import java.io.PrintWriter;import java.net.HttpURLConnection;import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;import java.net.Proxy;import java.net.URL;import java.net.UnknownHostException;import java.text.SimpleDateFormat;import java.util.ArrayList;import java.util.Collections;
import java.util.Map;
import java.util.Date;import java.util.Iterator;import java.util.Map.Entry;
import org.json.JSONArray;import org.json.JSONException;import org.json.JSONObject;import com.OsMoDroid.LocalService.SendCoor;import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;import android.app.PendingIntent;import android.app.PendingIntent.CanceledException;import android.content.BroadcastReceiver;import android.content.SharedPreferences;
import android.content.Context;import android.content.Intent;import android.content.IntentFilter;import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.NetworkInfo;import android.os.Bundle;import android.os.Environment;
import android.os.SystemClock;
import android.os.Handler;import android.os.Message;import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;import android.util.Log;import android.widget.Toast;import de.tavendo.autobahn.*;import de.tavendo.autobahn.Wamp.ConnectionHandler;
import de.tavendo.autobahn.Wamp.EventHandler;
/**
 * @author dfokin
 *Class for work with LongPolling
 */
public class IM {	private static final int RECONNECT_TIMEOUT = 1000*5;
	
	private static final int KEEP_ALIVE = 1000*270;

	private static final String RECONNECT_INTENT = "com.osmodroid.reconnect";
	
	private static final String KEEPALIVE_INTENT = "com.osmodroid.keepalive";

	protected  boolean running       = false;	//protected boolean connected     = false;	protected boolean autoReconnect = true;	protected Integer timeout       = 0;	private HttpURLConnection con;	private InputStream instream;	String adr;	String mykey;//	String timestamp=Long.toString(System.currentTimeMillis());String lcursor="";	int pingTimeout=900;	Thread myThread;	private BufferedReader    in      = null;	Context parent;	String myLongPollCh;
	ArrayList<String[]>  myLongPollChList;	//ArrayList<String> list= new ArrayList<String>();	ConnectionHandler c;

	WampOptions o;	int mestype=0;	LocalService localService;	
	FileOutputStream fos;
	ObjectOutputStream output = null;	final private static SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	
	private  WampConnection mConnection = new WampConnection();
	final String wsuri = "ws://osmo.mobi:5739/";

	protected boolean connOpened=false;	public IM(ArrayList<String[]> longPollChList, Context context,String key,final LocalService localService) {
	
		this.localService=localService; 
		parent=context;
		manager = (AlarmManager)(parent.getSystemService( Context.ALARM_SERVICE ));
		reconnectPIntent = PendingIntent.getBroadcast( parent, 0, new Intent(RECONNECT_INTENT), 0 );
		keepAlivePIntent = PendingIntent.getBroadcast( parent, 0, new Intent(KEEPALIVE_INTENT), 0 );
		myLongPollChList=longPollChList;
		mykey=key;
		//getadres(longPollChList);
		parent.registerReceiver(bcr, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

		c = new ConnectionHandler() {
			
			@Override
			public void onOpen() {
				connOpened=true;
				//ExceptionHandler.reportOnlyHandler(parent.getApplicationContext()).uncaughtException(Thread.currentThread(), new Throwable("websocket onopen"));
				Log.d(this.getClass().getName(), "websocket onopen, code=");
				localService.connect=true;
				localService.refresh();
				resubscribe();
				setkeepAliveAlarm();
			}
			
			@Override
			public void onClose(int code, String reason) {
				connOpened=false;
				//ExceptionHandler.reportOnlyHandler(parent.getApplicationContext()).uncaughtException(Thread.currentThread(), new Throwable("websocket onclose, code="+code+" reason="+reason));
				Log.d(this.getClass().getName(), "websocket onclose, code="+code+" reason="+reason);
				Log.d(this.getClass().getName(), "websocket onclose, isConnected="+mConnection.isConnected());
				localService.connect=false;
				localService.refresh();
				if(localService.isOnline()&&running){
					setReconnectAlarm();
				}
				disablekeepAliveAlarm();
				
			}
		};
		o = new WampOptions();
		o.setReconnectInterval(0);
		o.setReceiveTextMessagesRaw(true);
		start();
	}
	  BroadcastReceiver reconnectReceiver = new BroadcastReceiver() {
          @Override public void onReceive( Context context, Intent _ )
          {
              start();
              context.unregisterReceiver( this ); // this == BroadcastReceiver, not Activity
          }
      };
      BroadcastReceiver keepAliveReceiver = new BroadcastReceiver() {
          @Override public void onReceive( Context context, Intent _ )
          {
              if(mConnection!=null&&mConnection.isConnected()){
            	  Log.d(this.getClass().getName(), "websocket send ping");
            	  //ExceptionHandler.reportOnlyHandler(parent.getApplicationContext()).uncaughtException(Thread.currentThread(), new Throwable("websocket send ping"));
            	  mConnection.sendPing();
              }
          }
      };
      
      
      AlarmManager manager;
      PendingIntent reconnectPIntent;
      PendingIntent keepAlivePIntent;
	 
      public void setkeepAliveAlarm(){
    	  Log.d(this.getClass().getName(), "void setKeepAliveAlarm");
    	  parent.registerReceiver(keepAliveReceiver, new IntentFilter(KEEPALIVE_INTENT));
    	  manager.cancel(keepAlivePIntent);
    	  manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, KEEP_ALIVE, KEEP_ALIVE, keepAlivePIntent);
      }
      
      public void disablekeepAliveAlarm(){
    	  Log.d(this.getClass().getName(), "void disableKeepAliveAlarm");
    	  try {
			parent.unregisterReceiver(keepAliveReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	  manager.cancel(keepAlivePIntent);
      }
      
      public void setReconnectAlarm() 
	    {	
    	  Log.d(this.getClass().getName(), "void setReconnectAlarm");
    	  parent.registerReceiver( reconnectReceiver, new IntentFilter(RECONNECT_INTENT) );
    	  manager.cancel(reconnectPIntent);
    	  manager.set( AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + RECONNECT_TIMEOUT, reconnectPIntent );
	    }
	

	private void getadres(ArrayList<String[]> longPollChList) {
		//подключении не вставляется
		//http://d.esya.ru/?identifier=1364306035.30188078700000:somaanjUobpong&ncrnd=1364306032050
		//http://d.esya.ru/?identifier=somaanjUobpong&ncrnd=1364306032050
		
		adr="http://d.esya.ru/?identifier="+mykey;
		for (String str[] : longPollChList){
			if  (str[2].length()>0){
				adr=adr+","+str[2]+":"+str[0];
			}
			else {
			adr=adr+","+str[0];
			}
		}
		adr=adr+"&ncrnd="+Long.toString(System.currentTimeMillis());
		Log.d(this.getClass().getName(), "IM adr="+adr);
		Log.d(this.getClass().getName(), "try to save longPollChList");
		localService.saveObject(longPollChList, OsMoDroid.FILENAME);
		Log.d(this.getClass().getName(), "Success saved longPollChList");
		
	}
	
	public void addchannels(ArrayList<String[]> longPollChList){
		
		myLongPollChList.addAll(longPollChList);
		//getadres(myLongPollChList);
		resubscribe();
		
		
	}
	
public void removechannels(ArrayList<String[]> longPollChList){
	for (String[] extstr: longPollChList){
		Log.d(this.getClass().getName(), "extstr="+extstr[0]);
		 for (Iterator<String[]> iter = myLongPollChList.iterator(); iter.hasNext();) {
		      String[] s = iter.next();
		      if (s[0].equals(extstr[0])) {
		    	 Log.d(this.getClass().getName(), "myLongPollChList.count="+myLongPollChList.size());
		    	iter.remove();
		        Log.d(this.getClass().getName(), "iter removed");
		        Log.d(this.getClass().getName(), "myLongPollChList.count="+myLongPollChList.size());
		      }
		    }
	}
		//getadres(myLongPollChList);
	resubscribe();
	}
	String getMessageType(String ids){
	for (String[] str: myLongPollChList){
		if (str[0].equals(ids)){
			return str[1];
		}
	}
	
	return ids;
	
}			public void addtoDeviceChat(String message) {int u = -1;			try {
				MyMessage mes =new MyMessage( new JSONObject(message));
				Log.d(this.getClass().getName(), "MyMessage,from "+mes.from);
				Log.d(this.getClass().getName(), "DeviceList= "+LocalService.deviceList);
if (mes.from.equals(localService.settings.getString("device", ""))){
	for (Device dev : LocalService.deviceList){
		if(Integer.toString(dev.u).equals(mes.to)){
			u=dev.u;
		}
	}
} else {
				for (Device dev : LocalService.deviceList){
					if(Integer.toString(dev.u).equals(mes.from)){
						u=dev.u;
					}
				}
}		
				
				if (LocalService.currentDevice!=null&& u ==LocalService.currentDevice.u){
					boolean contains = false;
					for (MyMessage mes1: LocalService.chatmessagelist){
						
						
						if(mes1.u==mes.u){
							 contains = true;
							
						}
						
					}
					if (!contains){
						LocalService.chatmessagelist.add(mes);
						Collections.sort(LocalService.chatmessagelist);
						
						localService.alertHandler.post(new Runnable(){
							public void run() {
								if (LocalService.chatmessagesAdapter!=null){
									LocalService.chatmessagesAdapter.notifyDataSetChanged();
								}
							}
						});
						
						
					}
				
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (u!=-1){
			Message msg = new Message();			Bundle b = new Bundle();			b.putInt("deviceU", u);			msg.setData(b);			localService.alertHandler.sendMessage(msg);		}			}	private BroadcastReceiver bcr = new BroadcastReceiver() {		@Override		public void onReceive(Context context, Intent intent) {		//	Log.d(this.getClass().getName(), "BCR"+this);		//	Log.d(this.getClass().getName(), "BCR"+this+" Intent:"+intent);			if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {				Bundle extras = intent.getExtras();			//	Log.d(this.getClass().getName(), "BCR"+this+ " "+intent.getExtras());				if(extras.containsKey("networkInfo")) {					NetworkInfo netinfo = (NetworkInfo) extras.get("networkInfo");				//	Log.d(this.getClass().getName(), "BCR"+this+ " "+netinfo);				//	Log.d(this.getClass().getName(), "BCR"+this+ " "+netinfo.getType());					if(netinfo.isConnected()) {						Log.d(this.getClass().getName(), "BCR Network is connected");						Log.d(this.getClass().getName(), "Running:"+running);						// Network is connected						if(!running ) {							//SetAlarm();
							start();
						}											}					else {						Log.d(this.getClass().getName(), "BCR Network is not connected");						Log.d(this.getClass().getName(), "Running:"+running);						if (running)
						{							
							stop();
						}
											}				}				else if(extras.containsKey("noConnectivity")) {					Log.d(this.getClass().getName(), "BCR Network is noConnectivity");					Log.d(this.getClass().getName(), "Running:"+running);					if (running)
					{						
						stop();
					}
									}		    }		}	};

		 /**
	 * Выключает IM
	 */
	void close(){		Log.d(this.getClass().getName(), "void IM.close");
		try {
			parent.unregisterReceiver(bcr);
		} catch (Exception e) {
		
			e.printStackTrace();
		}
				stop();	};	 void start(){		Log.d(this.getClass().getName(), "void IM.start");
		running = true;		mConnection.connect(wsuri, c, o);
				  }void parseEx (String toParse, String topic){

//	[
//	  {
//	    "ids": { "om_omc_RAzHkQ9JzY5_chat": "1364554177.56155052100000" },
//	    "data": "0|40|fgfg|2013-03-29 14:49:37"
//	  }
//	]
	
	
	try {
		Log.d(this.getClass().getName(), "toParse= "+ toParse+ " topic="+topic);
	
		Log.d(this.getClass().getName(), "topic type="+getMessageType( topic));
if (getMessageType( topic).equals("o")){
	Log.d(this.getClass().getName(), "type=o");
	String status = "";
	String messageText = "";
    String[] data = toParse.split("-");
    Log.d(this.getClass().getName(), "data[0]="+data[0]+" data[1]="+data[1]+" data[2]="+data[2]);
    Log.d(this.getClass().getName(), "LocalService.DeviceList="+LocalService.deviceList.toString());
    final ArrayList<Device> tempdeviceList = new ArrayList<Device>();
    tempdeviceList.addAll(LocalService.deviceList);
    
    for (Device device : tempdeviceList){
        if (data[1].equals(Integer.toString(device.u))&&device.u!=(Integer.parseInt(LocalService.settings.getString("device", ""))) ){
        	//LocalService.deviceList.remove(device);
        	int idx=LocalService.deviceList.indexOf(device);
            if (LocalService.settings.getBoolean("statenotify", true)&&data[0].equals("state")) {
                if (data[2].equals("1")) { status=localService.getString(R.string.started);device.state = "1"; } else { status=localService.getString(R.string.stoped);device.state = "0"; }
                messageText = messageText+localService.getString(R.string.monitoringondevice)+device.name+"\" "+status;
                Log.d(this.getClass().getName(), "DeviceState="+messageText);
                
            }
            if (LocalService.settings.getBoolean("onlinenotify", false)&&data[0].equals("online")&&device.u!=(Integer.parseInt(LocalService.settings.getString("device", ""))) ){
                if (data[2].equals("1")&&device.online.equals("0")) { status=localService.getString(R.string.enternet);  device.online = "1";}
                if (data[2].equals("0")&&device.online.equals("1")) { status=localService.getString(R.string.exitnet); device.online = "0"; }
               
                messageText = messageText+localService.getString(R.string.device)+device.name+"\" "+status;
                Log.d(this.getClass().getName(), "DeviceOnline="+messageText);
            }
            if (data[0].equals("geozone")&&device.u!=(Integer.parseInt(LocalService.settings.getString("device", ""))) ){
                messageText = messageText+localService.getString(R.string.device)+device.name+"\" "+data[2];
                Log.d(this.getClass().getName(), "DeviceGeoZone="+messageText);
            }
            localService.saveObject(LocalService.deviceList, OsMoDroid.DEVLIST);
            if (LocalService.deviceAdapter!=null) { 
            //	LocalService.deviceAdapter.notifyDataSetChanged();
            	Log.d(getClass().getSimpleName(),"devicelist="+ LocalService.deviceList);
            	
            	localService.alertHandler.post(new Runnable() {

					public void run() {

						LocalService.deviceList.clear();
		            	LocalService.deviceAdapter.notifyDataSetChanged();
		            	LocalService.deviceList.addAll(tempdeviceList);
		            	LocalService.deviceAdapter.notifyDataSetChanged();}
				});
            	
            	if (!messageText.equals("")){
            	Message msg = new Message();

    			Bundle b = new Bundle();

    			b.putBoolean("om_online", true);
    			b.putString("MessageText", sdf1.format(new Date())+" "+messageText);

    			msg.setData(b);

    			localService.alertHandler.sendMessage(msg);
    			Log.d(this.getClass().getName(), "Sended message:"+messageText);
            	}
            	//lv1.setAdapter(LocalService.deviceAdapter);
            	} else { Log.d(this.getClass().getName(), "deviceadapter is null");}
        }
    }
	
}



if (getMessageType( topic).equals("m")){
	
	Log.d(this.getClass().getName(), "type=m");
	
	  String messageJSONText = toParse;
	  
	  if (!messageJSONText.equals("")){

			addtoDeviceChat(messageJSONText);

			}
}

if (getMessageType( topic).equals("r")){
	Log.d(this.getClass().getName(), "type=r");
	if(topic.equals("om_check_"+OsMoDroid.settings.getString("device", ""))){
		 netutil.newapicommand((ResultsListener)localService, "om_checker:"+toParse);
		 return;
	}
	
	if (toParse.equals("start")){
        if (!localService.state){
        	localService.alertHandler.post(new Runnable() {

				public void run() {

        	localService.startServiceWork();
            try {
                localService.Pong(localService);
            } catch (JSONException e) {
                e.printStackTrace();
            }}});
        }
}

if (toParse.equals("stop")){
        if (localService.state){
        	localService.alertHandler.post(new Runnable() {

				public void run() {

            localService.stopServiceWork(true);
            try {
                localService.Pong(localService);
            } catch (JSONException e) {
                e.printStackTrace();
            }}});
        }
}

if (toParse.equals("ping")){
        try {
            localService.Pong(localService);
        } catch (JSONException e) {
            e.printStackTrace();
        }
}

if (toParse.length()>7&&toParse.substring(0, 7).equals("routeto")){
	int pluspos=toParse.lastIndexOf("_");
	localService.informRemoteClientRouteTo(Float.parseFloat(toParse.substring(8, pluspos)), Float.parseFloat(toParse.substring(pluspos+1, toParse.length())));
}

if (toParse.length()>7&&toParse.substring(0, 7).equals("vibrate")){
	localService.vibrate( localService, Long.parseLong(toParse.substring(7, toParse.length())));
}



if (toParse.equals("batteryinfo")){
        try {
            localService.batteryinfo(localService);
        } catch (JSONException e) {
            e.printStackTrace();
        }
}

if (toParse.equals("systeminfo")){
    try {
        localService.systeminfo(localService);
    } catch (JSONException e) {
        e.printStackTrace();
    }
}


if (toParse.equals("satellite")){
    try {
        localService.satelliteinfo(localService);
    } catch (JSONException e) {
        e.printStackTrace();
    }
}

if (toParse.equals("wifiinfo")){
    try {
        localService.wifiinfo(localService);
    } catch (JSONException e) {
        e.printStackTrace();
    }
}


if (toParse.equals("closeclient")){
	localService.alertHandler.post(new Runnable() {

		public void run() {
localService.stopServiceWork(false);
localService.stopSelf();
			
    try {
        localService.Pong(localService);
    } catch (JSONException e) {
        e.printStackTrace();
    }}});
             
}

if (toParse.equals("getpreferences")){
	try {
		localService.getpreferences(localService);
		} catch (JSONException e) {
			e.printStackTrace();
			}
		}
if (toParse.equals("wifion")){

localService.wifion(localService);

}
if (toParse.equals("wifioff")){

localService.wifioff(localService);

}

if (toParse.equals("signalon")){

localService.enableSignalisation(true);

}

if (toParse.equals("signaloff")){

localService.disableSignalisation(true);

}

if (toParse.equals("alarmon")){

localService.playAlarmOn(true);

}

if (toParse.equals("alarmoff")){

localService.playAlarmOff(true);

}





if (toParse.equals("where")){
localService.mayak=true;
if (!localService.state){
	localService.alertHandler.post(new Runnable() {

		public void run() {
	localService.myManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, localService);
	 Log.d(this.getClass().getName(), "подписались на GPS");
		}
	});
	}
	localService.alertHandler.postDelayed(new Runnable() {

		public void run() {
	localService.myManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, localService);
	 Log.d(this.getClass().getName(), "подписались на NETWORK");
		}
	},30000);
	localService.alertHandler.postDelayed(new Runnable() {

		public void run() {
	if (localService.state){
			localService.myManager.removeUpdates(localService);
			localService.requestLocationUpdates();
			 Log.d(this.getClass().getName(), "Переподписались");
	} else {
		localService.myManager.removeUpdates(localService);
		 Log.d(this.getClass().getName(), "Отписались");
	}
		}
	},90000);
	  localService.Pong(localService);


}



	if (toParse.substring(0, 4).equals("play")){
		 MediaPlayer mp = new MediaPlayer();
		 String mp3Path = Environment.getExternalStorageDirectory()
				 .getAbsolutePath()+ "/OsMoDroid/mp3";
		 Log.d(this.getClass().getName(), "try play:"+(mp3Path+"/"+toParse.substring(4, toParse.length())+".mp3"));
		    try {
		        mp.setDataSource(mp3Path+"/"+toParse.substring(4, toParse.length())+".mp3");
		    } catch (IllegalArgumentException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    } catch (IllegalStateException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
		    try {
		        mp.prepare();
		    } catch (IllegalStateException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
		    mp.start();
	}





Object item = new JSONObject(toParse);
Log.d(this.getClass().getName(), "item="+item+" "+item.getClass()+ " JSONObject is " + (item instanceof JSONObject) );

if (item instanceof JSONObject){

Log.d(this.getClass().getName(), "set preference");
JSONObject jo = (JSONObject)item;
Iterator<String> locIt = jo.keys();
SharedPreferences.Editor editor = LocalService.settings.edit();
Map<String, ?> entries = LocalService.settings.getAll();
while (locIt.hasNext()){
	String lockeyname= locIt.next();
	 for (Entry<String, ?> entry : entries.entrySet()) {
         Object v = entry.getValue();
         String key = entry.getKey();
if (key.equals(lockeyname)){
         if (v instanceof Boolean)
        	 editor.putBoolean(key,jo.optString(lockeyname).equalsIgnoreCase("1")   );
         else if (v instanceof Float)
        	 editor.putFloat(key, Float.parseFloat(jo.optString(lockeyname)));
         else if (v instanceof Integer)
        	 editor.putInt(key, Integer.parseInt(jo.optString(lockeyname)));
         else if (v instanceof Long)
        	 editor.putLong(key, Long.parseLong(jo.optString(lockeyname)));
         else if (v instanceof String)
        	 editor.putString(key, jo.optString(lockeyname));
     }
	 }
	
	
}
editor.commit();
localService.alertHandler.post(new Runnable() {

	public void run() {

		localService.applyPreference();
		
try {
    localService.Pong(localService);
} catch (JSONException e) {
    e.printStackTrace();
}}});

}	

	
	
}

if (getMessageType( topic).equals("ch")){
	Log.d(this.getClass().getName(), "type=ch");
	Log.d(this.getClass().getName(), "Изменилось состояние в канале " + toParse);
	// 02-24 10:03:31.127: D/IM(562): "data":
	//  "data": "0|1436|55.307453|39.232767|0"
	//09-17 17:55:53.122: D/com.OsMoDroid.IM(877):     "data": "0|3084+37.416667+-122.083332+0"
	String[] data = toParse.split("\\|");
	String[] datanew = data[1].split("\\+");
	//Log.d(this.getClass().getName(), "data[0]=" + data[0] + " data[1]=" + data[1] + " data[2]=" + data[2]+" data[3]="+data[3]+" data[4]="+data[4]);
	if(data[0].equals("0"))
	{
		for (Channel channel : LocalService.channelList)
		{
			if (channel.ch.equals(topic))
			{
			Log.d(this.getClass().getName(), "chanal nest" + channel.name);
			for (Device device : channel.deviceList) {
				Log.d(this.getClass().getName(), "device nest" + device.name + " " + device.u);
				if (datanew[0].equals(Integer.toString(device.u))) {
					Log.d(this.getClass().getName(), "Изменилось состояние устройства в канале с " + device.toString());
					device.lat = Float.parseFloat(datanew[1]);
					device.lon = Float.parseFloat(datanew[2]);
					device.speed= datanew[3];
					Log.d(this.getClass().getName(), "Изменилось состояние устройства в канале на" + device.toString());
					if(LocalService.devlistener!=null){LocalService.devlistener.onDeviceChange(device);}
				}

			}

		}
	}
		
		localService.informRemoteClientChannelUpdate();
		localService.alertHandler.post(new Runnable() {
			public void run() {
				if (LocalService.channelsDevicesAdapter != null && LocalService.currentChannel != null) {
					Log.d(this.getClass().getName(), "Adapter:" + LocalService.channelsDevicesAdapter.toString());
					for (Channel channel : LocalService.channelList) {
						if (channel.u==LocalService.currentChannel.u) {
							LocalService.currentchanneldeviceList.clear();
							LocalService.currentchanneldeviceList.addAll(channel.deviceList);
						}
						
					}
					
					LocalService.channelsDevicesAdapter.notifyDataSetChanged();
				}
			}
		});
	}
	else {
		netutil.newapicommand((ResultsListener)localService, "om_device_channel_adaptive:"+localService.settings.getString("device", ""));
	}
}

if (getMessageType( topic).equals("chch")){
	Log.d(this.getClass().getName(), "type=chch");
	Log.d(this.getClass().getName(), "Сообщение в чат канала " + toParse);
	//09-16 18:25:41.057: D/com.OsMoDroid.IM(1474):     "data": "0|40+\u041e\u043f\u0430\u0441\u043d\u043e +2013-09-16 22:25:44"
	//"data": "0|40|cxbcxvbcxvbcxvb|2013-03-14 22:42:34"
	String[] data = toParse.split("\\|");
	//Log.d(this.getClass().getName(), "data[0]=" + data[0] + " data[1]=" + data[1] + " data[2]=" + data[2]);
	String[] datanew = data[1].split("\\+");
	//Log.d(this.getClass().getName(), "datanew[0]=" + datanew[0] + " datanew[1]=" + datanew[1] + " datanew[2]=" + datanew[2]);
	for (final Channel channel : LocalService.channelList) {
		Log.d(this.getClass().getName(), "chanal nest" + channel.name);
		if (topic.equals(channel.ch+"_chat")){
			
		
		
		for (Device device : channel.deviceList) {
			Log.d(this.getClass().getName(), "device nest" + device.name + " " + device.u);
			if (datanew[0].equals(Integer.toString(device.u))) {
				Log.d(this.getClass().getName(), "Сообщение от устройства в канале " + device.toString());
			}
		}
				channel.messagesstringList.clear();
				channel.messagesstringList.add(datanew[1]);
				localService.alertHandler.post(new Runnable(){
					public void run() {
						if (LocalService.channelsmessagesAdapter!=null&& LocalService.currentChannel != null){
							LocalService.currentChannel.messagesstringList.addAll(channel.messagesstringList);
							LocalService.channelsmessagesAdapter.notifyDataSetChanged();
						}
					}
				});
		}
	}
}
		
	} catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}	 void stop (){
		 Log.d(this.getClass().getName(), "void IM.stop");
		 running = false;
		 manager.cancel(reconnectPIntent);		 if(mConnection.isConnected()){mConnection.disconnect();}
		//mConnection=null;
			}

	 void resubscribe() {
		 
		 if(mConnection!=null&&connOpened){
			 mConnection.unsubscribe();
			 Log.d(this.getClass().getName(), "websocket unsubscribe all");
		for (String str[] : myLongPollChList){
//			if(str[1].equals("r")){
//				mConnection.publish(str[0], OsMoDroid.settings.getString("hash", ""));
//			}
			mConnection.subscribe(str[0], String.class, new EventHandler() {
				
				@Override
				public void onEvent(String topic, Object event) {
					 Log.d(this.getClass().getName(), "websocket event:"+((String) event)+" topic:"+topic);
					parseEx((String) event, topic);
				}
			});
			   Log.d(this.getClass().getName(), "websocket subscribe:"+str[0]);
            }
		
				
		
		}
	}}

package com.OsMoDroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class IM {
	protected boolean running       = false;
	protected boolean connected     = false;
	protected boolean autoReconnect = true;
	protected Integer timeout       = 0;
	private HttpURLConnection con;
	String adr;
	Long timestamp=System.currentTimeMillis();
	int pingTimeout=900;
	Thread myThread;
	private BufferedReader    in      = null;
	Context parent;
	String mychanel;
	ArrayList<String> list= new ArrayList<String>();
	
	
	final Handler alertHandler = new Handler() {
		@Override
		public void handleMessage(Message message) {
		Bundle b = message.getData();
		String text = b.getString("MessageText");
		Toast.makeText(parent, text, Toast.LENGTH_SHORT).show();
		list.add(0,text);
		Log.d(this.getClass().getName(), "List:"+list);
		 Bundle a=new Bundle();                                
         a.putStringArrayList("meslist", list);
     Intent activ=new Intent(parent,  mesActivity.class);
     activ.putExtras(a);
     activ.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP	| Intent.FLAG_ACTIVITY_SINGLE_TOP);
     PendingIntent contentIntent = PendingIntent.getActivity(parent, OsMoDroid.notifyidApp(),activ, 0);
   
     NotificationCompat.Builder nb = new NotificationCompat.Builder(parent)
		.setSmallIcon(android.R.drawable.ic_menu_send).setAutoCancel(true)
		.setTicker("���������").setContentText(text)
		.setWhen(System.currentTimeMillis())
		.setContentTitle("���������")
		.setDefaults(Notification.DEFAULT_ALL);

		Notification notification = nb.build();// getNotification();
		//notification.flags |= Notification.FLAG_INSISTENT;
		
		
		
		notification.setLatestEventInfo(parent, "OsMoDroid",	"", contentIntent);
		LocalService.mNotificationManager.notify(OsMoDroid.mesnotifyid, notification);    

		 if (OsMoDroid.activityVisible) {
    		try {
			
			contentIntent.send(parent, 0, activ);
			LocalService.mNotificationManager.cancel(OsMoDroid.mesnotifyid);
			
		} catch (CanceledException e) {
			Log.d(this.getClass().getName(), "pending intent exception"+e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
     
     
		
		
		
		}
		};
	
		public void showToastMessage(String message) {
			Message msg = new Message();
			Bundle b = new Bundle();
			b.putString("MessageText", message);
			msg.setData(b);
			alertHandler.sendMessage(msg);
			}	
		
	
	private BroadcastReceiver bcr = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
				Bundle extras = intent.getExtras();
				if(extras.containsKey("networkInfo")) {
					NetworkInfo netinfo = (NetworkInfo) extras.get("networkInfo");
					if(netinfo.isConnected()) {
						// Network is connected
						if(!running ) {
							System.out.println("Starting from Reciever"+this.toString());
							start();
						}
					}
					else {
						stop();
					}
				}
				else if(extras.containsKey("noConnectivity")) {
					stop();
				}
		    }
		}
	};
	
	void close(){
		
		parent.unregisterReceiver(bcr);
		stop();
	};
	void start(){
		this.running = true;
		System.out.println("About to notify state from start()");
		System.out.println("State notifed of start()");
		myThread = new Thread(new IMRunnable());
		myThread.start();
	}
	
	public IM(String channel, Context context) {
		adr="http://d.esya.ru/?identifier="+channel+",im_messages&ncrnd="+timestamp;
		this.parent=context;
		mychanel=channel;
		parent.registerReceiver(bcr, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
		start();
		
		
		
	}
	
	void parse (String toParse){
		try {
		
		String messageText="";
			
			Log.d(this.getClass().getName(), "IM toParse:"+toParse);
			JSONArray result = new JSONArray(toParse);
		
		
			for (int i = 0; i < result.length(); i++) {
		        JSONObject jsonObject = result.getJSONObject(i);
		        
		        messageText=messageText + (new  JSONObject(jsonObject.optString("data")).optString("from_name"))+":"  + new JSONObject(jsonObject.optString("data")).optString("text") +"\n"  ;
		        Iterator it = (new JSONObject(jsonObject.optString("ids"))).keys();
				  while (it.hasNext())
	          	{
	          		
	String keyname= (String)it.next();          		 
	Log.d(getClass().getSimpleName(),(new JSONObject(jsonObject.optString("ids"))).optString(keyname));
	 
	adr="http://d.esya.ru/?identifier="+mychanel+",im_messages&ncrnd="+(new JSONObject(jsonObject.optString("ids"))).optString(keyname);
	
	
	          	}
			}
			Log.d(getClass().getSimpleName(),adr);
			showToastMessage(messageText);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(this.getClass().getName(), e.toString());
		}
		
	}
	void stop (){
		try {
			
			in.close();
			con.disconnect();
			} catch (Exception e) {
				
				e.printStackTrace();
			}
			running = false;
	}
	
	
	private class IMRunnable implements Runnable {
		
		StringBuilder stringBuilder= new StringBuilder();
		private InputStreamReader stream  = null;
		
		private boolean           error   = false;
		private int               retries = 0;
		int maxRetries = 100;
		
		
		
		public void run() {
			String response = "";
			while (running) {
				try {
					++retries;
					int portOfProxy = android.net.Proxy.getDefaultPort();
					if (portOfProxy > 0) {
						Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
								android.net.Proxy.getDefaultHost(), portOfProxy));
						con = (HttpURLConnection) new URL(adr).openConnection(proxy);
					} else {
						con = (HttpURLConnection) new URL(adr).openConnection();
					}
					con.setReadTimeout(pingTimeout*1000);
					con.setConnectTimeout(10000);
					
					
					
					stream = new InputStreamReader(con.getInputStream());
					in   = new BufferedReader(stream, 1024);
					if(error){
						error = false;
					}

					// Set a timeout on the socket
					// This prevents getting stuck in readline() if the pipe breaks
					

					retries = 0;
					connected = true;
					

				} catch (UnknownHostException e) {
					error = true;
					
					//stop();
				} catch (Exception e) {
					error = true;
					
					//stop();
				}
				if(retries > maxRetries) {
					stop ();
					break;
				}
				
				
				if(retries > 0) {
					try {
						Thread.sleep(1000*10);
					} catch (InterruptedException e) {
						// If can't back-off, stop trying
						
						running = false;
						break;
					}
				}
				
				
				if(!error && running) {
					
					

					try {
						// Wait for a response from the channel
						
						stringBuilder.setLength(0);
					 	
						    int c = 0;
						    int i=0;
						  
						    while (!(c==-1) && running) {
						    	
						    	c = in.read();
						        if (!(c==-1))stringBuilder.append((char) c);
						        i=i+1;
						    }

						
					//return chbuf.toString();
						    //Log.d(this.getClass().getName(), "void input end");
						    parse( stringBuilder.toString());
						
						
						
//						while(running && (response=in. .read()) != null) {
//							// Got a response
//							//killConnection.cancel();
//							parse(response);
//						}
						
						in.close();
						con.disconnect();
					}
					catch(IOException e) {
						error = true;
						
							//stop();
						}
					}
				
				else {
					// An error was encountered when trying to connect
					connected = false;
				}
				
				
			}
			
		}
		
	}
	
	
	

}

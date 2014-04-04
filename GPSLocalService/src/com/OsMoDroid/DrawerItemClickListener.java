package com.OsMoDroid;



import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class DrawerItemClickListener implements OnItemClickListener {

	 MainFragment main;
	 StatFragment stat;
	 MapFragment map;
	 ChannelsFragment chans;
	 DevicesFragment devs;
	 SimLinksFragment links;
	 NotifFragment notif;
	 TracFileListFragment trac;
	 DeviceChatFragment devchat;
	 ChannelDevicesFragment chandev;
	 int currentItem=0;
	 
	 GPSLocalServiceClient globalActivity;
	
	 
	 
	 @Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		//selectItem(arg2);
		LocalService.currentItemName=(String)arg0.getAdapter().getItem(arg2);
		selectItem(LocalService.currentItemName,null);
	}
	public void selectItem(String name, Bundle bundle) {
		 
        final FragmentTransaction ft = GPSLocalServiceClient.fMan.beginTransaction();
      
//getString(R.string.tracker), getString(R.string.stat),getString(R.string.map),getString(R.string.chanals)
//getString(R.string.devices),getString(R.string.links), getString(R.string.notifications), getString(R.string.tracks)            
	 
        // Locate Position
       if(name.equals(OsMoDroid.context.getString(R.string.tracker))){
    	   if(main==null){ 
           	main=new MainFragment();
           	}
               ft.replace(R.id.fragment_container, main);
               currentItem=0;
       }
       
       else if(name.equals( OsMoDroid.context.getString(R.string.stat))){
    	   if(stat==null){ 
           	stat=new StatFragment();
           	}
               ft.replace(R.id.fragment_container, stat);
               currentItem=1;
       }
       
       else if(name.equals( OsMoDroid.context.getString(R.string.map))){
    	   if(map==null){
      		 map=new MapFragment();
      	 }
          ft.replace(R.id.fragment_container, map);
          currentItem=2;
       }
       
       else if(name.equals( OsMoDroid.context.getString(R.string.chanals))){
    	   if(chans==null){ 
           	chans=new ChannelsFragment();
           	}
    	   if(bundle!=null){
    		   chans.channelpos=bundle.getInt("channelpos",-1);
    		   
    	   }
               ft.replace(R.id.fragment_container, chans);
               currentItem=3;
       }
       
       else if(name.equals( OsMoDroid.context.getString(R.string.devices))){
    	   if(devs==null){ 
           	devs=new DevicesFragment();
           	}
    	   if(bundle!=null){
    		   
    		   //devs.setArguments(bundle.geti);
    		   devs.deviceU=bundle.getInt("deviceU",-1);
    	   }
               ft.replace(R.id.fragment_container, devs);
               currentItem=4;
       }
       
       else if(name.equals( OsMoDroid.context.getString(R.string.links))){
    	   if(links==null){ 
           	links=new SimLinksFragment();
           	}
               ft.replace(R.id.fragment_container, links);
               currentItem=5;
       }

       else if(name.equals( OsMoDroid.context.getString(R.string.notifications))){
    	   if(notif==null){
           	notif=new NotifFragment();
           	}
               ft.replace(R.id.fragment_container, notif);
               currentItem=6;
       }

       else if(name.equals( OsMoDroid.context.getString(R.string.tracks))){
    	   if(trac==null){ 
           	trac=new TracFileListFragment();
           	}
               ft.replace(R.id.fragment_container, trac);
               currentItem=7;
       }
       else if(name.equals( OsMoDroid.context.getString(R.string.exit))){
    	   Intent i = new Intent(globalActivity, LocalService.class);
           globalActivity.stopService(i);
           globalActivity.finish();
       } 
        
       
        
        GPSLocalServiceClient.fMan.popBackStack();
        //ft.addToBackStack("").commit();
        GPSLocalServiceClient.mDrawerList.setItemChecked(currentItem, true);
        GPSLocalServiceClient.mDrawerLayout.closeDrawer(GPSLocalServiceClient.mDrawerList);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            	 ft.commit();
            }
        }, 100);
       
        
        //setTitle(myfriendname[position]);
       
    }

}
package com.OsMoDroid;

import java.util.ArrayList;

import org.osmdroid.ResourceProxy;
import org.osmdroid.ResourceProxy.string;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

public class MapFragment extends SherlockFragment implements DeviceChange, IMyLocationProvider,LocationListener {
	 	ResourceProxyImpl mResourceProxy;
		MapView mMapView;
		private IMapController mController;
		private MyLocationNewOverlay myLoc;
		private PathOverlay myTracePathOverlay;
		private GPSLocalServiceClient globalActivity;
		//private View view;
		boolean rotate=false;
		//private Context context;
		//ArrayList<PathOverlay> paths = new ArrayList<PathOverlay>();
		private IMyLocationConsumer myLocationConumer;
		private long lastgpslocation=0;
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			
			super.onCreateOptionsMenu(menu, inflater);
		}



		
		
	@Override
     public void onCreate(Bundle savedInstanceState) {
		Log.d(getClass().getSimpleName(), "map oncreate"); 
		super.onCreate(savedInstanceState);
         setHasOptionsMenu(true);
         //setRetainInstance(true);
         LocalService.devlistener=this;
         super.onCreate(savedInstanceState);
     }
	
	
	
	@Override
	public void onAttach(Activity activity) {
		Log.d(getClass().getSimpleName(), "map onattach");	
		globalActivity = (GPSLocalServiceClient)activity;
		
		super.onAttach(activity);
	}
	
	@Override
	public void onDetach() {
		LocalService.devlistener=null;
		mResourceProxy=null;
		mMapView=null;
		mController=null;
		myLoc.disableMyLocation();
		myLoc=null;
		myTracePathOverlay=null;
		globalActivity=null;
		Log.d(getClass().getSimpleName(), "map ondetach");
		super.onDetach();
	}
	@Override
	public void onDestroyView() {
		Log.d(getClass().getSimpleName(), "map ondestroyview");
		OsMoDroid.editor.putInt("centerlat", mMapView.getMapCenter().getLatitudeE6());
		OsMoDroid.editor.putInt("centerlon", mMapView.getMapCenter().getLongitudeE6());
		OsMoDroid.editor.putInt("zoom", mMapView.getZoomLevel());
		OsMoDroid.editor.putBoolean("isfollow", myLoc.isFollowLocationEnabled());
		OsMoDroid.editor.commit();
		//ch.map=null;
	
		super.onDestroyView();
	}
	class MAPSurferTileSource extends OnlineTileSourceBase {

		MAPSurferTileSource(String aName, string aResourceId, int aZoomMinLevel,
                        int aZoomMaxLevel, int aTileSizePixels,
                        String aImageFilenameEnding, String... aBaseUrl) {
                super(aName, aResourceId, aZoomMinLevel, aZoomMaxLevel,
                                aTileSizePixels, aImageFilenameEnding, aBaseUrl);
        }
		@Override
		public String getTileURLString(MapTile aTile) {
		//	x=710&y=381&z=10
			return getBaseUrl() + "x="+ aTile.getX() + "&y="
                    + aTile.getY()+ "&z="+aTile.getZoomLevel() ;
			
		}
}



	@Override
	public void onDestroy() {
		
		Log.d(getClass().getSimpleName(), "map ondestroy");
		super.onDestroy();
	}





	@Override
	public void onPause() {
		Log.d(getClass().getSimpleName(), "map onpause");
		mMapView.getOverlays().remove(myLoc);
		myLoc.disableMyLocation();
		
		super.onPause();
	}





	@Override
	public void onResume() {
		Log.d(getClass().getSimpleName(), "map onResume");
		globalActivity.actionBar.setTitle(getString(R.string.map));
		mMapView.getOverlays().add(myLoc);
		myLoc.enableMyLocation();
		super.onResume();
	}





	@Override
	public void onStart() {
		Log.d(getClass().getSimpleName(), "map onstart");
		
		super.onStart();
	}





	@Override
	public void onStop() {
		Log.d(getClass().getSimpleName(), "map onstop");
		
		super.onStop();
	}





	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Log.d(getClass().getSimpleName(), "map onviewcreated");
		if(!LocalService.channelsupdated&&!OsMoDroid.settings.getString("key", "").equals(""))
		{
			Log.d(getClass().getSimpleName(), "map request channels");
			Netutil.newapicommand((ResultsListener)LocalService.serContext, (Context)getSherlockActivity(), "om_device_channel_adaptive:"+OsMoDroid.settings.getString("device", ""));
		}
		
		super.onViewCreated(view, savedInstanceState);
	}





	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		Log.d(getClass().getSimpleName(), "map onviewstaterestored");
		super.onViewStateRestored(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
			//SherlockFragmentActivity context = getSherlockActivity();
			Log.d(getClass().getSimpleName(), "map oncreateview");
			mResourceProxy = new ResourceProxyImpl(inflater.getContext().getApplicationContext());
			final String name = "MapSurfer";
			final int aZoomMinLevel=0; 
			final int aZoomMaxLevel=18;
			final int aTileSizePixels=256;
			final String aImageFilenameEnding = ".png";
			final String[] aBaseUrl=new String[] {"http://openmapsurfer.uni-hd.de/tiles/roads/"};
			MAPSurferTileSource myTileSource = new MAPSurferTileSource(name, string.base, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding, aBaseUrl);
			View view = inflater.inflate(R.layout.map, container, false);
			mMapView = (MapView)view.findViewById(R.id.mapview);
			ImageButton centerImageButton = (ImageButton)view.findViewById(R.id.imageButtonCenter);
			ImageButton rotateImageButton = (ImageButton)view.findViewById(R.id.ImageButtonRotate);
			mMapView.setTileSource(myTileSource);
			if(myTracePathOverlay==null)
			{
				myTracePathOverlay=new PathOverlay(Color.RED, 10, mResourceProxy);
				myTracePathOverlay.addPoints(LocalService.traceList);
			} else
			{
				myTracePathOverlay.clearPath();
				myTracePathOverlay.addPoints(LocalService.traceList);
			}
	
			
			
			
            mMapView.getOverlays().add(myTracePathOverlay);
            
            myLoc = new MyLocationNewOverlay (getSherlockActivity(),this, mMapView);
            
            myLoc.setOptionsMenuEnabled(true);
            if(OsMoDroid.settings.getBoolean("isfollow", true))
            {
            myLoc.enableFollowLocation();
            }
            //mMapView.getOverlays().add(myLoc);
            mMapView.setBuiltInZoomControls(true);
            mMapView.setMultiTouchControls(true);
            mController = mMapView.getController();
    		
            if(OsMoDroid.settings.getInt("centerlat", -1)!=-1){
            	mController.setZoom(OsMoDroid.settings.getInt("zoom",10));
            	mController.setCenter(new GeoPoint(OsMoDroid.settings.getInt("centerlat", 0), OsMoDroid.settings.getInt("centerlon", 0)));
            } 
            else
            {
            	mController.setZoom(10);
            }
            centerImageButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Location center = MapFragment.this.getLastKnownLocation();
					if (center!=null){
					mController.setCenter(new GeoPoint(MapFragment.this.getLastKnownLocation()));
					mController.setZoom(16);
					}
					myLoc.enableFollowLocation();
					
				}
			});
            
            rotateImageButton.setOnClickListener(new View.OnClickListener() {


				@Override
				public void onClick(View v) {
					Log.d(getClass().getSimpleName(), "map click on rotate");
					if(rotate){
						rotate=false;
						mMapView.setMapOrientation(0);
					}
					else 
					{
						rotate=true;
					}
					
				}
			});
			
			CompassOverlay compas = new CompassOverlay(getSherlockActivity(), mMapView);
			ChannelsOverlay choverlay = new ChannelsOverlay(mResourceProxy, mMapView);
			mMapView.getOverlays().add(choverlay);
			mMapView.getOverlays().add(compas);
			compas.enableCompass();
			mMapView.setKeepScreenOn(true);
			return view;
	}



	

	@Override
	public void onDeviceChange(Device dev) {
		 Log.d(getClass().getSimpleName(), "ondevicechange");
		 
		 mMapView.invalidate();
			
	}


	@Override
	public void onChannelListChange() {
		
		mMapView.invalidate();
		LocalService.channelsupdated=true;
		
	}


	@Override
	public Location getLastKnownLocation() {
		Location forcelocation = LocalService.myManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location forcenetworklocation = LocalService.myManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if(forcelocation!=null){
			if (forcenetworklocation!=null){
				if(forcenetworklocation.getTime()>forcelocation.getTime()){
					return forcenetworklocation;
				}
			}
			return forcelocation;
		}
		else{
			return forcenetworklocation;	
		}
		
	}





	@Override
	public boolean startLocationProvider(IMyLocationConsumer locationConsumer) {
		 myLocationConumer=locationConsumer;
         boolean result = false;
         for (final String provider : LocalService.myManager.getProviders(true)) {
                 if (LocationManager.GPS_PROVIDER.equals(provider)
                                 || LocationManager.NETWORK_PROVIDER.equals(provider)) {
                         result = true;
                         LocalService.myManager.requestLocationUpdates(provider, 0,
                                         0, this);
                 }
         }
         return result;

		
	}





	@Override
	public void stopLocationProvider() {
		LocalService.myManager.removeUpdates(this);
		myLocationConumer=null;
		
	}





	@Override
	public void onLocationChanged(Location location) {
		if(myLoc.isFollowLocationEnabled()&&location.hasBearing()&&rotate&&location.getSpeed()>1){
		mMapView.setMapOrientation(-location.getBearing());
		}
		if(location.getProvider().equals(LocationManager.GPS_PROVIDER)){
			lastgpslocation=System.currentTimeMillis();
		} 
		//Log.d(getClass().getSimpleName(), "onlocchange mapfrag rotate="+rotate);
		//Log.d(getClass().getSimpleName(), "onlocchange mapfrag bearing="+Float.toString(location.getBearing()));
		//Log.d(getClass().getSimpleName(), "onlocchange mapfrag hasbearing="+location.hasBearing());
		if (myLocationConumer != null){
			if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER)){
				if(System.currentTimeMillis()>lastgpslocation+30000){
					myLocationConumer.onLocationChanged(location, this);
				}
			}else
			{
				myLocationConumer.onLocationChanged(location, this);
			}
		}

		
	}





	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}





	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}





	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}





	@Override
	public void onNewPoint(GeoPoint geopoint) {
		Log.d(getClass().getSimpleName(), "map on new point");
		myTracePathOverlay.addPoint(geopoint);
		
	}
	
	
	}

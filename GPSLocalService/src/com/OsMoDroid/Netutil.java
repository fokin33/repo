package com.OsMoDroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.OsMoDroid.R;
public class Netutil {
	
					dialog= ProgressDialog.show(mContext,"", mContext.getString(R.string.commandpleasewait), true);
			con.connect();
			try {
					String str=inputStreamToString(in);
			} catch (Exception e) {
	        return resAPI;
	    @Override

	    @Override
	}
	public static MyAsyncTask newapicommand(ResultsListener listener, String action) {
		MyAsyncTask t = new MyAsyncTask((ResultsListener) context, context);
	}
		MyAsyncTask t = new MyAsyncTask((ResultsListener) context, context);
	}
			MyAsyncTask t = new MyAsyncTask(listener);
	

	public static String inputStreamToString(InputStream in) throws IOException, NullPointerException {

	public static String bytesToHex(byte[] b) {

	public static String SHA1(String text) {
	}


	
	
	
}
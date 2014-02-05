package com.cms.senseapp.service;

import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cms.senseapp.R;
import com.cms.senseapp.helpers.DataManager;
import com.cms.senseapp.helpers.SenseListener;
import com.cms.senseapp.helpers.SenseObserver;
import com.cms.senseapp.listeners.DataEventListener;

public class SenseRunner extends Thread implements DataEventListener {
	
	private static final String TAG = SenseRunner.class.getName();
	
	private static final int NOTIFY_FULL_ID = 1337;
	private static final int NOTIFY_UNAVAILABLE_ID = 7331;
	
	private static final String DEFAULT_SENSOR_FILE = "sensor_data.csv";
	
	private ContentResolver mContentResolver;
	private SensorManager mSensorManager;
	private SenseService mSenseService;
	private SenseListener mSenseListener;
	private SenseObserver mSenseObserver;
	private DataManager mDataManager;
	
	public SenseRunner(SenseService service) {
		this.mSenseService = service;
		bootstrap(DEFAULT_SENSOR_FILE);
	}
	
	public SenseRunner(SenseService service, String fileName) {
		this.mSenseService = service;
		bootstrap(fileName);
	}
	
	private void bootstrap(String file) {
		Log.d(TAG, "Starting new sensor runner...");
		
		mContentResolver = mSenseService.getContentResolver();
		mSensorManager = (SensorManager) mSenseService.getSystemService(Context.SENSOR_SERVICE);
		mDataManager = new DataManager(mSenseService.getExternalFilesDir(null), file);
		mSenseListener = new SenseListener(/*view, */mDataManager);
		mSenseObserver = new SenseObserver(mSenseService.getContentResolver(), mDataManager);
		
		mDataManager.addDataEventListener(this);
		mDataManager.init();
	}
	
	public synchronized void kill() {
		Log.d(TAG, "Received kill signal...");
		cleanup();
	}

	public void run() {
		init();
	}
	
	private void init() {
		Log.d(TAG, "Init sensor runner...");
		registerSensors(Sensor.TYPE_ALL);
		registerBroadcasts();
		registerSMSObserver();
	}
	
	private void cleanup() {
		Log.d(TAG, "Cleaning up sensor runner...");
		try {
			unregisterSensors(Sensor.TYPE_ALL);
			unregisterBroadcasts();
			unregisterSMSObserver();
			mDataManager.cleanup();
		} catch (Exception e) {}
	}
	
	private void registerSensors(int type) {
		List<Sensor> deviceSensors = mSensorManager.getSensorList(type);
		for (Sensor s : deviceSensors) {
			mSensorManager.registerListener(mSenseListener, s, SensorManager.SENSOR_DELAY_GAME);
		}
	}
	
	private void unregisterSensors(int type) {
		try {
			mSensorManager.unregisterListener(mSenseListener);
		} catch (Exception e) {}
	}
	
	private void registerBroadcasts() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		mSenseService.registerReceiver(mSenseListener, filter);
	}
	
	private void unregisterBroadcasts() {
		try {
			mSenseService.unregisterReceiver(mSenseListener);
		} catch (Exception e) {}
	}
	
	private void registerSMSObserver() {
		mContentResolver.registerContentObserver(Uri.parse("content://sms"), true, mSenseObserver);
	}
	
	private void unregisterSMSObserver() {
		mContentResolver.unregisterContentObserver(mSenseObserver);
	}

	public void capacityReached() {
		Log.w(TAG, "Media full received!");
		cleanup();
		showNotification(NOTIFY_FULL_ID, "Media full!", 
				"External storage capacity has been reached.");
		mSenseService.stopSelf();
	}

	public void mediaUnavailable() {
		Log.e(TAG, "Media unavailable received!");
		cleanup();
		showNotification(NOTIFY_UNAVAILABLE_ID, "Media unavailable!", 
				"External storage is currently unavailable.");
	}

	public void newFileStarted() {
		String androidId = Secure.getString(mSenseService.getContentResolver(), Secure.ANDROID_ID);
//		Log.d(TAG, "Got UUID " + androidId);
		
		List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
		int count = deviceSensors.size();
		String[] names = new String[count];
		int[] types = new int[count];
		for (int i = 0; i < count; i++) {
			Sensor s = deviceSensors.get(i);
			names[i] = s.getName();
			types[i] = s.getType();
		}
		
		mDataManager.postHeader(androidId, count, names, types);
	}
	
	private void showNotification(int id, String title, String content) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mSenseService);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setType("file/*");
		PendingIntent resultIntent = PendingIntent.getActivity(mSenseService, 0, intent, 0);
		builder = builder.setSmallIcon(R.drawable.ic_launcher);
		builder = builder.setContentTitle(title);
		builder = builder.setContentText(content);
		builder.setContentIntent(resultIntent);
		NotificationManager notificationManager =
			    (NotificationManager) mSenseService.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(id, builder.build());
	}
}
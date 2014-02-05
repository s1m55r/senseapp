package com.cms.senseapp.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cms.senseapp.R;
import com.cms.senseapp.helpers.DataManager;
import com.cms.senseapp.listeners.DataEventListener;

public class SmsRunner extends Thread implements DataEventListener {

	private static final String TAG = SmsRunner.class.getName();
	
	private static final int NOTIFY_FULL_ID = 1331;
	private static final int NOTIFY_UNAVAILABLE_ID = 7337;
	private static final int SMS_EVENT_TYPE = 666;
	
	private static final String DEFAULT_SMS_FILE = "sms_data.csv";
	
	private SmsService mSmsService;
	private DataManager mDataManager;
	
	private String monitorObject = "MONITOR";
	private boolean running;
	private long frequency;
	
	public SmsRunner(SmsService service) {
		this.mSmsService = service;
		bootstrap(DEFAULT_SMS_FILE);
	}
	
	public SmsRunner(SmsService service, String fileName, long seconds) {
		this.mSmsService = service;
		this.frequency = seconds * 1000;
		Log.i(TAG, "Running SMS at frequency " + frequency);
		bootstrap(fileName);
	}
	
	private void bootstrap(String file) {
		Log.d(TAG, "Starting new SMS runner...");
		
		this.mDataManager = new DataManager(mSmsService.getExternalFilesDir(null), file);
		this.mDataManager.setBufferCapacity(1);
		running = true;
		
		mDataManager.addDataEventListener(this);
		mDataManager.init();
	}
	
	public void kill() {
		Log.d(TAG, "Received kill signal...");
		cleanup();
	}
	
	public void run() {
		init();
	}
	
	private void init() {
		Log.d(TAG, "Init SMS runner...");
		while (running) {
			mDataManager.postSMSEvent(System.nanoTime(), SMS_EVENT_TYPE, 0, 
					System.currentTimeMillis()*1000000);
			doWait(frequency);
		}
	}
	
	private void cleanup() {
		Log.d(TAG, "Cleaning up SMS runner...");
		try {
			running = false;
			doNotify();
			mDataManager.cleanup();
		} catch (Exception e) {}
	}
	
	private void doWait(long millis) {
		synchronized(monitorObject) {
			try {
				monitorObject.wait(millis);
			} catch (InterruptedException ie) {}
		}
	}
	
	private void doNotify() {
		synchronized(monitorObject) {
			monitorObject.notifyAll();
		}
	}

	public void capacityReached() {
		Log.w(TAG, "Media full received!");
		cleanup();
		showNotification(NOTIFY_FULL_ID, "Media full!", 
				"External storage capacity has been reached.");
	}

	public void mediaUnavailable() {
		Log.e(TAG, "Media unavailable received!");
		cleanup();
		showNotification(NOTIFY_UNAVAILABLE_ID, "Media unavailable!", 
				"External storage is currently unavailable.");
	}

	public void newFileStarted() {
		Log.e(TAG, "New SMS file started!");
		
		String androidId = Secure.getString(mSmsService.getContentResolver(), Secure.ANDROID_ID);
		mDataManager.postHeader(androidId, 0, new String[0], new int[0]);
		
		showNotification(NOTIFY_FULL_ID, "New SMS file", "New SMS file started!");
	}
	
	private void showNotification(int id, String title, String content) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mSmsService);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setType("file/*");
		PendingIntent resultIntent = PendingIntent.getActivity(mSmsService, 0, intent, 0);
		builder = builder.setSmallIcon(R.drawable.ic_smserr);
		builder = builder.setContentTitle(title);
		builder = builder.setContentText(content);
		builder.setContentIntent(resultIntent);
		NotificationManager notificationManager =
			    (NotificationManager) mSmsService.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(id, builder.build());
	}
}
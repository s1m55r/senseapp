package com.cms.senseapp.service;

import java.util.ArrayList;
import java.util.List;

import com.cms.senseapp.MainActivity;
import com.cms.senseapp.R;
import com.cms.senseapp.listeners.ServiceStateListener;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class SmsService extends Service {
	
	private static final String TAG = SmsService.class.getName();
	
	private static final int NOTIFY_ONGOING = 1339;
	private static final long DEFAULT_FREQUENCY = 300;
	
	private static final String SMS_FILE_PARAM = "sms_file";
	private static final String FREQ_PARAM = "sms_freq";
	
	private static Boolean isRunning = false;
	
	private List<ServiceStateListener> listeners;
	
	private SmsRunner runner;
	
	public void onCreate() {
		Log.d(TAG, "Starting service...");
		this.listeners = new ArrayList<ServiceStateListener>();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Served start command...");
		
		String file = null;
		long frequency = DEFAULT_FREQUENCY;
		if (intent != null) {
			file = intent.getStringExtra(SMS_FILE_PARAM);
			String f = intent.getStringExtra(FREQ_PARAM).replaceAll("[^\\d]", "");
			try {
				frequency = Long.parseLong(f) % 3601;
				if (frequency == 0) {
					frequency = 300;
				}
			} catch (Exception e) {
				frequency = 300;
			}
		}
		
		synchronized(isRunning) {
			Log.i(TAG, "Checking runner...");
			if (!((runner != null) && runner.isAlive())) {
				isRunning = true;
				putInForeground();
				if ((file != null) && (!file.isEmpty())) {
					runner = new SmsRunner(this, file, frequency);
				}
				else {
					runner = new SmsRunner(this);
				}
				runner.start();
			}
		}

		notifyServiceStateListeners();
		return START_REDELIVER_INTENT;
	}

	public void onDestroy() {
		Log.i(TAG, "Served destroy command...");
		synchronized (isRunning) {
			stopForeground(true);
			if (runner != null) {
				runner.kill();
			}
			isRunning = false;
		}
		notifyServiceStateListeners();
	}
	
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "Low memory conditions detected!");
	}
	
	private void putInForeground() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder = builder.setSmallIcon(R.drawable.ic_smsicon);
		builder = builder.setContentTitle("SenseApp");
		builder = builder.setContentText("Gathering sensor data");
		
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent resultIntent = PendingIntent.getActivity(this, 0, intent, 0);
		builder.setContentIntent(resultIntent);
		
		startForeground(NOTIFY_ONGOING, builder.build());
	}

	public IBinder onBind(Intent intent) {
		return null;
	}

	public static boolean isRunning() {
		return isRunning;
	}
	
	public void addServiceStateListener(ServiceStateListener listener) {
		listeners.add(listener);
	}
	
	private void notifyServiceStateListeners() {
		for (ServiceStateListener ssl : listeners) {
			ssl.onServiceStatusChanged(isRunning, this);
		}
	}
}
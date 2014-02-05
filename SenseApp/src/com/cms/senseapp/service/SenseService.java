package com.cms.senseapp.service;

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cms.senseapp.MainActivity;
import com.cms.senseapp.R;
import com.cms.senseapp.listeners.ServiceStateListener;

public class SenseService extends Service {

	private static final String TAG = SenseService.class.getName();
	
	private static final int NOTIFY_ONGOING = 1338;
	
	private static final String SENSOR_FILE_PARAM = "sense_file";

	private static Boolean isRunning = false;
	
	private List<ServiceStateListener> listeners;
	
	private SenseRunner runner;
	
	public void onCreate() {
		Log.d(TAG, "Starting service...");
		this.listeners = new ArrayList<ServiceStateListener>();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Served start command...");
		
		String file = null;
		if (intent != null) {
			file = intent.getStringExtra(SENSOR_FILE_PARAM);
		}
		
		synchronized(isRunning) {
			Log.i(TAG, "Checking runner...");
			if (!((runner != null) && runner.isAlive())) {
				isRunning = true;
				putInForeground();
				if ((file != null) && (!file.isEmpty())) {
					runner = new SenseRunner(this, file);
				}
				else {
					runner = new SenseRunner(this);
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
		builder = builder.setSmallIcon(R.drawable.ic_another);
		builder = builder.setContentTitle("SenseApp");
		builder = builder.setContentText("Gathering sensor data");
		
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent resultIntent = PendingIntent.getActivity(this, 0, intent, 0);
		builder.setContentIntent(resultIntent);
		
		startForeground(NOTIFY_ONGOING, builder.build());
	}

	public IBinder onBind(Intent arg0) {
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
package com.cms.senseapp;

import java.util.List;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.cms.senseapp.listeners.ServiceStateListener;
import com.cms.senseapp.service.SenseService;
import com.cms.senseapp.service.SmsService;

public class MainActivity extends Activity implements ServiceStateListener {

	private static final String TAG = MainActivity.class.getName();
	
	private static final String SENSOR_FILE_PARAM = "sense_file";
	private static final String SMS_FILE_PARAM = "sms_file";
	private static final String FREQ_PARAM = "sms_freq";
	private static final String PREFS_NAME = "sense_prefs";
	
	private SensorManager mSensorManager;
	private SharedPreferences preference;
	
	private ToggleButton serviceButton;
	private ToggleButton smsButton;
	private EditText serviceText;
	private EditText smsText;
	private EditText smsFreq;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		preference = getSharedPreferences(PREFS_NAME, 0);
		
		serviceButton = (ToggleButton) findViewById(R.id.toggle_service);
		serviceButton.setChecked(SenseService.isRunning());
		
		serviceButton = (ToggleButton) findViewById(R.id.toggle_sms);
		serviceButton.setChecked(SmsService.isRunning());
		
		serviceText = (EditText) findViewById(R.id.service_file);
		smsText = (EditText) findViewById(R.id.sms_file);
		smsFreq = (EditText) findViewById(R.id.sms_frequency);
		
		String defaultServiceFile = getString(R.string.default_service_file);
		String defaultSmsFile = getString(R.string.default_sms_file);
		String defaultSmsFrequency = getString(R.string.default_sms_frequency);
		
		serviceText.setText(preference.getString(SENSOR_FILE_PARAM, defaultServiceFile));
		smsText.setText(preference.getString(SMS_FILE_PARAM, defaultSmsFile));
		smsFreq.setText(preference.getString(FREQ_PARAM, defaultSmsFrequency));
	}

	protected void onResume() {
		super.onResume();
//		Log.d(TAG, "Resumed main...");
		init();
	}
	
	private void init() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	private void cleanup() {
		try {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} catch (Exception e) {}
	}

	protected void onPause() {
		super.onPause();
//		Log.d(TAG, "Pausing main...");
		cleanup();
	}
	
	protected void onDestroy() {
		super.onDestroy();
		cleanup();
	}
	
	public void onServiceStateToggled(View v) {
		boolean started = ((ToggleButton) v).isChecked();
		Intent service = new Intent(this, SenseService.class);
		if (started) {
			Log.i(TAG, "Starting sensor service...");
			service.putExtra(SENSOR_FILE_PARAM, serviceText.getText().toString());
			startService(service);
			putPreferences(SENSOR_FILE_PARAM, serviceText.getText().toString());
		}
		else {
			Log.i(TAG, "Stopping sensor service...");
			stopService(service);
		}
	}
	
	public void onSMSStateToggled(View v) {
		boolean started = ((ToggleButton) v).isChecked();
		Intent service = new Intent(this, SmsService.class);
		if (started) {
			Log.i(TAG, "Starting SMS service...");
			service.putExtra(SMS_FILE_PARAM, smsText.getText().toString());
			service.putExtra(FREQ_PARAM, smsFreq.getText().toString());
			startService(service);
			putPreferences(SMS_FILE_PARAM, smsText.getText().toString());
			putPreferences(FREQ_PARAM, smsFreq.getText().toString());
		}
		else {
			Log.i(TAG, "Stopping SMS service...");
			stopService(service);
		}
	}

	public void onServiceStatusChanged(boolean isRunning, Service service) {
		if (service instanceof SenseService) {
			Log.d(TAG, "Sensor service " + (isRunning ? "started!" : "stopped!"));
			serviceButton.setChecked(isRunning);
		}
		else if (service instanceof SmsService) {
			Log.d(TAG, "SMS service " + (isRunning ? "started!" : "stopped!"));
			smsButton.setChecked(isRunning);
		}
	}
	
	private void putPreferences(String key, String value) {
		Editor editor = preference.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public String listAllSensors(int type) {
		List<Sensor> deviceSensors = mSensorManager.getSensorList(type);
		String str = "";
		for (Sensor s : deviceSensors) {
			str += s.getName() + "\n";
			str += "Vendor: " + s.getVendor() + "\n";
			str += "Version: " + s.getVersion() + "\n";
			str += "Res: " + s.getResolution() + "\n";
			str += "MaxRange: " + s.getMaximumRange() + "\n";
			str += "MinDelay:" + s.getMinDelay() + "\n";
			str += "Power: " + s.getPower() + "\n";
			str += "Type: " + s.getType() + "\n";
			str += "\n";
		}
		return str;
	}
}
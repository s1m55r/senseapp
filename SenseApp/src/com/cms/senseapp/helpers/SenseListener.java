package com.cms.senseapp.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SenseListener extends BroadcastReceiver implements SensorEventListener {

	private static final String TAG = SenseListener.class.getName();
	// private static final int SAMPLE_DENSITY = 10;
	
	private static final int SCREEN_STATE_CODE = -123;
	private static final int SCREEN_STATE_OFF = 0;
	private static final int SCREEN_STATE_ON = 1;
	private static final int SCREEN_STATE_UNLOCKED = 2;
	
//	private static int accCounter = 0;
//	private static int magCounter = 0;
//	private static int ortCounter = 0;
//	private static int gyrCounter = 0;
//	private static int rotCounter = 0;
	
	private float[] gravity;
	private float[] geomag;
	private float[] realGravity;
	private float[] R;
    private float[] I;
    private float[] orientation;
	
//	private TextView view;
	private DataManager mDataManager;
	
	public SenseListener(/*TextView view, */DataManager manager) {
//		this.view = view;
		this.mDataManager = manager;
		
		gravity = new float[3];
		geomag = new float[3];
		realGravity = new float[3];
		R = new float[16];
		I = new float[16];
		orientation = new float[3];
	}

	public void onReceive(Context context, Intent intent) {
		long systime = System.currentTimeMillis() * 1000000;
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			mDataManager.postData(System.nanoTime(), SCREEN_STATE_CODE, 
					SensorManager.SENSOR_STATUS_ACCURACY_HIGH, SCREEN_STATE_OFF, systime);
		}
		else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			mDataManager.postData(System.nanoTime(), SCREEN_STATE_CODE, 
					SensorManager.SENSOR_STATUS_ACCURACY_HIGH, SCREEN_STATE_ON, systime);
		}
		else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
			mDataManager.postData(System.nanoTime(), SCREEN_STATE_CODE, 
					SensorManager.SENSOR_STATUS_ACCURACY_HIGH, SCREEN_STATE_UNLOCKED, systime);
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TO DO nothing
	}

	@SuppressWarnings("deprecation")
	public void onSensorChanged(SensorEvent event) {
		if (true/*event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE*/) {
			switch(event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				getAccelerometerData(event);
				getOrientationData(event.timestamp);
				getFixedAccData(event.timestamp);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				getMagneticData(event);
				getOrientationData(event.timestamp);
				break;
			case Sensor.TYPE_GRAVITY:
				getGravityData(event);
				getFixedAccData(event.timestamp);
				break;
			case Sensor.TYPE_GYROSCOPE:
				getGyroscopeData(event);
				break;
			case Sensor.TYPE_ROTATION_VECTOR:
				getRotationData(event);
				break;
			case Sensor.TYPE_LIGHT:
				getLightData(event);
				break;
			case Sensor.TYPE_PROXIMITY:
				getProximityData(event);
				break;
			case Sensor.TYPE_AMBIENT_TEMPERATURE:
				getTemperatureData(event);
				break;
			case Sensor.TYPE_PRESSURE:
				getPressureData(event);
				break;
			case Sensor.TYPE_RELATIVE_HUMIDITY:
				getHumidityData(event);
				break;
			case Sensor.TYPE_LINEAR_ACCELERATION:
				getLinearAcceleration(event);
				break;
			case Sensor.TYPE_GAME_ROTATION_VECTOR:
				getGameRotation(event);
				break;
			case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
				getGyroUncalibrated(event);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
				getMagUncalibrated(event);
				break;
			case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
				getGeomagRotation(event);
				break;
			case Sensor.TYPE_ORIENTATION:
				break;
			default:
				Log.w(TAG, "Unknown sensor: " + event.sensor.getName() + " | " + event.sensor.getType());
			}
		}
	}

	private void getAccelerometerData(SensorEvent event) {
		// if (accCounter == 0) {
			mDataManager.postData(event.timestamp, Sensor.TYPE_ACCELEROMETER, event.accuracy, 
					event.values[0], event.values[1], event.values[2]);
		// }
		// accCounter = ++accCounter % SAMPLE_DENSITY;
		gravity = event.values.clone();
	}

	private void getGravityData(SensorEvent event) {
		// if (accCounter == 0) {
			mDataManager.postData(event.timestamp, Sensor.TYPE_GRAVITY, event.accuracy, 
					event.values[0], event.values[1], event.values[2]);
		// }
		// accCounter = ++accCounter % SAMPLE_DENSITY;
		realGravity = event.values.clone();
	}
	
	private void getMagneticData(SensorEvent event) {
		// if (magCounter == 0) {
			mDataManager.postData(event.timestamp, Sensor.TYPE_MAGNETIC_FIELD, event.accuracy, 
					event.values[0], event.values[1], event.values[2]);
		// }
		// magCounter = ++magCounter % SAMPLE_DENSITY;
		geomag = event.values.clone();
	}
	
	@SuppressWarnings("deprecation")
	private void getOrientationData(long timestamp) {
		// if (ortCounter == 0) {
			SensorManager.getRotationMatrix(R, I, gravity, geomag);
			SensorManager.getOrientation(R, orientation);
			for (int i = 0; i < 3; i++) {
				orientation[i] = (float) Math.toDegrees(orientation[i]);
			}
			mDataManager.postData(timestamp, Sensor.TYPE_ORIENTATION, 
					SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 
					orientation[0], orientation[1], orientation[2]);
		// }
		// ortCounter = ++ortCounter % SAMPLE_DENSITY;
	}
	
	private void getFixedAccData(long timestamp) {
		float[] facc = new float[3];
		for (int i = 0; i < 3; i++) {
			facc[i] = gravity[i] - realGravity[i];
		}
		mDataManager.postData(timestamp, Sensor.TYPE_LINEAR_ACCELERATION, 
				SensorManager.SENSOR_STATUS_UNRELIABLE, 
				facc[0], facc[1], facc[2]);
	}

	private void getGyroscopeData(SensorEvent event) {
		// if (gyrCounter == 0) {
			mDataManager.postData(event.timestamp, Sensor.TYPE_GYROSCOPE, event.accuracy, 
					event.values[0], event.values[1], event.values[2]);
		// }
		// gyrCounter = ++gyrCounter % SAMPLE_DENSITY;
	}

	private void getRotationData(SensorEvent event) {
		// if (rotCounter == 0) {
			mDataManager.postData(event.timestamp, Sensor.TYPE_ROTATION_VECTOR, event.accuracy, 
					event.values[0], event.values[1], event.values[2]);
		// }
		// rotCounter = ++rotCounter % SAMPLE_DENSITY;
	}
	
	private void getProximityData(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_PROXIMITY, event.accuracy, event.values[0]);
	}
	
	private void getLightData(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_LIGHT, event.accuracy, event.values[0]);
	}
	
	private void getTemperatureData(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_AMBIENT_TEMPERATURE, event.accuracy, event.values[0]);
	}
	
	private void getPressureData(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_PRESSURE, event.accuracy, event.values[0]);
	}
	
	private void getHumidityData(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_RELATIVE_HUMIDITY, event.accuracy, event.values[0]);
	}

	private void getLinearAcceleration(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_LINEAR_ACCELERATION, event.accuracy, event.values);
	}

	private void getGameRotation(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_GAME_ROTATION_VECTOR, event.accuracy, event.values);
	}

	private void getGyroUncalibrated(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_GYROSCOPE_UNCALIBRATED, event.accuracy, event.values);
	}

	private void getMagUncalibrated(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, event.accuracy, event.values);
	}

	private void getGeomagRotation(SensorEvent event) {
		mDataManager.postData(event.timestamp, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, event.accuracy, event.values);
	}
	
	public void cleanup() {
		
	}
	
//	private void showData(String str) {
//		view.append(str);
//	}
}
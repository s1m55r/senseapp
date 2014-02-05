package com.cms.senseapp.helpers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.util.Log;

import com.cms.senseapp.listeners.DataEventListener;
import com.cms.senseapp.listeners.StorageEventListener;

public class DataManager implements StorageEventListener {

	private static final String TAG = DataManager.class.getName();
	
	private static final char COMMA = ',';
	private static final char NEWLINE = '\n';
	private static final String COUNT = "COUNT";
	private int mBufferCapacity = 1024;
	
	private StringBuilder buffer;
	private int entries;
	private boolean emptied;
	private StorageManager mStorageManager;
	
	private List<DataEventListener> listeners;
	
	public DataManager(File fileDir) {
		listeners = new ArrayList<DataEventListener>();
		buffer = new StringBuilder();
		mStorageManager = new StorageManager(fileDir);
		entries = 0;
	}
	
	public DataManager(File fileDir, String fileName) {
		listeners = new ArrayList<DataEventListener>();
		buffer = new StringBuilder();
		mStorageManager = new StorageManager(fileDir, fileName);
	}
	
	public void init() {
//		Log.d(TAG, "Init data manager");
		mStorageManager.addStorageEventListener(this);
		mStorageManager.init();
	}
	
	public void cleanup() {
//		Log.i(TAG, "Cleaning up data manager...");
		if (!emptied) {
			emptied = true;
			mStorageManager.flushData(buffer.toString(), false, true);
		}
		mStorageManager.cleanup();
	}
	
	public void postData(long timestamp, int type, int accuracy, float... values) {
		buffer.append(timestamp);
		buffer.append(COMMA);
		buffer.append(type);
		buffer.append(COMMA);
		buffer.append(values.length);
		buffer.append(COMMA);
		for (float f : values) {
			buffer.append(f);
			buffer.append(COMMA);
		}
		buffer.append(accuracy);
		buffer.append(NEWLINE);
		
		raiseAndFlush();
	}
	
	public void postSMSEvent(long timestamp, int smsCode, int direction, long alttime) {
		buffer.append(timestamp);
		buffer.append(COMMA);
		buffer.append(smsCode);
		buffer.append(COMMA);
		buffer.append(direction);
		buffer.append(COMMA);
		buffer.append(alttime);
		buffer.append(NEWLINE);
		
		raiseAndFlush();
	}
	
	public void postHeader(String uuid, int count, String[] names, int[] types) {
		StringBuilder header = new StringBuilder();
		header.append(uuid);
		header.append(NEWLINE);
		header.append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
		header.append(NEWLINE);
		header.append(COUNT);
		header.append(COMMA);
		header.append(count);
		header.append(NEWLINE);
		for (int i = 0; i < count; i++) {
			header.append(names[i]);
			header.append(COMMA);
			header.append(types[i]);
			header.append(NEWLINE);
		}
		
		mStorageManager.flushData(header.toString());
	}
	
	private void raiseAndFlush() {
		if ((++entries) >= mBufferCapacity) {
			Log.d(TAG, "Flushing buffer...");
			mStorageManager.flushData(buffer.toString());
			buffer = new StringBuilder();
			entries = 0;
		}
	}
	
	public void addDataEventListener(DataEventListener listener) {
		listeners.add(listener);
	}

	public void storageUnavailable() {
		for (DataEventListener del : listeners) {
			del.mediaUnavailable();
		}
	}

	public void storageFull() {
		for (DataEventListener del : listeners) {
			del.capacityReached();
		}
	}

	public void fileCreated() {
		for (DataEventListener del : listeners) {
			del.newFileStarted();
		}
	}
	
	public void setBufferCapacity(int capacity) {
		this.mBufferCapacity = capacity;
	}
}
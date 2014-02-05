package com.cms.senseapp.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.cms.senseapp.listeners.StorageEventListener;

public class StorageManager {

	private static final String TAG = StorageManager.class.getName();
	
	private static final String SUCCESS_STRING = "0";
	private static final String FULL_STRING = "1";
	
	private static final byte STORAGE_STATUS_UNAVAILABLE = 1;
	private static final byte STORAGE_STATUS_FULL = 2;
	private static final byte STORAGE_STATUS_STARTED = 3;
	
	private static final String NO_RETRY_STRING = "nr";
	private static final String DEFAULT_DATA_FILE = "sense_data.csv";
	private static final long MAX_STORAGE = 536870912;
	private static final int MAX_RETRY_ATTEMPTS = 6;
	private static final int RETRY_TIMEOUT = 5000;
	
	private String mFileName;
	private File fileDir;
	private File dataFile;
	private FileOutputStream out;
	
	private List<StorageEventListener> listeners;
	
	public StorageManager(File fileDir) {
		listeners = new ArrayList<StorageEventListener>();
		this.fileDir = fileDir;
		this.mFileName = DEFAULT_DATA_FILE;
	}
	
	public StorageManager(File fileDir, String fileName) {
		listeners = new ArrayList<StorageEventListener>();
		this.fileDir = fileDir;
		this.mFileName = fileName;
	}
	
	public void init() {
//		Log.d(TAG, "Init storage manager");
		
		Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread thread, Throwable ex) {
				Log.e(TAG, "***Exception in thread " + thread.getName() + "***");
				Log.e(TAG, Log.getStackTraceString(ex));
			}
		});
		
		setUpFile(mFileName);
	}
	
	public void cleanup() {
//		Log.d(TAG, "Cleaning up storage manager...");
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {}
		}
	}
	
	public void flushData(String data) {
		try {
			new StorageTask().execute(data);
		}
		catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}
	
	public void flushData(String data, boolean async, boolean noretry) {
		try {
			if (async) {
				if (noretry) {
					new StorageTask().execute(data, NO_RETRY_STRING);
				}
				else {
					new StorageTask().execute(data);
				}
			}
			else {
				performWrite(data.getBytes(), noretry);
			}
		}
		catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}
	
	private void setUpFile(String fileName) {
		String mediaState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(mediaState)) {
			dataFile = new File(fileDir, fileName);
			if (dataFile != null) {
				try {
					out = new FileOutputStream(dataFile, true);
					if (dataFile.length() == 0) {
						writeFileHeader();
					}
					Log.i(TAG, "Opened stream to " + dataFile.getAbsolutePath());
				} catch (FileNotFoundException e) {
					Log.e(TAG, "Error opening file stream: " + e.getMessage());
					notifyStorageEventListeners(STORAGE_STATUS_UNAVAILABLE);
				}
			}
			else {
				Log.w(TAG, "Can't write to file");
				notifyStorageEventListeners(STORAGE_STATUS_UNAVAILABLE);
			}
		}
		else {
			Log.w(TAG, "Media unavailable!");
			notifyStorageEventListeners(STORAGE_STATUS_UNAVAILABLE);
		}
	}
	
	private void writeFileHeader() {
		notifyStorageEventListeners(STORAGE_STATUS_STARTED);
	}
	
	public void addStorageEventListener(StorageEventListener listener) {
		listeners.add(listener);
	}
	
	private void notifyStorageEventListeners(byte code) {
//		Log.d(TAG, "Notifying storage event listeners code " + code);
		switch(code) {
		case STORAGE_STATUS_UNAVAILABLE:
			for (StorageEventListener sel : listeners) {
				sel.storageUnavailable();
			}
			break;
		case STORAGE_STATUS_FULL:
			for (StorageEventListener sel : listeners) {
				sel.storageFull();
			}
			break;
		case STORAGE_STATUS_STARTED:
			for (StorageEventListener sel : listeners) {
				sel.fileCreated();
			}
			break;
		}
	}
	
	private String performWrite(byte[] data, boolean noretry) {
		try {
			synchronized(out) {
//				Log.d(TAG, "Write started at " + System.currentTimeMillis());
				String mediaState = Environment.getExternalStorageState();
				if (Environment.MEDIA_MOUNTED.equals(mediaState)) {
					long stored = dataFile.length();
					if (stored < MAX_STORAGE) {
						try {
							out.write(data);
							Log.i(TAG, "Wrote " + data.length + " bytes to " + mFileName);
							return SUCCESS_STRING;
						} catch (IOException e) {
							Log.w(TAG, "Failure writing to file: " + e + "!");
							Log.e(TAG, Log.getStackTraceString(e));
							if (!noretry) {
								for (int i = 0; i < MAX_RETRY_ATTEMPTS; i++) {
									try {
										try {
											if (out != null) {
												out.close();
											}
										} catch (Exception ine) {}
										Thread.sleep(RETRY_TIMEOUT);
										Log.i(TAG, "Retrying...");
										out = new FileOutputStream(dataFile, true);
										if (dataFile.length() == 0) {
											writeFileHeader();
										}
										out.write(data);
										return SUCCESS_STRING;
									} catch (IOException e1) {
										Log.w(TAG, "Retry failed!");
									} catch (InterruptedException e1) {}
								}
							}
						}
					}
					else {
						return FULL_STRING;
					}
				}
//				Log.d(TAG, "Write finished at " + System.currentTimeMillis());
			}
		} catch (Exception e) {}
		return null;
	}
	
	private class StorageTask extends AsyncTask<String, Void, String> {

		protected String doInBackground(String... params) {
//			Log.d(TAG, "Started background task");
			try {
				byte[] data = params[0].getBytes();
				boolean noretry = (params.length > 1 ? true : false);
				return performWrite(data, noretry);
			}
			catch (Exception e) {
				Log.w(TAG, Log.getStackTraceString(e));
				return null;
			}
		}

		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result == null) {
				notifyStorageEventListeners(STORAGE_STATUS_UNAVAILABLE);
			}
			else {
//				Log.i(TAG, "Got result " + result);
				if (FULL_STRING.equals(result)) {
					notifyStorageEventListeners(STORAGE_STATUS_FULL);
				}
			}
		}
	}
}
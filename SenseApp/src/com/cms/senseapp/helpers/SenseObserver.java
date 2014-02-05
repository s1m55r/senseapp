package com.cms.senseapp.helpers;

import java.util.HashMap;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class SenseObserver extends ContentObserver {
	
	private static final String TAG = SenseObserver.class.getName();

	private static final int SMS_EVENT_TYPE = 545;
	private static final long SMS_WINDOW_GAP = 30000;	// gap in milliseconds
	
	private static final String[] COMMOM_PROJECTION = {"protocol"};
	private static final String[] OUTGOING_PROJECTION = {"protocol", "date"};
	private static final String SORT_ORDER = "date DESC";
	
	private ContentResolver mContentResolver;
	private DataManager mDataManager;
	private HashMap<Long, Boolean> dates;
	
	public SenseObserver(ContentResolver resolver, DataManager manager) {
		super(new Handler());
		
		this.mContentResolver = resolver;
		this.mDataManager = manager;
		
		dates = new HashMap<Long, Boolean>();
	}

	public boolean deliverSelfNotifications() {
		return false;
	}

	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		try {
			Cursor cur = mContentResolver.query(Uri.parse("content://sms"), 
											COMMOM_PROJECTION, null, null, SORT_ORDER);
			cur.moveToFirst();
			String protocol = cur.getString(0);
			
			if (protocol == null) {
				Cursor ocur = mContentResolver.query(Uri.parse("content://sms/sent"), 
						OUTGOING_PROJECTION, null, null, SORT_ORDER);
				ocur.moveToFirst();
				long date = ocur.getLong(1);
				long time = System.currentTimeMillis();
				if ((!dates.containsKey(date)) && (Math.abs(date - time) < SMS_WINDOW_GAP)) {
					dates.put(date, null);
					Log.i(TAG, "Outgoing SMS!");
					mDataManager.postSMSEvent(System.nanoTime(), SMS_EVENT_TYPE, 0, 
							System.currentTimeMillis()*1000000);
				}
				ocur.close();
			}
			else {
				Log.i(TAG, "Incoming SMS!");
				mDataManager.postSMSEvent(System.nanoTime(), SMS_EVENT_TYPE, 1, 
						System.currentTimeMillis()*1000000);
			}
			cur.close();
		} catch (Exception ex) {
			Log.e(TAG, ex.toString());
		}
	}
}
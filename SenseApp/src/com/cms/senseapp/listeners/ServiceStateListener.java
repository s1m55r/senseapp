package com.cms.senseapp.listeners;

import android.app.Service;


public interface ServiceStateListener {
	public void onServiceStatusChanged(boolean isRunning, Service service);
}
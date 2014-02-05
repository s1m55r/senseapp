package com.cms.senseapp.listeners;

public interface DataEventListener {
	public void capacityReached();
	public void mediaUnavailable();
	public void newFileStarted();
}
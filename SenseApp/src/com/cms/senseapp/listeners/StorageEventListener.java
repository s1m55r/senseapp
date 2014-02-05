package com.cms.senseapp.listeners;

public interface StorageEventListener {
	public void storageUnavailable();
	public void storageFull();
	public void fileCreated();
}
package com.genware.service;

public interface IConnectionStatusCallback {
	public void connectionStatusChanged(int connectedState, String reason);
}

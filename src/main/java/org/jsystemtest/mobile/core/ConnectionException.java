package org.jsystemtest.mobile.core;

public class ConnectionException extends Exception {
	private static final long serialVersionUID = 1L;
	private final String deviceSerial;
	private Throwable t;

	public ConnectionException(String deviceSerial) {
		super("Failed to connect to device with serial " + deviceSerial);
		this.deviceSerial = deviceSerial;

	}

	public ConnectionException(String deviceSerial, Throwable t) {
		super("Failed to connect to device with serial " + deviceSerial);
		this.deviceSerial = deviceSerial;
		this.t = t;

	}

	public Throwable getT() {
		return t;
	}

	public String getDeviceSerial() {
		return deviceSerial;
	}

}

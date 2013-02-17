package org.jsystemtest.mobile.core.device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.jsystemtest.mobile.core.ConnectionException;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;

public class WiFiDevice extends AbstractAndroidDevice {

	private final static Logger logger = Logger.getLogger(WiFiDevice.class);

	private final String host;

	private final int tcpPort;

	public WiFiDevice(AndroidDebugBridge adb, IDevice device, String host, int tcpPort) throws Exception {
		super(adb, device);
		this.tcpPort = tcpPort;
		this.host = host;

	}

	public void connect() throws ConnectionException {
		String cmd = "adb connect " + device.getSerialNumber() + ":" + tcpPort;
		Runtime run = Runtime.getRuntime();
		Process pr;
		try {
			pr = run.exec(cmd);
			pr.waitFor();
			BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			long startTime = System.currentTimeMillis();
			while (!buf.ready() && System.currentTimeMillis() - startTime < 30000) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// Don't care
				}
			}
			if (buf.ready()) {
				char[] cbuf = new char[256];
				buf.read(cbuf);
				logger.debug(String.valueOf(cbuf));
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			logger.error("Failed to connect to device " + getSerialNumber());
			throw new ConnectionException(getSerialNumber(), e);
		}

	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	@Override
	public void runTestOnDevice(String pakageName, String testClassName, String testName) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void installPackage(String apkLocation, boolean reinstall) throws InstallException {
		// TODO Auto-generated method stub

	}

}

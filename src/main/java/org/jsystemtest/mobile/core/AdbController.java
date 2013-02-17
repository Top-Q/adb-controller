package org.jsystemtest.mobile.core;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsystemtest.mobile.core.device.AbstractAndroidDevice;
import org.jsystemtest.mobile.core.device.USBDevice;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;

/**
 * <b>ADB Controller</b><br>
 * Uses the AndroidDebugBridge object to support ADB operations.<br>
 * Holds the ADB TCP Clients.<br>
 * 
 * @see <a
 *      href="http://developer.android.com/guide/developing/tools/adb.html">ADB
 *      documentaion</a>
 * @author topq
 * 
 */
public class AdbController implements IDeviceChangeListener {

	public enum CommunicationBus {
		USB, WIFI
	}

	private final static Logger logger = Logger.getLogger(AdbController.class);
	private static AdbController instance;

	private File adbLocation;
	private long timeoutForDeviceConnection = 120000;
	private Map<String, AbstractAndroidDevice> devices = new HashMap<String, AbstractAndroidDevice>();
	private CommunicationBus communicationBus = CommunicationBus.USB;
	private AndroidDebugBridge adb;

	/**
	 * Initialize the system object. <br>
	 * Get all the connected devices (if exist), set port forwarding &
	 * initialize the TCP connection<br>
	 */
	private AdbController() throws Exception {
		if (null == adbLocation) {
			adbLocation = findAdbFile();
		}

		// Init the AndroidDebugBridge object
		AndroidDebugBridge.init(false);
		adb = AndroidDebugBridge.createBridge(adbLocation.getAbsolutePath() + File.separator + "adb", true);
		if (adb == null) {
			throw new IllegalStateException("Failed to create ADB bridge");
		}
		AndroidDebugBridge.addDeviceChangeListener(this);
	}

	public static AdbController getInstance() throws Exception {
		if (instance == null) {
			synchronized (AdbController.class) {
				if (instance == null) {
					instance = new AdbController();
				}
			}
		}
		return instance;
	}

	/**
	 * Close system object, close the TCP connections & remove port forwarding
	 */
	public void close() {
		for (AbstractAndroidDevice device : devices.values()) {
			device.disconnect();
		}
		terminate();
	}

	/**
	 * Close the AndroidDebugBridge
	 */
	public void terminate() {
		AndroidDebugBridge.terminate();
	}

	/**
	 * Get IDevice by serial number
	 * 
	 * @param deviceSerial
	 * @return the IDevice with the requested serial number if exists
	 * @throws Exception
	 */
	public AbstractAndroidDevice getDevice(String deviceSerial) {
		return devices.get(deviceSerial);
	}

	/**
	 * Print the devices
	 * 
	 * @throws Exception
	 */
	public void printDevices() throws Exception {
		for (AbstractAndroidDevice device : devices.values()) {
			System.out.println(device.toString());
		}
	}

	/**
	 * Search for the adb.exe in the folder that is specified in the
	 * ANDROID_HOME environment variable
	 * 
	 * @return
	 */
	private static File findAdbFile() throws IOException {
		// Check if the adb file is in the current folder
		File[] adbFile = new File(".").listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().equals("adb") || pathname.getName().equals("adb.exe");
			}
		});
		if (adbFile != null && adbFile.length > 0) {
			return adbFile[0].getParentFile();
		}

		final String androidHome = System.getenv("ANDROID_HOME");
		if (androidHome == null || androidHome.isEmpty()) {
			throw new IOException("ANDROID_HOME environment variable is not set");
		}

		final File root = new File(androidHome);
		if (!root.exists()) {
			throw new IOException("Android home: " + root.getAbsolutePath() + " does not exist");
		}

		try {
			// String[] extensions = { "exe" };
			Collection<File> files = FileUtils.listFiles(root, null, true);
			for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
				File file = (File) iterator.next();
				// TODO: Eran - I think should be using equals as compareTo is
				// more sortedDataStructure oriented.
				if (file.getName().equals("adb.exe") || file.getName().equals("adb")) {
					return file.getParentFile();
				}
			}
		} catch (Exception e) {
			throw new IOException("Failed to find adb in " + root.getAbsolutePath());
		}
		throw new IOException("Failed to find adb in " + root.getAbsolutePath());
	}

	public File getAdbLocation() {
		return adbLocation;
	}

	public CommunicationBus getCommunicationBus() {
		return communicationBus;
	}

	/**
	 * Device connection mode
	 * 
	 * @param communicationBus
	 */
	public void setCommunicationBus(CommunicationBus communicationBus) {
		this.communicationBus = communicationBus;
	}

	/**
	 * Wait for device with the given serial to be connect or until timeout is
	 * reached. The default timeout is 5 seconds
	 * 
	 * @param serial
	 * @return The device with the given serial
	 * @throws ConnectionException
	 * 
	 *             If device was not connect until given timeout
	 */
	public USBDevice waitForDeviceToConnect(String serial) throws ConnectionException {
		final long start = System.currentTimeMillis();
		while (!devices.containsKey(serial)) {
			if (System.currentTimeMillis() - start > timeoutForDeviceConnection) {
				throw new ConnectionException(serial);
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Not important
			}
		}
		return (USBDevice) devices.get(serial);
	}

	/**
	 * 
	 * @param numOfDevices
	 *            The minimum number of devices that are expected to connect
	 * @return All USB devices that were connected
	 * @throws ConnectionException
	 *             If found less devices then expected
	 */
	public USBDevice[] waitForDevicesToConnect(final int numOfDevices) throws ConnectionException {
		final long start = System.currentTimeMillis();
		while (devices.size() < numOfDevices) {
			if (System.currentTimeMillis() - start > timeoutForDeviceConnection) {
				throw new ConnectionException("Found only " + devices.size() + " devices while expecting for at least "
						+ numOfDevices);
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Not important
			}
		}
		List<USBDevice> foundDevices = new ArrayList<USBDevice>();
		for (AbstractAndroidDevice device : devices.values()) {
			foundDevices.add((USBDevice) device);
		}
		return foundDevices.toArray(new USBDevice[] {});

	}

	@Override
	public void deviceConnected(IDevice device) {
		try {
			devices.put(device.getSerialNumber(), new USBDevice(adb, device));
		} catch (Exception e) {
			logger.error("Failed to add device", e);
		}

	}

	@Override
	public void deviceDisconnected(IDevice device) {
		devices.remove(device.getSerialNumber());
	}

	@Override
	public void deviceChanged(IDevice device, int changeMask) {
		// TODO Auto-generated method stub

	}

	public void setAdbLocation(File adbLocation) {
		this.adbLocation = adbLocation;
	}

}
